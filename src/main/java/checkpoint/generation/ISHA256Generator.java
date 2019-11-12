package checkpoint.generation;

import java.io.IOException;
import java.nio.file.Path;

import checkpoint.datamodel.ISHA256;

/** Generates the {@link ISHA256} values of files on disk.
 *  Thus this can be seen as a builder for {@link ISHA256} instances, and those
 *  can be seen as storage objects for the hash.
 * 
 *  Instances are supposed to be long-lived and used for generating hashes of
 *  many different files.
 *  This is to avoid garbage-collection churn through frequent allocation of
 *  large I/O buffers.
 *  
 *  Implemented at {@link JavaSHA256Generator} using Java's SHA256
 *  implementation. */
public interface ISHA256Generator {

	/** Provides the same hash values as GNU coreutils' sha256sum command with
	 *  the '--binary' flag.
	 *  
	 *  Must obey {@link Thread#isInterrupted()} by throwing
	 *  {@link InterruptedException} because we will use it upon arbitrarily
	 *  large user-supplied files. */
	ISHA256 sha256ofFile(Path p) throws IOException, InterruptedException;

}
