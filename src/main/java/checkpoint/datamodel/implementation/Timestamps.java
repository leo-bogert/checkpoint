package checkpoint.datamodel.implementation;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import checkpoint.datamodel.ITimestamps;

public final class Timestamps extends ITimestamps {

	public static Timestamps readTimestamps(Path p) {
		throw new UnsupportedOperationException("FIXME: Implement!");
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
