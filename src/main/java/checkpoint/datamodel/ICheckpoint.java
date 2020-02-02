package checkpoint.datamodel;

import java.io.IOException;
import java.nio.file.Path;

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

	int getNodeCount();

	/** Returns the sum of {@link INode#getSize()} of all contained nodes.
	 *  Please notice that this will be 0 after loading a checkpoint from disk
	 *  because we don't include it in the file format yet. A TODO for that
	 *  exists at {@link INode#getSize()}. */
	long getNodeSize();

	int getHashingFailureCount();

	int getTimestampingFailureCount();

}
