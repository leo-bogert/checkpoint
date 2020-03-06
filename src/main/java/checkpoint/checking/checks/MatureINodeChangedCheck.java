package checkpoint.checking.checks;

import checkpoint.checking.CheckFailedException;
import checkpoint.checking.ICheck;
import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

/** Checks for directories and files on which you haven't worked for a long time
 *  being modified, which indicates something tampered with them as you're
 *  likely to not change them anymore if you stopped changing them for months at
 *  some point.
 *  
 *  In more precise words this means:  
 *  Checks only "mature" {@link INode}s.  
 *  Those are such where {@link INode#getTimetamps()} as compared to
 *  {@link ICheckpoint#getDateEstimate()} of the given old checkpoint indicates
 *  that they have not changed for a "long" time, where "long" defaults to
 *  {@link #DEFAULT_MATURE_TIME_IN_MONTHS} and may be specified by the user.  
 *  FIXME: Implement configurability.  
 *  Reports failure if the hash or timestamps of a mature INode indicate that it
 *  has changed in the new checkpoint, or if the INode was deleted.  
 *  The timestamps are checked in addition to the hash to ensure mature files
 *  cannot wrongly become immature by resetting their timestamps by accident.  
 *  Also helping you with preservation of your files' timestamps is a design
 *  goal of checkpoint. */
public final class MatureINodeChangedCheck implements ICheck {

	public static final int DEFAULT_MATURE_TIME_IN_MONTHS = 3;

	@Override public void apply(
			ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint, INode oldNode)
			throws CheckFailedException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
