package checkpoint.datamodel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/** Searches the filesystem for {@link INode}s eligible for inclusion when
 *  creating a {@link ICheckpoint}.
 *  TODO: Move this and implementation to package checkpoint.generation? */
public interface INodeFinder {

	/** The set and Paths of the resulting INodes must be equal to the shell
	 *  command:
	 *      cd inputDir && find . -mount \( -type f -o -type d \) -print0
	 *   
	 *  The paths must not end with a null-byte: The -print0 is merely included
	 *  here to not spread a dangerous shell command which wouldn't work with
	 *  files whose name contains a linebreak.
	 *  
	 *  FIXME: Support Thread.interrupt(). */
	Collection<INode> findNodes(Path inputDir) throws IOException;

}
