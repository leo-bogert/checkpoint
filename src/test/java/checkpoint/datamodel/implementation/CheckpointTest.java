package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.SHA256.sha256fromString;
import static checkpoint.datamodel.implementation.Node.constructNode;
import static checkpoint.datamodel.implementation.Timestamps.timestampsFromDates;
import static java.nio.file.Files.readAllBytes;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.codec.DecoderException;
import org.junit.Ignore;
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

	@Ignore("FIXME: Not implemented yet!")
	@Test public void testAddNode() {
		// DON'T FORGET TO REMOVE @Ignore WHEN IMPLEMENTING!
	}

	@Ignore("FIXME: Not implemented yet!")
	@Test public void testSave() {
		// Draft follows.
		// DON'T FORGET TO REMOVE @Ignore WHEN IMPLEMENTING!
		
		/*
		String filename1 = "/boot!",
		       filename2 = "/boot/vmlinuz";
		String hash1 =
			"(directory)",
		       hash2 =
			"1111111111111111111111111111111111111111111111111111111111111111";
		
		String checkpointContents =
			filename1
				+ '\0'
				+ '\t' + hash1
				+ '\t' + "Access: 2019-01-23 12:34:56"
				+ '\t' + "Modify: 2019-02-23 12:34:56"
				+ '\t' + "Change: 2019-03-23 12:34:56"
				+ '\n'
			+ filename2
				+ '\0'
				+ '\t' + hash2
				+ '\t' + "Access: 2019-04-23 12:34:56"
				+ '\t' + "Modify: 2019-05-23 12:34:56"
				+ '\t' + "Change: 2019-06-23 12:34:56"
				+ '\n';
			+ "This checkpoint is INCOMPLETE but can be resumed.\n\0"
		*/
	}

	@Test public void testLoad() throws IOException, DecoderException {
		String poorFilename = "This \n is \r\n bad \t due to the whitespace!\n";
		Node n = constructNode(Paths.get(poorFilename), false,
			sha256fromString(someHash), someTimestamps);
		Path testCheckpoint = tempDir.newFolder().toPath();
		Checkpoint original = new Checkpoint();
		original.addNode(n);
		original.save(testCheckpoint);
		
		Checkpoint loaded = Checkpoint.load(testCheckpoint);
		Path savedAgain = tempDir.newFolder().toPath();
		loaded.save(savedAgain);
		
		assertArrayEquals(
			readAllBytes(testCheckpoint.resolve("checkpoint.txt")),
			readAllBytes(savedAgain.resolve("checkpoint.txt")));
		
		// FIXME: Add more tests
	}

}
