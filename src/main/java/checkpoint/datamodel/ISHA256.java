package checkpoint.datamodel;

import checkpoint.datamodel.implementation.JavaSHA256;

/** Provides the same hash values as GNU coreutils' sha256sum command with the
 *  '--binary' flag.
 *  
 *  Java does not support static abstract interface functions so there are
 *  commented-out functions which are also required to be implemented.
 *  
 *  Implemented at {@link JavaSHA256} using Java's SHA256 implementation.
 *  TODO: Performance: Provide alternate implementations and benchmark which one
 *  is the fastest. Candidates: BouncyCastle, Apache Java Commons. */
public interface ISHA256 {

	/** Must obey {@link Thread#isInterrupted()} by throwing
	 *  {@link InterruptedException} because we will use it upon arbitrarily
	 *  large user-supplied files. */
	/*
	static ISHA256 sha256fromFile(Path p)
		throws IOException, InterruptedException;
	 */

	// FIXME: Implement using https://stackoverflow.com/a/9655275
	/** Returns a hex-encoded string which can be decoded using
	 *  {@link #sha256fromString(String)}. */
	String toString();

	/** @throws NumberFormatException If the hex encoding is not valid. */
	/*
	static ISHA256 sha256fromString(String hexEncoded)
		throws NumberFormatException;
	 */
}
