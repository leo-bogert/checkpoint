package checkpoint.datamodel;

import java.io.IOException;
import java.nio.file.Path;

public interface ICheckpoint {

	/** @throws IllegalArgumentException If a node with the given
	 *     {@link INode#getPath()} is already contained . */
	void addNode(INode n) throws IllegalArgumentException;

	ICheckpoint load(Path checkpointDir) throws IOException;

	void save(Path checkpointDir) throws IOException;

}
