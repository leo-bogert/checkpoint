package checkpoint.datamodel.implementation;

import java.nio.file.Path;

import checkpoint.datamodel.INode;
import checkpoint.datamodel.ISHA256;
import checkpoint.datamodel.ITimestamps;

public final class Node implements INode {

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
