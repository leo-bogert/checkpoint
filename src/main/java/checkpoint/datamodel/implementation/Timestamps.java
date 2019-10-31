package checkpoint.datamodel.implementation;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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

	@Override public FileTime getAccessTime() {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public FileTime getStatusChangeTime() {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public FileTime getModificationTime() {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
