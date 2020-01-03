package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.SHA256.sha256fromString;
import static checkpoint.datamodel.implementation.Node.constructNode;
import static checkpoint.datamodel.implementation.Timestamps.timestampsFromDates;
import static java.nio.charset.StandardCharsets.UTF_8;
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

	@Test public void testPathComparator()
			throws IOException, InterruptedException {
		
		// Umlauts are a good test because of the following two aspects:
		// 1) They consist of multiple bytes.
		byte[] umlaut    = "Ä".getBytes(UTF_8);
		byte[] nonUmlaut = "B".getBytes(UTF_8);
		assertEquals(2, umlaut.length);
		// 2) LC_ALL=C sorting, which PathComparator ought to do, is supposed to
		// sort by the *unsigned* byte values - but the Java byte type is
		// signed. And comparison of some umlaut values can thus go wrong
		// because their signed value is negative:
		assertTrue(umlaut[0] < 0);
		// & 0xFF converts the signed byte to an unsigned integer.
		// TODO: Java 8: Use Byte.toUnsignedInt() instead.
		assertNotEquals(
			Integer.compare((umlaut[0] & 0xFF), (nonUmlaut[0] & 0xFF)),
			Byte.compare(umlaut[0], nonUmlaut[0]));
		
		// This test data is sorted in the reverse order it should be sorted to
		// ensure everything has to be moved by the sorting code.
		// TODO: Code quality: Also test with the example here:
		// https://stackoverflow.com/questions/31938751/bash-how-does-sort-sort-paths
		Path[] toSort = new Path[] {
			Paths.get("/Ä"),
			Paths.get("/C"),
			Paths.get("/B"),
			Paths.get("/A")};
		
		// Debug code to print the numeric values of the characters in the above
		// test data.
		/*
		for(Path p : toSort) {
			byte[] bytes = p.toString().getBytes(UTF_8);
			
			System.out.print(p.toString() + " unsigned: ");
			for(byte b : bytes) {
				int unsigned = b & 0xFF;
				System.out.print(unsigned);
				System.out.print(' ');
			}
			System.out.println();
			
			System.out.print(p.toString() + "   signed: ");
			for(byte b : bytes) {
				System.out.print(b);
				System.out.print(' ');
			}
			System.out.println();
		}
		*/
		fail("FIXME: Implement to test w.r.t. the above lines");
	}

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
		Node n = constructNode(Paths.get(poorFilename), false, 123,
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
