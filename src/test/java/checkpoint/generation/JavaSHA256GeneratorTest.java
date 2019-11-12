package checkpoint.generation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class JavaSHA256GeneratorTest {

	@Rule
	public final TemporaryFolder tempDir = new TemporaryFolder();

	@Test public void testSha256ofFile()
			throws IOException, InterruptedException, NoSuchAlgorithmException {
		
		// We will re-use the object for generating multiple hashes to ensure
		// the implementation is safe w.r.t. that.
		final JavaSHA256Generator g = new JavaSHA256Generator();
		
		Path p = tempDir.newFile().toPath();
		// Append CRLF to test data to see if the binary mode which ISHA256
		// requires is obeyed.
		write(p, "Test\r\n".getBytes(UTF_8));
		// echo -ne "Test\r\n" | sha256sum --binary
		assertEquals(
			"7dd91e07f0341646d53f6938278a4d3e87961fabea066f7e6f40b7398f3b0b0f",
			g.sha256ofFile(p).toString());
		
		// Test with a file larger than the read buffer of sha256ofFile() to
		// ensure bugs related to the "while(read(buffer..." loop are caught.
		byte[] bytes
			= new byte[JavaSHA256Generator.DEFAULT_READ_BUFFER_SIZE * 3];
		long seed = new Random().nextLong();
		new Random(seed).nextBytes(bytes);
		Path largeFile = tempDir.newFile().toPath();
		write(largeFile, bytes);
		assertEquals("Failed for seed: " + seed,
			encodeHexString(MessageDigest.getInstance("SHA-256").digest(bytes)),
			g.sha256ofFile(largeFile).toString());
	}

}
