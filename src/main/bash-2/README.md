# Second bash implementation

This is the second implementation of checkpoint, written in Bash like the first.

Its advantage is that it supports incremental computation so you can abort
computation of a checkpoint and restart it later on.

The cost of that is using a lot more complex Bash features which might make it
slower and could have bugs.
