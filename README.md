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

```bash
# Install runtime dependencies.
apt install default-jre libcommons-codec-java
# Install dependencies for compiling from source.
# You don't need to install the recommendations / suggestions of gradle.
# TODO: I have not tested if the other packages work without recommendations.
apt install git gnupg gradle junit4

# Download the source code.
# FIXME: Remove the branch from these instructions once it has been merged to
# branch master.
git clone https://github.com/leo-bogert/checkpoint.git --branch java-implementation
# Download the key which the source code is signed with.
gpg --recv-key '1517 3ECB BC72 0C9E F420  5805 B26B E43E 4B5E AD69'
cd checkpoint
# Verify the GnuPG signature of the latest commit.
# If this fails the code has been tampered with!
git verify-commit HEAD

# Compile.
gradle --no-daemon clean jar test

# To run it you can now use the following.
# TODO: Provide a wrapper script so "java -jar" is not necessary.
# (JARs are *not* executable without a wrapper on Ubuntu, it would try to run
# their contents as if they were a shell script, which causes random things to
# happen!)
java -jar build/libs/checkpoint.jar
```

### Python implementation

Download the source code as described in the section for installing the Java
implementation.

Then:

```bash
# Install runtime dependencies
apt install python python-psutil

# To run it you can now use the following:
cd checkpoint
src/main/python/checkpoint.py
```

## Usage

**WARNING:** The Java, Python and Bash implementations each have a completely
different syntax of their shell command!    
You can run the shell command of each implementation without arguments to get an
overview of its syntax.

## License

- You can use and relicense this however you want to. This may be limited by
  the licenses of the libraries it uses, see `build.gradle`.

- No warranty whatsoever is provided for any damage caused by this. Use at your
  own risk!