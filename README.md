checkpoint
==========

An incremental, recursive checksum script which also adds filedates to the listing. Designed for LARGE amounts of input files. It is resumable, which means it can continue computation where it was aborted without recomputing the existing checksums. Its output is suitable as input for diff. 

Installation
============

This script was written & tested on Ubuntu 12.10 x86_64. It should work on any Ubuntu or Debian-based distribution.

For the Python version of the script, you will need the following package(s):
	* python-psutil

