package checkpoint.datamodel.implementation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests {@link JavaSHA256}. */
public final class JavaSHA256Test {

	@Rule
	public final TemporaryFolder tempDir = new TemporaryFolder();

	@Test public void testSha256ofFile()
			throws IOException, InterruptedException {
		
		Path p = tempDir.newFile().toPath();
		// Append CRLF to test data to see if the binary mode which ISHA256
		// requires is obeyed.
		write(p, "Test\r\n".getBytes(UTF_8));
		// echo -ne "Test\r\n" | sha256sum --binary
		assertEquals(
			"7dd91e07f0341646d53f6938278a4d3e87961fabea066f7e6f40b7398f3b0b0f",
			JavaSHA256.sha256ofFile(p).toString());
	}

	@Test public void testToString() {
		fail("Not yet implemented");
	}

	@Test public void testSha256fromString() {
		fail("Not yet implemented");
	}

	@Test public void testEquals() {
		fail("Not yet implemented");
	}

	@Test public void testHashCode() {
		fail("Not yet implemented");
	}

}
