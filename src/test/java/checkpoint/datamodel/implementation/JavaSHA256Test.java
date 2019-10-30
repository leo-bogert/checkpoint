package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.JavaSHA256.sha256fromString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.commons.codec.DecoderException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import checkpoint.datamodel.ISHA256;

/** Tests {@link JavaSHA256}. */
public final class JavaSHA256Test {

	@Rule
	public final TemporaryFolder tempDir = new TemporaryFolder();

	@Test public void testSha256ofFile()
			throws IOException, InterruptedException, NoSuchAlgorithmException {
		
		Path p = tempDir.newFile().toPath();
		// Append CRLF to test data to see if the binary mode which ISHA256
		// requires is obeyed.
		write(p, "Test\r\n".getBytes(UTF_8));
		// echo -ne "Test\r\n" | sha256sum --binary
		assertEquals(
			"7dd91e07f0341646d53f6938278a4d3e87961fabea066f7e6f40b7398f3b0b0f",
			JavaSHA256.sha256ofFile(p).toString());
		
		// Test with a file larger than the read buffer of sha256ofFile() to
		// ensure bugs related to the "while(read(buffer..." loop are caught.
		byte[] bytes = new byte[JavaSHA256.READ_BUFFER_SIZE * 3];
		long seed = new Random().nextLong();
		new Random(seed).nextBytes(bytes);
		Path largeFile = tempDir.newFile().toPath();
		write(largeFile, bytes);
		assertEquals("Failed for seed: " + seed,
			encodeHexString(MessageDigest.getInstance("SHA-256").digest(bytes)),
			JavaSHA256.sha256ofFile(largeFile).toString());
	}

	@Test public void testSha256ToAndFromString() throws DecoderException {
		String hash =
			"7dd91e07f0341646d53f6938278a4d3e87961fabea066f7e6f40b7398f3b0b0f";
		
		assertEquals(hash, JavaSHA256.sha256fromString(hash).toString());
		
		try {
			// Too short input
			JavaSHA256.sha256fromString("00");
			fail();
		} catch(DecoderException e) {}
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test public void testEquals() throws DecoderException {
		String hashA =
			"0000000000000000000000000000000000000000000000000000000000000000",
		       hashB = // Different by one bit
			"0000000000000000000000000000000000000000000000000000000000000001";
		
		JavaSHA256 shaA = sha256fromString(hashA),
		           shaB = sha256fromString(hashB);
		
		assertFalse(shaA.equals(shaB));
		assertTrue( shaA.equals(shaA));
		assertTrue( shaA.equals(sha256fromString(hashA)));
		assertFalse(shaA.equals(null));
		assertFalse(shaA.equals(new Object()));
		
		// Arrays.equals() returns true when passing two null pointers instead
		// of two byte[], and the implementation of JavaSHA256 uses that
		// function.
		// Thus check JavaSHA256.equals() to be safe against the internal
		// byte[] of the object being null, which constructForUnitTestOnly(null)
		// does.
		// We only use constructForUnitTestOnly(null) to construct the left-hand
		// object of equals() and instead pass an anonymous class into the
		// right-hand argument because JavaSHA256.toBytes() would throw a
		// NullPointerException if we passed a JavaSHA256 at the right side.
		// That NullPointerException would hide the fact that a bogus
		// equals() implementation would not throw it for the case we hereby
		// test, i.e. toBytes() returning null.
		try {
			JavaSHA256.constructForUnitTestOnly(null).equals(
				new ISHA256() {
					@Override public byte[] toBytes() {
						return null;
					}
				});
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
		
		JavaSHA256 shaA1 = sha256fromString(hashA),
		           shaA2 = sha256fromString(hashA),
		           shaB = sha256fromString(hashB);
		
		assertNotEquals(shaA1.hashCode(), shaB.hashCode());
		assertEquals(shaA1.hashCode(), shaA1.hashCode());
		assertNotSame(shaA1, shaA2);
		assertEquals(shaA1.hashCode(), shaA2.hashCode());
	}

}
