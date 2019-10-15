package checkpoint.datamodel;

import java.nio.file.attribute.FileTime;

/** Filesystem timestamps of a file/directory.
 *  Implementations of this class must read all timestamps at once from disk
 *  upon instantiation to avoid unnecessary disk seeking. */
public abstract class Timestamps {

	/** We support including the access time in checkpoints even though
	 *  generating one will access all files because the access time is a
	 *  very suitable replacement for {@link #getBirthTime()} which isn't
	 *  implemented on an average Linux (see its JavaDoc):
	 *  If you mount your filesystem with "noatime" then the access times
	 *  won't ever be updated and thus are equal to the time of birth of each
	 *  file. */
	public abstract FileTime getAccessTime();

	/** Always returns null currently because:
	 *  - the Linux kernel does not currently support obtaining it in userspace.
	 *  - and Java 11 does not seem to support detection if the above is still
	 *    the case, it will just return a different one of the timestamps. */
	public final FileTime getBirthTime() {
		return null;
	}

	public abstract FileTime getStatusChangeTime();

	public abstract FileTime getModificationTime();

}