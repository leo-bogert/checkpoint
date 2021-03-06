#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
from os import path
import os
import stat
import logging
import sys
import signal
import psutil
import shlex, subprocess
import time
import itertools
import re

# As of 2013-02-14, Python does not support reading files line-by-line with a custom line delimiter.
# As we want to parse the output of "find -print0", this would be very useful.
# An iterator which supports this follows, copypasted from http://bugs.python.org/issue1152248
# TODO: As soon as Python supports this natively, use the native solution.
def fileLineIter(inputFile,
				inputNewline="\n",
				outputNewline=None,
				readSize=8192):
	"""Like the normal file iter but you can set what string indicates newline.
	The newline string can be arbitrarily long; it need not be restricted to a
	single character. You can also set the read size and control whether or not
	the newline string is left on the end of the iterated lines.  Setting
	newline to '\0' is particularly good for use with an input file created with
	something like "os.popen('find -print0')".
	"""
	if outputNewline is None: outputNewline = inputNewline
	partialLine = ''
	while True:
		charsJustRead = inputFile.read(readSize)
		if not charsJustRead: break
		partialLine += charsJustRead
		lines = partialLine.split(inputNewline)
		partialLine = lines.pop()
		for line in lines: yield line + outputNewline
	if partialLine: yield partialLine

clean_stat_regexp = re.compile('[.][0-9]+[ ]')
# As of 2015-02-16, stat does not support disabling printing of decimal places for the seconds of times
# Thus, this function removes them using the regexp clean_stat_regexp
def clean_stat(stat_output):
	return clean_stat_regexp.sub(' ', stat_output)

