package checkpoint.checking.checks;

import checkpoint.checking.CheckFailedException;
import checkpoint.checking.ICheck;
import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

/** Checks only "mature" {@link INode}s.
 *  Those are such where {@link INode#getTimetamps()} indicates that it has not
 *  changed for a "long" time, where "long" defaults to
 *  {@link #DEFAULT_MATURE_TIME_IN_MONTHS} and may be specified by the user.  
 *  FIXME: Implement configurability.  
 *  Reports failure if the hash of a mature INode indicates that it has changed
 *  in the new checkpoint, or if the INode was deleted. */
public final class MatureINodeChangedCheck implements ICheck {

	public static final int DEFAULT_MATURE_TIME_IN_MONTHS = 3;

	@Override public void apply(
			ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint, INode oldNode)
			throws CheckFailedException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
