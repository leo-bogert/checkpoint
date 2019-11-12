package checkpoint.datamodel.implementation;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.codec.DecoderException;

import checkpoint.datamodel.ISHA256;
import checkpoint.generation.JavaSHA256Generator;

public final class JavaSHA256 implements ISHA256 {

	public static int READ_BUFFER_SIZE = 1024 * 1024;

	private final byte[] sha256;

	// FIXME: Make private and add factory function for JavaSHA256Generator to
	// use instead
	public JavaSHA256(byte[] sha256) {
		this.sha256 = sha256;
	}

	static JavaSHA256 constructForUnitTestOnly(byte[] sha256) {
		return new JavaSHA256(sha256);
	}

	/**
	 * @deprecated Use {@link JavaSHA256Generator#sha256ofFile(Path)} instead
	 *     FIXME: Replace everywhere
	 */
	@Deprecated
	public static JavaSHA256 sha256ofFile(Path p)
			throws IOException, InterruptedException {
		
		return new JavaSHA256Generator().sha256ofFile(p);
	}

	@Override public String toString() {
		return encodeHexString(sha256);
	}

	public static JavaSHA256 sha256fromString(String hexEncoded)
			throws DecoderException {
		
		if(hexEncoded.length() != 64) {
			throw new DecoderException(
				"Invalid length for hex-encoded SHA256, should be 64: "
				+ hexEncoded.length());
		}
		
		// TODO: Performance: Remove the toCharArray() once we're fine with
		// requiring a more recent Apache Java Commons Codec library.
		return new JavaSHA256(decodeHex(hexEncoded.toCharArray()));
	}

	public byte[] toBytes() {
		return sha256.clone();
	}

	@Override public int hashCode() {
		requireNonNull(sha256);
		return Arrays.hashCode(sha256);
	}

	@Override public boolean equals(Object obj) {
		requireNonNull(obj);
		
		if(!(obj instanceof ISHA256)) {
			throw new UnsupportedOperationException(
					"Does not implement ISHA256: " + obj);
		}
		
		// Arrays.equals() returns true for two null-pointers as argument so
		// make sure we don't pass null.
		byte[] a = requireNonNull(sha256);
		byte[] b = requireNonNull(((ISHA256)obj).toBytes());
		
		return Arrays.equals(a, b);
	}

}
