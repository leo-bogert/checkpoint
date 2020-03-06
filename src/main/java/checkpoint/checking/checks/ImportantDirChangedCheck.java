package checkpoint.checking.checks;

import checkpoint.checking.CheckFailedException;
import checkpoint.checking.ICheck;
import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

/** Checks a set of user-specified directories and all their sub-directories
 *  and files for if any of them changed and reports all changes except
 *  non-destructive ones.  
 *  Non-destructive hereby means creation of new files and changes of timestamps
 *  of their parent directory which were probably caused by file creation. */
public final class ImportantDirChangedCheck implements ICheck {

	@Override public void apply(
			ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint, INode oldNode)
			throws CheckFailedException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
