package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.JavaSHA256.sha256fromString;
import static checkpoint.datamodel.implementation.Node.constructNode;
import static checkpoint.datamodel.implementation.Timestamps.timestampsFromDates;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.codec.DecoderException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class CheckpointTest {

	private static final String someHash
		= "7dd91e07f0341646d53f6938278a4d3e87961fabea066f7e6f40b7398f3b0b0f";

	private static final Timestamps someTimestamps = timestampsFromDates(
		new Date(DAYS.toMillis(1)),
		new Date(DAYS.toMillis(2)),
		new Date(DAYS.toMillis(3)));

	@Rule
	public final TemporaryFolder tempDir = new TemporaryFolder();


	@Test public void testAddNode() {
		fail("Not yet implemented");
	}

	@Test public void testSave() {
		fail("Not yet implemented");
	}

	@Test public void testLoad() throws IOException, DecoderException {
		String poorFilename = "This \n is \r\n bad \t due to the whitespace!\n";
		Node n = constructNode(Paths.get(poorFilename), false,
			sha256fromString(someHash), someTimestamps);
		Path testCheckpoint = tempDir.newFolder().toPath();
		Checkpoint original = new Checkpoint();
		original.addNode(n);
		original.save(testCheckpoint, true);
		
		Checkpoint loaded = Checkpoint.load(testCheckpoint);
		// FIXME: Validate the result of load()
		
		// FIXME: Add more tests
	}

}
