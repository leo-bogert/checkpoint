package checkpoint.datamodel;

import java.nio.file.Path;

/** Represents an entry of a checkpoint.
 *  Either a file or a directory on disk. */
public interface INode {

	Path getPath();

	/** Returns null for directories.
	 * 
	 *  TODO: Actually do return a hash by hashing the hashes of all files and
	 *  directories inside of it. */
	ISHA256 getHash();

	Timestamps getTimetamps();

}
