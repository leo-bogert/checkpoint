package checkpoint.datamodel;

import java.nio.file.Path;

/** Represents an entry of a checkpoint.
 *  Either a file or a directory on disk. */
public interface Node {

	Path getPath();

	/** Returns null for directories.
	 * 
	 *  TODO: Actually do return a hash by hashing the hashes of all files and
	 *  directories inside of it. */
	SHA256 getHash();

	Timestamps getTimetamps();

}
