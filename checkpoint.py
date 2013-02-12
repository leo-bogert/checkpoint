#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
from os import path
import os
import stat

class Checkpoint:
	input_dir = None # The directory for which the Checkpoint is generated
	output_dir = None # The directory to which the Checkpoint will be written
	output_files = None # An OutputFiles object which lists all files in the output directory

	entries = None	# A set containing all Entry objects of this checkpoint

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
			self.log = path.join(output_dir, "errors.log")

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

def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("input_directory", help="The directory for which to generate the checkpoint")
	parser.add_argument("output_directory", help="The directory to which the checkpoint shall be written. Will be created automatically if it does not exist")
	args = parser.parse_args()
	
	checkpoint = Checkpoint(args.input_directory, args.output_directory)
	checkpoint.generate_files_and_directories()

if __name__ == "__main__":
	main()
