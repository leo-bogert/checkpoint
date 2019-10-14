# Second bash implementation

This is the second implementation of checkpoint, written in Bash like the first.

Its advantage is that it supports incremental computation so you can abort
computation of a checkpoint and restart it later on.
Further, where the first implementations would dump errors of sha256sum/stat
calls into a log file, the second is able to document such failures in the
checkpoint file at each file for which they happened. 

The cost of the advanced features is using a lot more complex Bash features
which might make it slower and could have caused bugs.
