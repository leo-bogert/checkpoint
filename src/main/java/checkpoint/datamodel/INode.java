package checkpoint.datamodel;

import java.nio.file.Path;

/** Represents an entry of a checkpoint.
 *  Either a file or a directory on disk.
 *  
 *  Java does not support static abstract interface functions so there are
 *  commented-out functions which are also required to be implemented. */
public interface INode {

	/** The path must not be null, sha256 and timestamps may be null for the
	 *  reasons explained at their getters. */
	/*
	static INode constructNode(Path path, boolean isDirectory, ISHA256 sha256,
		ITimestamps timestamps);
	*/

	/** Must never be null. */
	Path getPath();

	boolean isDirectory();

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
