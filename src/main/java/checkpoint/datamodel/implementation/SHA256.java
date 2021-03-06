package checkpoint.datamodel.implementation;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.util.Arrays;

import org.apache.commons.codec.DecoderException;

import checkpoint.datamodel.ISHA256;

public final class SHA256 implements ISHA256 {

	private final byte[] sha256;

	private SHA256(byte[] sha256) {
		// Allow null because callers of constructForUnitTestOnly() need it.
		// clone() to be safe from future external modifications.
		this.sha256 = sha256 != null ? sha256.clone() : null;
	}

	/** Allows the input to be null and thus must only be used in tests. */
	static SHA256 constructForUnitTestOnly(byte[] sha256) {
		return new SHA256(sha256);
	}

	public static SHA256 construct(byte[] sha256) {
		return new SHA256(requireNonNull(sha256));
	}

	@Override public String toString() {
		return encodeHexString(sha256);
	}

	public static SHA256 sha256fromString(String hexEncoded)
			throws DecoderException {
		
		if(hexEncoded.length() != 64) {
			throw new DecoderException(
				"Invalid length for hex-encoded SHA256, should be 64: "
				+ hexEncoded.length());
		}
		
		// TODO: Performance: Remove the toCharArray() once we're fine with
		// requiring a more recent Apache Java Commons Codec library.
		return new SHA256(decodeHex(hexEncoded.toCharArray()));
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
