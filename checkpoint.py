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

class Checkpoint:
	input_dir = None # The directory for which the Checkpoint is generated
	output_dir = None # The directory to which the Checkpoint will be written
	output_files = None # An OutputFiles object which lists all files in the output directory
	
	entries = None	# A set containing all Entry objects of this checkpoint
	
	log = None # A logger object which logs to the log file and optionally to the terminal
	
	abortion_requested = False	# Set to true if signal HUP/INT/TERM is received. The computation is aborted gracefully then and the progress is saved to disk.
	
	eof_marker = { "complete" : "This checkpoint is complete.\n\0",
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
		log = None # The output log file
		
		def __init__(self, output_dir):
			self.checkpoint = path.join(output_dir, "checkpoint.txt")
			self.checkpoint_oldformat_dates = path.join(output_dir, "filedates.txt")
			self.checkpoint_oldformat_sha256 = path.join(output_dir, "files.sha256")
			self.log = path.join(output_dir, "checkpoint.log")
		
		def get_all(self):
			return vars(self)		
	
	class Entry:
		path = None # The path of the file/directory in the filesystem
		sha256sum = None # The sha256sum. None if the Entry is a directory
		stat = None # The filedates (output of stat)
		
		def __init__(self, path, sha256sum=None, stat=None):
			self.path = path
			self.sha256sum = sha256sum if not sha256sum == Checkpoint.CONST_SHA256SUM_DIRECTORY else None
			self.stat = stat
		
		def __hash__(self):
			return self.path.__hash__()
		
		def __eq__(self, other):
			return self.path.__eq__(other.path)
	
	def generate_files_and_directories(self):
		if not path.isdir(self.output_dir):
			os.makedirs(self.output_dir,0700)
		
		if os.geteuid() == 0:
			os.chown(self.output_dir, 0, 0)
		
		os.chmod(self.output_dir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
		
		for file in self.output_files.get_all().viewvalues():
			if not path.isfile(file):
				with open(file, "a") as f:	# create the file
					pass
			
			if os.geteuid() == 0:
				os.chown(file, 0, 0)
			os.chmod(file, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
	
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
	
	def interrupt_compute(self):
		abortion_requested = True
	
	def set_ioniceness(self):
		p = psutil.Process(os.getpid())
		p.set_ionice(psutil.IOPRIO_CLASS_IDLE)
	
	def load_from_disk(self):
		self.log.info("Loading existing checkpoint data to resume from it ...")
		
		if path.getsize(self.output_files.checkpoint) == 0:
			self.log.info("No existing checkpoint found, creating a fresh one...")
			self.entries = set()
			return True
		
		count = 0
		count_ignored = 0
		entries = set()
		with open(self.output_files.checkpoint, "r") as input:
			for line in input:
				splitline = line.split("\0", 1)
				
				file = splitline[0]
				
				if len(splitline) == 2:
					 checkpoint_data = splitline[1]
				else:
					if file + "\0" == self.eof_marker["complete"]:
						self.log.error("Checkpoint is complete already, nothing to do. Exiting.")
						return False
					elif file + "\0" == self.eof_marker["incomplete"]:
						self.log.info("Loaded {} existing checkpoint datasets, ignored {} existing datasets where sha256sum or stat had failed previously.".format(count, count_ignored))
						self.entries = entries
						return True
					else:
						raise IOError("End of file marker not found - Input file is incomplete!")
				
				(sha256sum, stat) = checkpoint_data.split("\t", 2)[1:]
				if Checkpoint.CONST_SHA256SUM_FAILED in sha256sum or Checkpoint.CONST_STAT_FAILED in stat:
					count_ignored += 1
					continue
			
				entry = Checkpoint.Entry(file, sha256sum, stat)
				assert entry not in entries
				entries.add(entry)
				count += 1
			
			raise IOError("End of file marker not found - Input file is incomplete!")
	
	def compute_sha256sum(self, file, log_file):
		sha_proc = subprocess.Popen(("sha256sum", "--binary", file), bufsize=-1, cwd=self.input_dir, stdout=subprocess.PIPE, stderr=log_file)
		sha_output = sha_proc.communicate()
		
		assert sha_proc.returncode != None	# process has exited
		
		if sha_proc.returncode == 0:
			sha_stdout = sha_output[0]
			(sha256sum, rest) = sha_stdout.split(" ", 1)
			assert rest.split("*", 1)[1] == file + "\n"
		else:
			sha256sum = Checkpoint.CONST_SHA256SUM_FAILED
		
		return sha256sum
	
	def compute_stat(self, file, log_file):
		stat_proc = subprocess.Popen(("stat", "--printf", "Birth: %w\tAccess: %x\tModify: %y\tChange: %z", file), bufsize=-1, cwd=self.input_dir, stdout=subprocess.PIPE, stderr=log_file)
		stat_output = stat_proc.communicate()
		
		assert stat_proc.returncode != None	# process has exited
		
		if stat_proc.returncode == 0:
			stat = stat_output[0]
		else:
			stat = Checkpoint.CONST_STAT__FAILED
		
		return stat
	
	def compute(self):
		self.log.info("Computing checkpoint ...")
		
		count_computed = 0
		count_failed = 0
		count_skipped = 0
		
		# The paths in the checkpoint shall be relative. So we set the working directory of the "find" process to the input dir.
		# Because we test for the type of the returned file paths on our own, we also need to set our working directory to the input dir.
		os.chdir(self.input_dir)
		
		with open(self.output_files.log, "a") as log_file:
			# We run find inside the target directory so the filenames in the output are relative
			# Max path length in Linux is 4096, so we use fileLineIter with readSize=8192 and set bufsize to 8192*2
			find = subprocess.Popen(shlex.split("find . -mount -print0 ( -type f -o -type d )"), bufsize=16384, cwd=self.input_dir, stderr=log_file, stdout=subprocess.PIPE)
			
			for file in fileLineIter(find.stdout, inputNewline="\0", outputNewline="", readSize=2*4096):
				if self.abortion_requested:
					self.log.info("Aborting computation due to signal!")
					break
				
				if Checkpoint.Entry(file) in self.entries:
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
					continue
				else:
					raise IOError("Unexpected type of file: " + file)
				
				stat = self.compute_stat(file, log_file)
				if stat == Checkpoint.CONST_STAT_FAILED:
					count_failed += 1
				
				self.entries.add(Checkpoint.Entry(file, sha256sum, stat))
				count_computed += 1
			
			if find.wait() != 0:
				raise SystemError("find exit-code is non-zero!")
		
		self.log.info("Computing finished. Computed {} entries successfully. {} computations failed. Skipped {} of {} files due to incremental computation.".format(count_computed, count_failed, count_skipped, len(self.entries)))


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("input_directory", help="The directory for which to generate the checkpoint")
	parser.add_argument("output_directory", help="The directory to which the checkpoint shall be written. Will be created automatically if it does not exist")
	parser.add_argument('--verbose', '-v', help="Not only log to the log file but also to stdout/stderr. Also, print additional debug messages which would not be written to the log file.", action='count')
	args = parser.parse_args()
	
	checkpoint = Checkpoint(args.input_directory, args.output_directory)
	checkpoint.generate_files_and_directories()
	checkpoint.init_logging(args.verbose > 0)
	checkpoint.trap_signals()
	checkpoint.set_ioniceness()
	if not checkpoint.load_from_disk():
		return False # The checkpoint is complete already, nothing to do.
	checkpoint.compute()

	return True

if __name__ == "__main__":
	sys.exit(0 if main() else 1)
