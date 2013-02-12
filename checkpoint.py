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

class Checkpoint:
	input_dir = None # The directory for which the Checkpoint is generated
	output_dir = None # The directory to which the Checkpoint will be written
	output_files = None # An OutputFiles object which lists all files in the output directory

	entries = None	# A set containing all Entry objects of this checkpoint

	log = None # A logger object which logs to the log file and optionally to the terminal

	abortion_requested = False	# Set to true if signal HUP/INT/TERM is received. The computation is aborted gracefully then and the progress is saved to disk.

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

		def __init__(self, path):
			self.path = path

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

	def trap_signals(self):
		signal.signal(signal.SIGHUP, self.interrupt_compute)
		signal.signal(signal.SIGINT, self.interrupt_compute)
		signal.signal(signal.SIGTERM, self.interrupt_compute)

	def interrupt_compute(self):
		abortion_requested = True

	def set_ioniceness(self):
		p = psutil.Process(os.getpid())
		p.set_ionice(psutil.IOPRIO_CLASS_IDLE)

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

if __name__ == "__main__":
	main()
