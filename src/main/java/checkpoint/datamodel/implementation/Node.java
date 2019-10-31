package checkpoint.datamodel.implementation;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import checkpoint.datamodel.INode;
import checkpoint.datamodel.ISHA256;
import checkpoint.datamodel.ITimestamps;

public final class Node implements INode {

	private final Path        path;
	private final boolean     isDirectory;
	private final ISHA256     sha256;
	private final ITimestamps timestamps;

	private Node(Path path, boolean isDirectory, ISHA256 sha256,
			ITimestamps timestamps) {
		
		this.path        = requireNonNull(path);
		this.isDirectory = isDirectory;
		this.sha256      = sha256;
		this.timestamps  = timestamps;
	}
	@Override public Path getPath() {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public boolean isDirectory() {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public ISHA256 getHash() {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public ITimestamps getTimetamps() {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
