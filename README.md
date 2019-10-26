# checkpoint

An incremental, recursive checksum script which also adds filedates to the listing. Designed for LARGE amounts of input files. It is resumable, which means it can continue computation where it was aborted without recomputing the existing checksums. Its output is suitable as input for diff. 

## Implementations

There are multiple implementations available here in different programming
languages:
- Java
- Python
- Bash

The Java implementation is the most recent one, the Python implementation the
second most recent one.  
The Java implementation has the most features, the other ones are not maintained
anymore.  
They are preserved for testing the Java implementation against it.  
This is currently **not** automated by the unit tests!

## Installation

The tool was developed on Ubuntu and should easily work on any other
Debian-based distribution.

### Java implementation

FIXME

### Python implementation

Install the package `python-psutil` before usage.

## License

- You can use and relicense this however you want to. This may be limited by
  the licenses of the libraries it uses, see `build.gradle`.

- No warranty whatsoever is provided for any damage caused by this. Use at your
  own risk!