package checkpoint.datamodel.implementation;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Map;

import checkpoint.datamodel.ITimestamps;

public final class Timestamps extends ITimestamps {

	private final FileTime atime;
	private final FileTime ctime;
	private final FileTime mtime;

	private Timestamps(Path p) throws IOException {
		// TODO: Performance: Check if it is faster to read "unix:*", i.e.
		// to avoid the parsing overhead of specifying individual attributes.
		Map<String, Object> attrs = Files.readAttributes(p,
			"unix:lastAccessTime,ctime,lastModifiedTime",
			LinkOption.NOFOLLOW_LINKS);
		
		atime = requireNonNull((FileTime)attrs.get("lastAccessTime"));
		ctime = requireNonNull((FileTime)attrs.get("ctime"));
		mtime = requireNonNull((FileTime)attrs.get("lastModifiedTime"));
	}

	public static Timestamps readTimestamps(Path p) throws IOException {
		return new Timestamps(p);
	}

	private Timestamps(FileTime atime, FileTime ctime, FileTime mtime) {
		this.atime = atime;
		this.ctime = ctime;
		this.mtime = mtime;
	}

	/** TODO: Java 8: Use Instant instead of Date */
	public static Timestamps timestampsFromDates(
			Date atime, Date ctime, Date mtime) {
		
		return new Timestamps(
			(atime != null ? FileTime.fromMillis(atime.getTime()) : null),
			(ctime != null ? FileTime.fromMillis(ctime.getTime()) : null),
			(mtime != null ? FileTime.fromMillis(mtime.getTime()) : null));
	}

	@Override public Date getAccessTime() {
		// We don't store the Date object because Date is not immutable.
		return new Date(atime.toMillis());
	}

	@Override public Date getStatusChangeTime() {
		// We don't store the Date object because Date is not immutable.
		return new Date(ctime.toMillis());
	}

	@Override public Date getModificationTime() {
		// We don't store the Date object because Date is not immutable.
		return new Date(mtime.toMillis());
	}

}
