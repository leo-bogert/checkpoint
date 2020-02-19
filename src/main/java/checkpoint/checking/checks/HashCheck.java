package checkpoint.checking.checks;

import checkpoint.checking.CheckFailedException;
import checkpoint.checking.ICheck;
import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

/** Checks files, i.e. {@link INode}s with {@link INode#isDirectory()} == false,
 *  for if the {@link INode#getHash()} and thus the file content has changed but
 *  the {@link INode#getTimetamps()} do not indicate that the content should
 *  have changed.  
 *  If that happens it is likely due to hardware failure or filesystem bugs as
 *  any change to a file's content should cause its timestamps to change. */
public final class HashCheck implements ICheck {

	@Override public void apply(
			ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint, INode oldNode)
			throws CheckFailedException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
