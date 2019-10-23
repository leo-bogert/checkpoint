package checkpoint.datamodel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/** Provides the same hash values as GNU coreutils' sha256sum command with the
 *  '--binary' flag. */
public interface ISHA256 {

	ISHA256 sha256fromFile(Path p)
		throws FileNotFoundException, IOException, InterruptedException;

	// FIXME: Implement using https://stackoverflow.com/a/9655275
	/** Returns a hex-encoded string which can be decoded using
	 *  {@link #sha256fromString(String)}. */
	String toString();

	/** @throws NumberFormatException If the hex encoding is not valid. */
	ISHA256 sha256fromString(String hexEncoded) throws NumberFormatException;

}
