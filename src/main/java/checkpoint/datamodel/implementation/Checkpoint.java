package checkpoint.datamodel.implementation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.TreeMap;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

public final class Checkpoint implements ICheckpoint {

	/** Storage of the {@link INode}s which have been added via
	 *  {@link #addNode(INode)}.
	 *  Beyond storage this also has the purpose of implicitly sorting them by
	 *  their {@link INode#getPath()} since our human readable file format
	 *  of {@link #save(Path)} will sort by path.
	 * 
	 *  FIXME: Code quality: Provide an explicit {@link Comparator} because
	 *  {@link Path#compareTo(Path)}'s JavaDoc says the ordering is platform
	 *  specific. 
	 *  FIXME: Performance: Replace with data structure which supports fast
	 *  concurrent adding so our many generator threads can deal with the
	 *  sorting in parallel. */
	private final TreeMap<Path, INode> nodes = new TreeMap<>();

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