package checkpoint.datamodel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public interface SHA256 {

	SHA256 sha256fromString(String hexEncoded);

	SHA256 sha256fromFile(Path p)
		throws FileNotFoundException, IOException, InterruptedException;

	// FIXME: Implement using https://stackoverflow.com/a/9655275
	String toString();

}
