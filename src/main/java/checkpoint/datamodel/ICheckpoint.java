package checkpoint.datamodel;

import java.io.IOException;
import java.nio.file.Path;

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

	ICheckpoint load(Path checkpointDir) throws IOException;

}