class Checkpoint:
	input_dir = None # The directory for which the Checkpoint is generated
	output_dir = None # The directory to which the Checkpoint will be written
	output_files = None # An OutputFiles object which lists all files in the output directory
	
	entries = None	# A dict containing all Entry objects of this checkpoint. The key is the file path
	
	log = None # A logger object which logs to the log file and optionally to the terminal
	
	abortion_requested = False	# Set to true if signal HUP/INT/TERM is received. The computation is aborted gracefully then and the progress is saved to disk.
	
	CONST_EOF_MARKER = { "complete" : "This checkpoint is complete.\n\0",
						"incomplete" : "This checkpoint is INCOMPLETE but can be resumed.\n\0" }
	
	CONST_SHA256SUM_DIRECTORY = "(directory)"
	CONST_SHA256SUM_FAILED = "(sha256sum failed!)"
	CONST_STAT_FAILED = "(stat failed!)"
	
	def __init__(self, input_dir, output_dir):
		self.input_dir, self.output_dir = map(path.abspath, (input_dir, output_dir))
		if not path.isdir(self.input_dir):
			raise IOError("Input directory does not exist!")
		
		self.output_files = Checkpoint.OutputFiles(self.output_dir)
	
	class OutputFiles:
		checkpoint = None # The file to which the checkpoint will be written
		checkpoint_oldformat_dates = None # The old format date-only checkpoint
		checkpoint_oldformat_sha256 = None # The old format sha256-only checkpoint
		file_list= None # The sorted listing of all files included in the checkpoint.
		file_list_unsorted = None # The listing of all files included in the checkpoint. Not sorted yet.
		log = None # The output log file
		
		def __init__(self, output_dir):
			self.checkpoint = "checkpoint.txt"
			# TODO: Remove, or implement generation
			self.checkpoint_oldformat_dates = "filedates.txt"
			# TODO: Remove, or implement generation
			self.checkpoint_oldformat_sha256 = "files.sha256"
			self.file_list = "checkpoint.txt.ls"
			self.file_list_unsorted = "checkpoint.txt.ls.tmp"
			self.log = "checkpoint.log"
			
			# __dict__ is equal to vars() but can be assigned
			self.__dict__ = { key: path.join(output_dir, value) for (key, value) in self.__dict__.iteritems() }
		
		def get_all(self):
			return vars(self).viewvalues()
	
	class Entry:
		sha256sum = None # The sha256sum. None if the Entry is a directory
		stat = None # The filedates (output of stat)
		
		def __init__(self, sha256sum=None, stat=None):
			self.sha256sum = sha256sum if not sha256sum == Checkpoint.CONST_SHA256SUM_DIRECTORY else None
			self.stat = stat
		
		def stat_filter(self, stat_array, exclude_times):
			for time in stat_array:
				if not time[0].lower() in exclude_times:
					yield time
		
		def get_stat(self, exclude_times):
			if not exclude_times:
				return self.stat
			else:
				return '\t'.join(self.stat_filter(self.stat.split('\t', 3), exclude_times))
			
	def generate_files_and_directories(self):
		if not path.isdir(self.output_dir):
			os.makedirs(self.output_dir,0700)
		
		if os.geteuid() == 0:
			os.chown(self.output_dir, 0, 0)
		
		os.chmod(self.output_dir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
		
		for file in self.output_files.get_all():
			if not path.isfile(file):
				with open(file, "a") as f:	# create the file
					pass
			
			if os.geteuid() == 0:
				os.chown(file, 0, 0)
			os.chmod(file, stat.S_IRUSR | stat.S_IWUSR)
	
	def init_logging(self, show_log_on_console = False):
		log = logging.getLogger()
		log.setLevel(logging.DEBUG)
		
		format = logging.Formatter(fmt='%(asctime)s %(levelname)s: %(message)s', datefmt='%Y-%m-%d %H:%M:%S')
		
		logfile = logging.FileHandler(self.output_files.log)
		logfile.setLevel(logging.INFO)
		logfile.setFormatter(format)
		log.addHandler(logfile)
		
		if show_log_on_console:
			# There is no inverse "setLevel" function so we need a filter class
			class NoStderrMessagesFilter:
				def filter(self, record):
					return record.levelno < logging.WARNING
			
			stdout = logging.StreamHandler(sys.stdout)
			stdout.setLevel(logging.DEBUG)
			stdout.setFormatter(format)
			stdout.addFilter(NoStderrMessagesFilter())
			log.addHandler(stdout)
			
			stderr = logging.StreamHandler(sys.stderr)
			stderr.setLevel(logging.WARNING)
			stderr.setFormatter(format)
			log.addHandler(stderr)
		
		self.log = log
	
	def trap_signals(self):
		signal.signal(signal.SIGHUP, self.interrupt_compute)
		signal.signal(signal.SIGINT, self.interrupt_compute)
		signal.signal(signal.SIGTERM, self.interrupt_compute)
	
	def interrupt_compute(self, signum, frame):
		self.abortion_requested = True
	
	def set_ioniceness(self):
		process = psutil.Process(os.getpid())
		if(hasattr(process, "ionice")):
			process.ionice(psutil.IOPRIO_CLASS_IDLE)
		else:
			process.set_ionice(psutil.IOPRIO_CLASS_IDLE)
	
	def load_from_disk(self):
		self.log.info("Loading existing checkpoint data to resume from it ...")
		
		if path.getsize(self.output_files.checkpoint) == 0:
			self.log.info("No existing checkpoint found, creating a fresh one...")
			self.entries = { }
			return True
		
		count = 0
		count_ignored = 0
		entries = { }
		with open(self.output_files.checkpoint, "r") as input:
			for line in input:
				splitline = line.split("\0", 1)
				
				file = splitline[0]
				
				if len(splitline) == 2:
					 checkpoint_data = splitline[1]
				else:
					self.entries = entries
					
					if file + "\0" == Checkpoint.CONST_EOF_MARKER["complete"]:
						self.log.info("Checkpoint is complete already.")
						return False
					elif file + "\0" == Checkpoint.CONST_EOF_MARKER["incomplete"]:
						self.log.info("Loaded {} existing checkpoint datasets, ignored {} existing datasets where sha256sum or stat had failed previously.".format(count, count_ignored))
						return True
					else:
						raise IOError("End of file marker not found - Input file is incomplete!")
				
				(sha256sum, stat) = checkpoint_data.strip().split("\t", 1)
				if Checkpoint.CONST_SHA256SUM_FAILED in sha256sum or Checkpoint.CONST_STAT_FAILED in stat:
					count_ignored += 1
					continue
				
				entry = Checkpoint.Entry(sha256sum, stat)
				assert file not in entries
				entries[file] = entry
				count += 1
			
			raise IOError("End of file marker not found - Input file is incomplete!")
	
	def compute_find(self):
		self.log.info("Computing list of files included in the checkpoint ...")
		
		with open(self.output_files.log, "a") as log_file:
			with open(self.output_files.file_list_unsorted, "w") as find_output:
				# We run find inside the target directory so the filenames in the output are relative
				# IMPORTANT: The "-print0" must be AFTER the search options or find will return ALL files. TODO: File a bug report
				subprocess.check_call(shlex.split("find . -mount ( -type f -o -type d ) -print0"), cwd=self.input_dir, stderr=log_file, stdout=find_output)
		
		self.log.info("Computing list of files included in the checkpoint finished.")
	
	def compute_sort(self):
		self.log.info("Sorting list of files included in the checkpoint ...")
		
		with open(self.output_files.log, "a") as log_file:
			with open(self.output_files.file_list, "w") as sort_output:
				# LC_ALL=C is critically important to ensure files of the same
				# directory will be next to each other in the output.
				# As a bonus it also ensures the sorting is always the same
				# indepedent of the local system's language configuration.
				subprocess.check_call(
					("sort", "--zero-terminated",
					self.output_files.file_list_unsorted), env={"LC_ALL": "C"},
					cwd=self.output_dir, stderr=log_file, stdout=sort_output)
		
		os.remove(self.output_files.file_list_unsorted)
		
		self.log.info("Sorting list of files included in the checkpoint finished.")
	
	def compute_sha256sum(self, file, log_file):
		sha_proc = subprocess.Popen(("sha256sum", "--binary", file), bufsize=-1, cwd=self.input_dir, stdout=subprocess.PIPE, stderr=log_file)
		sha_output = sha_proc.communicate()
		
		assert sha_proc.returncode != None	# process has exited
		
		if sha_proc.returncode == 0:
			sha_stdout = sha_output[0]
			(sha256sum, sha_filename) = sha_stdout.split(" *", 1)
			
			# if the sum starts with a backslash, sha256sum has escaped linebreaks and the backslash character in the filename
			
			sha_filename_unescape = lambda filename: filename if sha256sum[0] != "\\" else filename.replace("\\n", "\n").replace("\\\\", "\\")
			assert sha_filename_unescape(sha_filename) == file + "\n"
			
			if sha256sum[0] == "\\":
				sha256sum = sha256sum[1:]				
		else:
			sha256sum = Checkpoint.CONST_SHA256SUM_FAILED
		
		return sha256sum
	
	def compute_stat(self, file, log_file):
		# TODO: Use the Python API for obtaining the stat once Python supports obtaining the birth-time.

		stat_proc = subprocess.Popen(("stat", "--printf", "Birth: %w\tAccess: %x\tModify: %y\tChange: %z", file), bufsize=-1, cwd=self.input_dir, stdout=subprocess.PIPE, stderr=log_file)
		stat_output = stat_proc.communicate()
		
		assert stat_proc.returncode != None	# process has exited
		
		if stat_proc.returncode == 0:
			stat = clean_stat(stat_output[0])
		else:
			stat = Checkpoint.CONST_STAT_FAILED
		
		return stat
	
	def compute(self):
		self.log.info("Computing checkpoint ...")
		
		count_computed = 0
		count_failed = 0
		count_skipped = 0

		last_written_to_disk_time = time.time()
		
		# The paths in the file listing are relative.
		# Because we test for the type of the file paths, weneed to set our working directory to the input dir.
		os.chdir(self.input_dir)
		
		with open(self.output_files.log, "a") as log_file:
			with open(self.output_files.file_list, "r") as file_list:
				# Max path length in Linux is 4096, so the first intention would be to use something around that as readSize.
				# However, we should imagine that the typical bottleneck of this script is generating checksums for large amounts of small files:
				# Then the overhead of moving the disk heads continously is very large and would be even larger if they had to be moved very often
				# for reading from the file list file.
				# On the other hand, a 1 MiB buffer for the list of input files is very cheap and will greatly reduce moving of the disk heads so we go for it.
				for file in fileLineIter(file_list, inputNewline="\0", outputNewline="", readSize=1024*1024):
					if self.abortion_requested:
						self.log.info("Aborting computation due to signal!")
						break

					minutes_since_write_to_disk = (time.time() - last_written_to_disk_time) / 60
					if minutes_since_write_to_disk > 15:
						self.log.info(str(int(minutes_since_write_to_disk)) + " minutes have passed, writing progress to disk ...")
						self.write_to_disk(incomplete=True)
						last_written_to_disk_time = time.time()					
					
					if file in self.entries:
						count_skipped += 1
						continue
					
					if path.isfile(file):
						sha256sum = self.compute_sha256sum(file, log_file)
						if sha256sum == Checkpoint.CONST_SHA256SUM_FAILED:
							count_failed += 1
					elif path.isdir(file):
						sha256sum = Checkpoint.CONST_SHA256SUM_DIRECTORY
					elif not path.exists(file):
						self.log.warning("File deleted during processing: " + file)
						count_failed += 1
						continue
					else:
						raise IOError("Unexpected type of file: " + file)
					
					stat = self.compute_stat(file, log_file)
					if stat == Checkpoint.CONST_STAT_FAILED:
						count_failed += 1
					
					self.entries[file] = Checkpoint.Entry(sha256sum, stat)
					count_computed += 1
		
		self.log.info("Computing finished. Computed {} entries. sha256/stat failed {} times. Skipped {} of {} files due to incremental computation.".format(count_computed, count_failed, count_skipped, len(self.entries)))
	
	def write_to_disk(self, incomplete=False, exclude_times=None):
		self.log.info("Writing checkpoint to disk ...")
		
		count_written = 0
		count_total = 0
		
		with open(self.output_files.checkpoint, "w") as output:
			with open(self.output_files.file_list, "r") as file_list:
				# See compute for the reasons behind choice of readSize
				for file in fileLineIter(file_list, inputNewline="\0", outputNewline="", readSize=1024*1024):
					count_total += 1
					
					if file not in self.entries:
						continue
					
					entry = self.entries[file]					
					output.write(file)
					output.write("\0\t")
					output.write(entry.sha256sum if entry.sha256sum else Checkpoint.CONST_SHA256SUM_DIRECTORY)
					output.write("\t")
					output.write(entry.get_stat(exclude_times))
					output.write("\n")
					
					count_written += 1
			
			eof_marker = Checkpoint.CONST_EOF_MARKER["incomplete" if incomplete or self.abortion_requested else "complete"]
			output.write(eof_marker)

		self.log.info("Writing checkpoint to disk finished. Wrote an entry for {} of the {} input files/directories. ".format(count_written, count_total))
		self.log.info(eof_marker.rstrip("\n\0"))


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("input_directory", help="The directory for which to generate the checkpoint.")
	parser.add_argument("output_directory", help="The directory to which the checkpoint shall be written. Will be created automatically if it does not exist.")
	parser.add_argument("--exclude-times", help="Exclude the selected file timestamps from the output. Can be a combination of the letters a(ccess), b(irth), c(hange), m(modification).", choices=["".join(perm) for i in xrange(1,5) for perm in itertools.combinations('abcm', i)])
	parser.add_argument("--rewrite-only", help="Do nothing but load the checkpoint from disk and write it to disk again. Useful to filter out unwanted information aftwards, for example with --exclude-times", action='store_true')
	parser.add_argument('--verbose', '-v', help="Not only log to the log file but also to stdout/stderr. Also, print additional debug messages which would not be written to the log file. Notice that error messages from subprocesses will only be visible in the log file.", action='count')
	args = parser.parse_args()
	
	checkpoint = Checkpoint(args.input_directory, args.output_directory)
	checkpoint.generate_files_and_directories()
	checkpoint.init_logging(args.verbose > 0)
	checkpoint.trap_signals()
	checkpoint.set_ioniceness()
	if not checkpoint.load_from_disk() and not args.rewrite_only:
		# The checkpoint is complete already, nothing to do.
		checkpoint.log.error("Nothing to do, exiting.")
		return False
	if not args.rewrite_only:
		checkpoint.compute_find()
		checkpoint.compute_sort()
		checkpoint.compute()
	checkpoint.write_to_disk(exclude_times=args.exclude_times)
	
	if checkpoint.abortion_requested:
		return False
	
	return True

if __name__ == "__main__":
	sys.exit(0 if main() else 1)
