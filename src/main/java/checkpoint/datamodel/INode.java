package checkpoint.datamodel;

import java.nio.file.Path;

/** Represents an entry of a checkpoint.
 *  Either a file or a directory on disk. */
public interface INode {

	/** Must never be null. */
	Path getPath();

	/** Returns null for directories.
	 *  TODO: Actually do return a hash by hashing the hashes of all files and
	 *  directories inside of it.
	 *  
	 *  For non-directories may return null if computing the ISHA256 failed
	 *  due to e.g. IOException. */
	ISHA256 getHash();

	/** May return null if reading the ITimestamps failed due to e.g.
	 *  IOException. */
	ITimestamps getTimetamps();

}
