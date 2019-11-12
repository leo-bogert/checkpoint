package checkpoint.generation;

import java.io.IOException;
import java.nio.file.Path;

import checkpoint.datamodel.ISHA256;

/** Generates the {@link ISHA256} values of files on disk.
 *  Thus this can be seen as a builder for {@link ISHA256} instances, and those
 *  can be seen as immutable storage objects for the hash.
 * 
 *  Instances are supposed to be long-lived and used for generating hashes of
 *  many different files.
 *  This is to avoid garbage-collection churn through frequent allocation of
 *  large I/O buffers. */
public interface ISHA256Generator {

	/** Must obey {@link Thread#isInterrupted()} by throwing
	 *  {@link InterruptedException} because we will use it upon arbitrarily
	 *  large user-supplied files. */
	ISHA256 sha256ofFile(Path p) throws IOException, InterruptedException;

}
