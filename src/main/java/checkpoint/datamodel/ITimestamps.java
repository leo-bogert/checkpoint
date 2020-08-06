package checkpoint.datamodel;

import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;

import checkpoint.datamodel.implementation.Checkpoint;

/** Filesystem timestamps of a file/directory.
 *  Implementations of this must be immutable!
 * 
 *  Java does not support abstract static functions so there are commented-out
 *  functions which are also required to be implemented.
 *  
 *  Using {@link Date} instead of {@link FileTime} because Java 7 does not yet
 *  support:
 *   - class Instant
 *   - {@link FileTime}.getInstant().
 *   - class DateTimeFormatter to consume objects of Instant.
 *  
 *  Thus the only way to convert a FileTime to a String seems to be to use
 *  {@link FileTime#toMillis()}, feed that into the legacy class
 *  {@link Date} and use {@link SimpleDateFormat} upon it.
 *  
 *  TODO: Java 8: Deal with the above. */
public abstract class ITimestamps {

	/** All timestamps which could be supported by {@link ITimestamps}
	 *  implementations.  
	 *  To be used in an {@link EnumSet} for filtering purposes, see
	 *  {@link ICheckpoint#save(java.nio.file.Path, EnumSet)}. */
	public static enum TimestampTypes {
		AccessTime,
		BirthTime,
		StatusChangeTime,
		ModificationTime
	}

	/** Implementations must read all timestamps at once from disk to avoid
	 *  unnecessary disk seeking! */
	/*
	public abstract static ITimestamps readTimestamps(Path p)
		throws IOException;
	*/

	/** Any of the given {@link Date} objects may be null if it has been
	 *  filtered out of the {@link Checkpoint}. */
	/*
	public abstract static ITimestamps timestampsFromDates(
			Date atime, Date ctime, Date mtime);
	*/

	/** Null if it has been filtered out of the {@link Checkpoint}.
	 *  
	 *  We support including the access time in checkpoints even though
	 *  generating one will access all files because the access time is a
	 *  very suitable replacement for {@link #getBirthTime()} which isn't
	 *  implemented on an average Linux (see its JavaDoc):
	 *  If you mount your filesystem with "noatime" then the access times
	 *  won't ever be updated and thus are equal to the time of birth of each
	 *  file. */
	public abstract Date getAccessTime();

	/** Always returns null currently because:
	 *  - the Linux kernel does not currently support obtaining it in userspace.
	 *  - and Java 11 does not seem to support detection if the above is still
	 *    the case, it will just return a different one of the timestamps.
	 *  
	 *  TODO: Remove the implementation and thereby the above limitation once
	 *  it is possible to get the birth time from the kernel.
	 *  Once you do that also make this abstract class an interface instead to
	 *  match its name prefix. */
	public final Date getBirthTime() {
		return null;
	}

	/** Null if it has been filtered out of the {@link Checkpoint}. */
	public abstract Date getStatusChangeTime();

	/** Null if it has been filtered out of the {@link Checkpoint}. */
	public abstract Date getModificationTime();

}