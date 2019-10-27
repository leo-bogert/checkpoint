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
		new Random().nextBytes(bytes);
		Path largeFile = tempDir.newFile().toPath();
		write(largeFile, bytes);
		assertEquals(
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
	}

	@Test public void testHashCode() {
		fail("Not yet implemented");
	}

}
