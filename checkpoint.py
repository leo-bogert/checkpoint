#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
from os import path
import os
import stat

class Checkpoint:
	input_dir = None # The directory for which the Checkpoint is generated
	output_dir = None # The directory to which the Checkpoint will be written

	def __init__(self, input_dir, output_dir):
		self.input_dir, self.output_dir = map(path.abspath, (input_dir, output_dir))
		if not path.isdir(self.input_dir):
			raise IOError("Input directory does not exist!")
		
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

	entries = None	# All entries

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
