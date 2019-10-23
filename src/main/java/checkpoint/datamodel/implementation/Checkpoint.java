package checkpoint.datamodel.implementation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

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
	 *  sorting in parallel. Perhaps {@link ConcurrentSkipListMap}? */
	private final TreeMap<Path, INode> nodes = new TreeMap<>();


	@Override public synchronized void addNode(INode n)
			throws IllegalArgumentException {
		
		// Instead of using putIfAbsent() so we can throw if the key is already
		// contained we put() the new INode and replace it with put()ing old one
		// again if put()'s return value tells us that there already was an
		// entry for the key.
		// We do so because that is faster than putIfAbsent():
		// putIfAbsent() will first do a contains() check, and then put().
		// This means that the tree of the map has to be walked twice in the
		// regular case of no duplicate existing, whereas our approach only
		// walks twice if there is a duplicate - which should never happen if
		// the code has no bugs.
		Path key = n.getPath();
		INode oldValue = nodes.put(key, n);
		if(oldValue != null) {
			nodes.put(key, oldValue);
			
			throw new IllegalArgumentException(
				"Bug, please report: INode already contained for path: " + key);
		}
	}

	@Override public synchronized void save(Path checkpointDir)
			throws IOException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public synchronized Checkpoint load(Path checkpointDir)
			throws IOException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}