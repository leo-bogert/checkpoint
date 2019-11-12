package checkpoint.datamodel;

import java.util.Arrays;

import checkpoint.datamodel.implementation.JavaSHA256;

/** Interface for storage classes for SHA256 hashes.
 *  Generation happens via {@link ISHA256Generator} instead.
 *  
 *  Java does not support static abstract interface functions so there are
 *  commented-out functions which are also required to be implemented. */
public interface ISHA256 {

	/** Must obey {@link Thread#isInterrupted()} by throwing
	 *  {@link InterruptedException} because we will use it upon arbitrarily
	 *  large user-supplied files. */
	/*
	static ISHA256 sha256ofFile(Path p)
		throws IOException, InterruptedException;
	 */

	/** Returns a hex-encoded string which can be decoded using
	 *  {@link #sha256fromString(String)}. */
	String toString();

	/** @throws DecoderException If the hex encoding is not valid. */
	/*
	static ISHA256 sha256fromString(String hexEncoded)
		throws DecoderException;
	 */

	/** Make sure to clone() the underlying array when implementing this, Java
	 *  arrays are not immutable even if final! */
	byte[] toBytes();

	/**
	 *  Same as {@link Arrays#hashCode(byte[])} upon {@link #toBytes()} with
	 *  one difference:
	 *  If {@link #toBytes()} would return null or throw
	 *  {@link NullPointerException} then {@link NullPointerException} must be
	 *  thrown.
	 *  See {@link #equals(Object)} for the defensive programming reasons
	 *  behind this.
	 *  
	 *  WARNING: Arrays.hashCode() does not throw on its own, you must do that
	 *  manually! */
	@Override int hashCode();

	/** Must return true if {@link #getBytes()} returns an equal non-null (!)
	 *  byte[] for both this and a given object which implements ISHA256.
	 *  
	 *  WARNING: Arrays.equals() will consider two null pointers to be equal!
	 *  
	  * If {@link #getBytes()} for this or the given object is null or the
	  * object is null, {@link NullPointerException} must be thrown.
	  * If the given object does not implement ISHA256
	  * {@link UnsupportedOperationException} must be thrown.
	  * 
	  * By the above throwing we intentionally violate the contract of
	  * {@link Object#equals(Object)}:
	  * The design goal of the Checkpoint-tool is to ensure maximum
	  * reliability against data corruption so we must make very sure not to
	  * have bugs which would cause wrongly assuming file hashes do match. */
	@Override boolean equals(Object obj);

}
