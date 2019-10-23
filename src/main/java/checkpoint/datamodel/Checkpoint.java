package checkpoint.datamodel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public interface Checkpoint {

	/** @throws IllegalArgumentException If a node with the given
	 *     {@link Node#getPath()} is already contained . */
	void addNode(Node n) throws IllegalArgumentException;

	Checkpoint load(Path checkpointDir) throws IOException;

	void save(Path checkpointDir) throws IOException;

}
