package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.SHA256.sha256fromString;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.apache.commons.codec.DecoderException;
import org.junit.Test;

import checkpoint.datamodel.ISHA256;

/** Tests {@link SHA256}. */
public final class SHA256Test {

	@Test public void testSha256ToAndFromString() throws DecoderException {
		String hash =
			"7dd91e07f0341646d53f6938278a4d3e87961fabea066f7e6f40b7398f3b0b0f";
		
		assertEquals(hash, SHA256.sha256fromString(hash).toString());
		
		try {
			// Too short input
			SHA256.sha256fromString("00");
			fail();
		} catch(DecoderException e) {}
	}

	@Test public void testToBytes() throws DecoderException {
		String hash = // All bytes = 0, the last = 1
			"0000000000000000000000000000000000000000000000000000000000000001";
		SHA256 sha = sha256fromString(hash);
		
		// Test basic equality of the returned array to what it should be.
		
		byte[] bytes = sha.toBytes();
		assertEquals(256 / 8, bytes.length);
		
		// new byte[] defaults to all entries being 0, which matches our hash up
		// to the last byte.
		int allButLast = bytes.length - 1;
		assertArrayEquals(new byte[allButLast],
			Arrays.copyOf(bytes, allButLast));
		
		assertEquals((byte)1, bytes[bytes.length - 1]);
		
		// Test if a clone() of the array is returned upon every call.
		// This is critical because Java arrays cannot be immutable.
		
		assertNotSame(sha.toBytes(), sha.toBytes());
	}

	@Test public void testEquals() throws DecoderException {
		String hashA =
			"0000000000000000000000000000000000000000000000000000000000000000",
		       hashB = // Different by one bit
			"0000000000000000000000000000000000000000000000000000000000000001";
		
		SHA256 shaA = sha256fromString(hashA),
		       shaB = sha256fromString(hashB);
		
		assertFalse(shaA.equals(shaB));
		assertTrue( shaA.equals(shaA));
		assertTrue( shaA.equals(sha256fromString(hashA)));

		assertNotNull(shaA);
		try {
			shaA.equals(null);
			fail();
		} catch(NullPointerException e) {
			// Success
		}
		
		try {
			// Not an instance of ISHA256.
			shaA.equals(new Object());
			fail();
		} catch(UnsupportedOperationException e) {
			// Success
		}
		
		// Arrays.equals() returns true when passing two null pointers instead
		// of two byte[], and the implementation of SHA256 uses that function.
		// Thus check SHA256.equals() to be safe against the internal byte[] of
		// the object being null, which constructForUnitTestOnly(null) does.
		// We only use constructForUnitTestOnly(null) to construct the left-hand
		// object of equals() and instead pass an anonymous class into the
		// right-hand argument because SHA256.toBytes() would throw a
		// NullPointerException if we passed a SHA256 at the right side.
		// That NullPointerException would hide the fact that a bogus
		// equals() implementation would not throw it for the case we hereby
		// test, i.e. toBytes() returning null.
		ISHA256 x = SHA256.constructForUnitTestOnly(null);
		ISHA256 y = new ISHA256() {
			@Override public byte[] toBytes() {
				return null;
			}
		};
		assertNotNull(x);
		assertNotNull(y);
		try {
			x.equals(y);
			fail();
		} catch(NullPointerException e) {
			// Success
		}
	}

	@Test public void testHashCode() throws DecoderException {
		String hashA =
			"7dd91e07f0341646d53f6938278a4d3e87961fabea066f7e6f40b7398f3b0b0f",
		       hashB =
			"3ea600325ec065453cad9753910a8e811822aa93b479073a269cc497e17b0fec";
		
		SHA256 shaA1 = sha256fromString(hashA),
		       shaA2 = sha256fromString(hashA),
		       shaB = sha256fromString(hashB);
		
		assertNotEquals(shaA1.hashCode(), shaB.hashCode());
		assertEquals(shaA1.hashCode(), shaA1.hashCode());
		assertNotSame(shaA1, shaA2);
		assertEquals(shaA1.hashCode(), shaA2.hashCode());
		
		SHA256 x = SHA256.constructForUnitTestOnly(null);
		assertNotNull(x);
		try {
			x.hashCode();
			fail("Must throw if byte[] sha256 is null!");
		} catch(NullPointerException e) {
			// Success
		}
	}

}
