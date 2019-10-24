package checkpoint.datamodel.implementation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import checkpoint.datamodel.ISHA256;

/** Implements {@link ISHA256} using Java's default SHA256 implementation. */
public final class JavaSHA256 implements ISHA256 {

	private final byte[] sha256;

	private JavaSHA256(byte[] sha256) {
		this.sha256 = sha256;
	}

	@Override public JavaSHA256 sha256fromFile(Path p)
			throws FileNotFoundException, IOException, InterruptedException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

	@Override public JavaSHA256 sha256fromString(String hexEncoded)
			throws NumberFormatException {
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}
