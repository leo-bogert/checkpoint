# Python implementation

This is the third implementation of checkpoint.

It is faster than the second bash implementation, and has a lot more features.
See `./checkpoint.py --help`.

The major disadvantage of this implementation is that it is still too slow to
use it upon a full Linux filesystem.  
The major reasons for that are:
- the slowness of Python
- the fact that it uses the system's sha256sum/stat binaries instead of Python
  API
- it is single-threaded.
