#!/usr/bin/env python
# -*- coding: utf-8 -*-

class Checkpoint:
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

def main():
	pass

if __name__ == "__main__":
	main()
