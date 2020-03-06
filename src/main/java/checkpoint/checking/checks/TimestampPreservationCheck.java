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
 *  `cp`. */
public final class TimestampPreservationCheck implements ICheck {

	@Override public void apply(
			ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint, INode oldNode)
			throws CheckFailedException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
