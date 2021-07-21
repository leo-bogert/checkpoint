package checkpoint.checking.checks;

import checkpoint.checking.CheckFailedException;
import checkpoint.checking.ICheck;
import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

/** For INodes with {@link INode#isDirectory()} == false reports them
 *  if their {@link INode#getHash()} indicates that they have not changed but
 *  their {@link INode#getTimetamps()} did change.  
 *  This indicates that something destroyed your file timestamps for no good
 *  reasons, such as copying a filesystem without the `--archive` argument to
 *  `cp`.
 *  
 *  FIXME: Rename to `FileTimestampPreservationCheck` to reflect that it does
 *  not check directories.  
 *  FIXME: We could also check timestamp preservation of directories by checking
 *  if the set of contained files/dirs changed. If it did not change then there
 *  is no reason for the directory timestamp to have changed. */
public final class TimestampPreservationCheck implements ICheck {

	@Override public void apply(
			ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint, INode oldNode)
			throws CheckFailedException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
