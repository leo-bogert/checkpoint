package checkpoint.datamodel;

import java.nio.file.Path;

import checkpoint.generation.ConcurrentCheckpointGenerator;

/** Represents an entry of a checkpoint.
 *  Either a file or a directory on disk.
 *  
 *  Java does not support static abstract interface functions so there are
 *  commented-out functions which are also required to be implemented. */
public interface INode {

	/** The path must not be null, sha256 and timestamps may be null for the
	 *  reasons explained at their getters.
	 *  
	 *  The given path must follow the constraints specified at getPath(), the
	 *  size those at getSize().
	 *  FIXME: We don't actually validate whether it does when loading an
	 *  existing Chechkpoint from disk. */
	/*
	static INode constructNode(Path path, boolean isDirectory, long size,
		ISHA256 sha256, ITimestamps timestamps);
	*/

	/** Initializes the ISHA256 and ITimestamps to null.
	 * 
	 *  The given path must follow the constraints specified at getPath(), the
	 *  size those at getSize(). */
	/*
	static INode constructNode(Path path, boolean isDirectory, long size);
	*/

	/** Must never be null.
	 *  Must be relative to the checkpoint's input directory and prefixed with
	 *  "./".
	 *  E.g. when creating a checkpoint upon directory "/a", the file "/a/b/c"
	 *  should have a getPath() value of "./b/c" */
	Path getPath();

	boolean isDirectory();

	/** Returns the size of the file if available.
	 *  Returns 0 for directories to ensure progress computation at
	 *  {@link ConcurrentCheckpointGenerator} is trivial.
	 * 
	 *  Returns 0 if the size is not available, which is the case if the
	 *  checkpoint has been loaded from disk as our file format currently does
	 *  not include it.
	 *  TODO: Allow including this in the checkpoint file. */
	long getSize();

	/** Returns null for directories.
	 *  TODO: Actually do return a hash by hashing the hashes of all files and
	 *  directories inside of it.
	 *  
	 *  For non-directories may return null if computing the ISHA256 failed
	 *  due to e.g. IOException, or if the ISHA256 has not been computed yet. */
	ISHA256 getHash();

	void setHash(ISHA256 sha256);

	/** May return null if reading the ITimestamps failed due to e.g.
	 *  IOException or if the ITimestamps have not been read yet. */
	ITimestamps getTimetamps();

	void setTimestamps(ITimestamps timestamps);

}
