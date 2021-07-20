package checkpoint.datamodel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.SortedMap;

import checkpoint.datamodel.implementation.Node;

/** All functions are safe to be called concurrently both with regards to
 *  themselves and other functions of this interface. */
public interface ICheckpoint {

	/** @throws IllegalArgumentException If a node with the given
	 *     {@link INode#getPath()} is already contained.
	 *     The additional execution time for checking this instead of relying
	 *     upon callers to be bug-free is acceptable because:
	 *     - implementations shall use a data structure which provides implicit
	 *       sorting since {@link #save(Path)} will need sorting anyway
	 *     - and such data structures will usually also determine if the given
	 *       element is already contained when trying to add it, so that
	 *       information is available for free anyway. */
	void addNode(INode n) throws IllegalArgumentException;

	void save(Path checkpointDir) throws IOException;

	// Java does not support static abstract interface methods, so the following
	// is required but commented out:
	/* static ICheckpoint load(Path checkpointDir) throws IOException; */

	/** Set to true if all {@link Node}s which are available on the filesystem
	 *  have been computed and stored in the checkpoint.
	 *  Persisted by {@link #save(Path)} and loaded by load().
	 * 
	 *  The EOF-marker of the resulting plain-text files will tell the user if
	 *  the checkpoint is complete or not, depending upon this boolean. You must
	 *  thus keep this at the default value of false if you're doing an
	 *  intermediate save - which you might want to do every N minutes. */
	void setCompleteFlag(boolean isComplete);

	/** @see #setCompleteFlag(boolean) */
	boolean isComplete();

	/** Returns the highest {@link Date} found in the
	 *  {@link INode#getTimetamps()} of all contained {@link INode}s.  
	 *  If no dates or no nodes are contained a Date with Unix time of 
	 *  {@link Long#MIN_VALUE} is returned. */
	Date getDateEstimate();

	int getNodeCount();

	/** Returns the sum of {@link INode#getSize()} of all contained nodes.
	 *  Please notice that this will be 0 after loading a checkpoint from disk
	 *  because we don't include it in the file format yet. A TODO for that
	 *  exists at {@link INode#getSize()}. */
	long getNodeSize();

	/** NOTICE: You MUST synchronize on this ICheckpoint object while accessing
	 *  the returned map!
	 *  Implementations should enforce this by containing:
	 *      assert(Thread.holdsLock(this));
	 *  
	 *  TODO: Java 8: Replace with a function which consumes an object which
	 *  implements a functional interface (see JavaDoc of package
	 *  java.util.function) and apply the object's functional method upon the
	 *  map inside of this function instead of having the caller iterate over
	 *  the map. Then this function can synchronize on its own. */
	SortedMap<Path, INode> getNodes();

	int getHashingFailureCount();

	int getTimestampingFailureCount();

}
