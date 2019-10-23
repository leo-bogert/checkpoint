package checkpoint.datamodel.implementation;

import java.io.IOException;
import java.nio.file.Path;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

public final class Checkpoint implements ICheckpoint {

	@Override public void addNode(INode n) throws IllegalArgumentException {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public void save(Path checkpointDir) throws IOException {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public Checkpoint load(Path checkpointDir) throws IOException {
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}