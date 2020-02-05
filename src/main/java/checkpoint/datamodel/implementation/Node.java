package checkpoint.datamodel.implementation;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import checkpoint.datamodel.INode;
import checkpoint.datamodel.ISHA256;
import checkpoint.datamodel.ITimestamps;

public final class Node implements INode {

	private final Path        path;
	private final boolean     isDirectory;
	private final long        size;
	private       ISHA256     sha256;
	private       ITimestamps timestamps;

	private Node(Path path, boolean isDirectory, long size,
			ISHA256 sha256, ITimestamps timestamps) {
		
		this.path        = requireNonNull(path);
		this.isDirectory = isDirectory;
		this.size        = size;
		this.sha256      = sha256;
		this.timestamps  = timestamps;
		
		if(isDirectory) {
			if(sha256 != null || size != 0)
				throw new IllegalArgumentException();
		}
		
		if(size < 0)
			throw new IllegalArgumentException();
	}

	public static Node constructNode(Path path, boolean isDirectory, long size,
			ISHA256 sha256, ITimestamps timestamps) {
		
		return new Node(path, isDirectory, size, sha256, timestamps);
	}

	public static Node constructNode(Path path, boolean isDirectory,
			long size) {
		
		return new Node(path, isDirectory, size, null, null);
	}

	@Override public Path getPath() {
		// Path is immutable so we don't need to clone().
		return path;
	}

	@Override public boolean isDirectory() {
		return isDirectory;
	}

	@Override public long getSize() {
		return size;
	}

	@Override public ISHA256 getHash() {
		// ISHA256 is immutable so we don't need to clone().
		return sha256;
	}

	@Override public void setHash(ISHA256 sha256) {
		this.sha256 = sha256;
	}

	@Override public ITimestamps getTimetamps() {
		// ITimestamps is immutable so we don't need to clone().
		return timestamps;
	}

	@Override public void setTimestamps(ITimestamps timestamps) {
		this.timestamps = timestamps;
	}

}
