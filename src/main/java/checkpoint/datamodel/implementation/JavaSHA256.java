package checkpoint.datamodel.implementation;

import static java.nio.file.StandardOpenOption.READ;
import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.DecoderException;

import checkpoint.datamodel.ISHA256;

/** Implements {@link ISHA256} using Java's default SHA256 implementation. */
public final class JavaSHA256 implements ISHA256 {

	private final byte[] sha256;

	private JavaSHA256(byte[] sha256) {
		this.sha256 = sha256;
	}

	public static JavaSHA256 sha256fromFile(Path p)
			throws IOException, InterruptedException {
		
		// TODO: Performance: Recycle the MessageDigest objects using reset(),
		// by changing this class to be non-immutable = having this function
		// (and sha256fromString()) not be static and storing the MessageDigest
		// in the class.
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
		Thread thread = Thread.currentThread();
		
		SeekableByteChannel channel = Files.newByteChannel(p, READ);
		try {
			// FIXME: Performance: Support overriding buffer size on command
			// line and determine a good one.
			// FIXME: Adjust buffer size automatically from channel.size().
			// FIXME: Performance: Use two buffers and while hashing one of
			// them read into the other one asynchronously.
			// FIXME: Performance: Try if a direct buffer, obtainable using
			// allocateDirect(), speeds up the function.
			// First make sure to read the warnings about that at ByteBuffer's
			// top-level JavaDoc.
			ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
			while(channel.read(buffer) > 0) {
				// rewind() & flip() are difficult to understand, but they're
				// what the Java tutorial recommends for reading a file at
				// section "Reading, Writing and Creating files".
				buffer.rewind();
				md.update(buffer);
				buffer.flip();
				
				// TODO: Performance: Try if checking this only every N'th
				// iteration provides a noticeable improvement.
				if(thread.isInterrupted())
					throw new InterruptedException();
			}
			
			return new JavaSHA256(md.digest());
		} finally {
			channel.close();
		}
	}

	@Override public String toString() {
		return encodeHexString(sha256);
	}

	public static JavaSHA256 sha256fromString(String hexEncoded)
			throws DecoderException {
		
		return new JavaSHA256(decodeHex(hexEncoded));
	}

}
