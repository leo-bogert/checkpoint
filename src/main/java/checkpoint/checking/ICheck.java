package checkpoint.checking;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;
import checkpoint.ui.shell.CheckCommand;

interface ICheck {

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
	 *  files to avoid bogus matches for e.g. empty files.*/
	void apply(ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint,
		INode oldNodeAtCheck) throws CheckFailedException;

}
