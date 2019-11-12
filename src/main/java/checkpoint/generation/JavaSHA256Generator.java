package checkpoint.generation;

import static java.nio.file.StandardOpenOption.READ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import checkpoint.datamodel.implementation.SHA256;

// FIXME: Trim visibility after SHA256 doesn't need to access it anymore.
/** Implements {@link ISHA256Generator} using Java's default SHA256
 *  implementation.
 *  
 *  TODO: Performance: Provide alternate implementations and benchmark which one
 *  is the fastest. Candidates: BouncyCastle, Apache Java Commons. */
public final class JavaSHA256Generator implements ISHA256Generator {

	/** In bytes.
	 *  FIXME: Performance: Determine a good default. */
	public static int DEFAULT_READ_BUFFER_SIZE = 1024 * 1024;

	/** Re-used to prevent memory allocation churn since we will hash **many**
	 *  files in typical usage of Checkpoint. */
	private final ByteBuffer buffer;

	public JavaSHA256Generator() {
		this(DEFAULT_READ_BUFFER_SIZE);
	}

	public JavaSHA256Generator(int readBufferBytes) {
		buffer = ByteBuffer.allocate(readBufferBytes);
	}

	public SHA256 sha256ofFile(Path p)
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
			// FIXME: Adjust buffer size automatically from channel.size()?
			// FIXME: Performance: Use two buffers and while hashing one of
			// them read into the other one asynchronously.
			// FIXME: Performance: Try if a direct buffer, obtainable using
			// allocateDirect(), speeds up the function.
			// First make sure to read the warnings about that at ByteBuffer's
			// top-level JavaDoc.
			buffer.clear();
			while(channel.read(buffer) > 0) {
				// FIXME: The Oracle Java tutorial wrongly says we should
				// rewind() the buffer before md.update() and then flip() it
				// afterwards, at section "Reading, Writing and Creating files"
				// at "Reading and Writing Files by Using Channel I/O".
				// See the debug logging added and quoted in commits:
				//     52f46c1d3965b9e69798b76ccdeb47ea97dada80
				//     f767a02d680a3a8604668187f9a4067029310842
				// Tell them it needs to be done like this instead:
				
				buffer.flip();
				md.update(buffer);
				// Its JavaDoc says it doesn't actually erase memory, just the
				// counters of the buffer, so this is fine to use w.r.t. speed.
				buffer.clear();
				
				// TODO: Performance: Try if checking this only every N'th
				// iteration provides a noticeable improvement.
				if(thread.isInterrupted())
					throw new InterruptedException();
			}
			
			return new SHA256(md.digest());
		} finally {
			channel.close();
		}
	}

}
