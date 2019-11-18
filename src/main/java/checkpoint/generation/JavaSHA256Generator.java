package checkpoint.generation;

import static java.nio.file.StandardOpenOption.READ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import checkpoint.datamodel.implementation.NodeFinder;
import checkpoint.datamodel.implementation.SHA256;

// FIXME: Trim visibility after SHA256 doesn't need to access it anymore.
/** Implements {@link ISHA256Generator} using Java's default SHA256
 *  implementation.
 *  
 *  TODO: Performance: Provide alternate implementations and benchmark which one
 *  is the fastest. Candidates: BouncyCastle, Apache Java Commons. */
public final class JavaSHA256Generator implements ISHA256Generator {

	/** In bytes.
	 *  Default is equal to the default read ahead amount of Linux 4.15 as
	 *  observed from '/sys/block/sda/queue/read_ahead_kb'. The documentation
	 *  for that is at:
	 *  https://www.kernel.org/doc/Documentation/block/queue-sysfs.txt
	 *  
	 *  Choosing this to be equal to the read ahead is a good idea because after
	 *  each time we read into the buffer we will be spending some time upon
	 *  applying SHA256 to it, so the kernel should be able to read ahead a full
	 *  buffer during that.
	 *  If we did choose more than the kernel's read ahead it wouldn't be able
	 *  to instantly satisfy our next request after applying SHA256.
	 *  
	 *  TODO: Performance: Benchmark using slightly less than the kernel
	 *  read-ahead to ensure we constantly request data from the kernel even
	 *  if there is some jitter.
	 *  
	 *  FIXME: Performance: Use two buffers and while hashing one of them read
	 *  into the other one using asynchronous I/O. This will allow us to do more
	 *  reading ahead than the kernel's small 128 KiB.
	 *  Once this is implemented remove the comment about the kernel read ahead
	 *  buffer in CreateCommand's description for --buffer.
	 *  Then perhaps even measure file size in {@link NodeFinder} and choose
	 *  buffer size to be large enough so that the majority of files, e.g. 80%,
	 *  will fit into it.
	 *  This will require some heuristics to choose an upper boundary though
	 *  because it may not fit into memory otherwise, especially considering
	 *  that {@link ConcurrentCheckpointGenerator} generates multiple threads
	 *  where each has a JavaSHA256Generator. */
	public static final int DEFAULT_READ_BUFFER_SIZE = 128 * 1024;

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
