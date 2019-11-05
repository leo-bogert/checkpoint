package checkpoint.datamodel.implementation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

// TODO: Test all functions
public final class TimestampsTest {

	@Rule
	public final TemporaryFolder tempDir = new TemporaryFolder();

	@Test public void testReadTimestamps() throws IOException {
		Path f = tempDir.newFile().toPath();
		assert(Files.exists(f));
		
		// This is the most interesting thing to test since not all Java
		// versions might support the "unix" FileAttributeView which it uses:
		// It is not mentioned in the Java documentation as of 2019 so it seems
		// to be an unofficial feature for now.
		Timestamps t = Timestamps.readTimestamps(f);
		
		assertNotNull(t);
		assertNotNull(t.getAccessTime());
		assertNull("Not implemented yet, see ITimestamps", t.getBirthTime());
		assertNotNull(t.getModificationTime());
		assertNotNull(t.getStatusChangeTime());
		
		// TODO: Test the time values, e.g. compare to current time and accept
		// a limited delta.
	}

}
