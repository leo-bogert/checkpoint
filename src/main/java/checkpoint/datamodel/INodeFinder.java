package checkpoint.datamodel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/** Searches the filesystem for {@link INode}s eligible for inclusion when
 *  creating a {@link ICheckpoint}. */
public interface INodeFinder {

	/** The set and Paths of the resulting INodes must be equal to the shell
	 *  command:
	 *      cd inputDir ; find . -mount \( -type f -o -type d \) -print0
	 *   
	 *  (The paths do not have to end with a null-byte: The -print0 is merely
	 *  included here to not spread dangerous shell commands which wouldn't work
	 *  with files whose name contains a linebreak.) */
	Collection<INode> findNodes(Path inputDir) throws IOException;

}
