package checkpoint.checking;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

import checkpoint.checking.checks.*;
import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;
import checkpoint.ui.shell.CheckCommand;

/** Implementations of this compare two checkpoints against each other to check
 *  the integrity of your files and directories.  
 *  For example {@link HashCheck} will alert you of files of which the checksum
 *  has changed but the file timestamps did not - which indicates disk failure.
 *  
 *  A typical usecase is to run the {@link CheckCommand} shell command with the
 *  two input checkpoints being:
 *  - one of a backup of your data.
 *  - one of the current state of your system.
 *  
 *  NOTICE: Implementations must be registered at {@link #IMPLEMENTATIONS} to
 *  be used!
 *  
 *  They shall be contained in the package {@link checkpoint.checking.checks}.
 *  They are run by the {@link CheckRunner} to power the {@link CheckCommand}
 *  shell command. */
public interface ICheck {

	/** NOTICE: You MUST add any implementations of this interface to this list
	 *  to ensure {@link CheckRunner} will use them!
	 *  TODO: Determine them automatically instead. As of 2020-02-14 Java
	 *  reflection does not seem capable of this, a library would be needed. */
	public static final List<Class<? extends ICheck>> IMPLEMENTATIONS
		= unmodifiableList(asList(
			HashCheck.class,
			ImportantDirChangedCheck.class,
			MatureINodeChangedCheck.class,
			TimestampPreservationCheck.class));

	/** Applies the check upon the given {@link INode} of the old
	 *  {@link ICheckpoint}, in comparison to the new {@link ICheckpoint}.
	 *  Throws {@link CheckFailedException} upon failure.
	 *  
	 *  Is called once for each node of the old checkpoint.
	 *  
	 *  Should apply the check by e.g. checking if the new ICheckpoint contains
	 *  an equivalent INode in terms of hash, timestamps, etc.
	 *  
	 *  There intentionally is no parameter to provide the equivalent INode
	 *  from the new checkpoint, in terms of equivalent
	 *  {@link INode#getPath()}:  
	 *  It is at the choice of implementations of this function to automatically
	 *  detect moved nodes (by e.g. matching hash) to avoid spamming the user
	 *  with false failures such as e.g.: "File deleted which should not have
	 *  been deleted!".  
	 *  But when doing that consider to ignore matching hashes on very small
	 *  files to avoid bogus matches for e.g. empty files. */
	void apply(ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint,
		INode oldNodeAtCheck) throws CheckFailedException;

}
