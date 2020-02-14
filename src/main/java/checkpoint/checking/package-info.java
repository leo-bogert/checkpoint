/** Contains code to implement the {@link checkpoint.ui.shell.CheckCommand}
 *  shell command which compares two checkpoints against each other and uses
 *  that to check the integrity of your files.  
 *  For example it will alert you of files of which the checksum has changed but
 *  the file timestamps did not - which indicates disk failure.
 *  
 *  A typical usecase is to run this with the two input checkpoints being:
 *  - one of a backup of your data.
 *  - one of the current state of your system. */
package checkpoint.checking;