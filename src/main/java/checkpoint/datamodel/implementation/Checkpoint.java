package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.JavaSHA256.sha256fromString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.codec.DecoderException;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;
import checkpoint.datamodel.ISHA256;
import checkpoint.datamodel.ITimestamps;

// FIXME: save() / load() don't support "(sha256sum failed!)" and
// "(stat failed!)" fields which the Python implementation is capable of
// producing.
public final class Checkpoint implements ICheckpoint {

	/** Storage of the {@link INode}s which have been added via
	 *  {@link #addNode(INode)}.
	 *  Beyond storage this also has the purpose of implicitly sorting them by
	 *  their {@link INode#getPath()} since our human readable file format
	 *  of {@link #save(Path)} will sort by path.
	 *  
	 *  WARNING: You must synchronize upon this Checkpoint when using this!
	 * 
	 *  FIXME: Code quality: Provide an explicit {@link Comparator} because
	 *  {@link Path#compareTo(Path)}'s JavaDoc says the ordering is platform
	 *  specific. 
	 *  FIXME: Performance: Replace with data structure which supports fast
	 *  concurrent adding so our many generator threads can deal with the
	 *  sorting in parallel. Perhaps {@link ConcurrentSkipListMap}? */
	private final TreeMap<Path, INode> nodes = new TreeMap<>();

	/** Used by {@link #dateFormat} and {@link #load(Path)}. */
	private static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss Z";
	
	/** Used by {@link #save(Path, boolean)}.
	 * 
	 *  WARNING: SimpleDateFormat is NOT thread-safe! Synchronize upon this
	 *  Checkpoint when using this!
	 *  
	 *  Not used by {@link #load(Path)} since that function is static and we
	 *  shouldn't require globally synchronizing all instances of Checkpoint
	 *  to access this concurrently. */
	private final SimpleDateFormat dateFormat
		= new SimpleDateFormat(DATE_FORMAT_STRING);

	// TODO: Check git history of Python/Bash implementations and figure out
	// why we add the \0 to them. It's probably to keep the line parser simple
	// so it can always split upon \0 as that is what separates the filename
	// from the rest of each non-EOF line.
	private static final class EOFMarkers {
		static final String CheckpointComplete
			= "This checkpoint is complete.\n\0";
		static final String CheckpointIncomplete
			= "This checkpoint is INCOMPLETE but can be resumed.\n\0";
	}

	@Override public synchronized void addNode(INode n)
			throws IllegalArgumentException {
		
		// Instead of using putIfAbsent() so we can throw if the key is already
		// contained we put() the new INode and replace it with put()ing old one
		// again if put()'s return value tells us that there already was an
		// entry for the key.
		// We do so because that is faster than putIfAbsent():
		// putIfAbsent() will first do a contains() check, and then put().
		// This means that the tree of the map has to be walked twice in the
		// regular case of no duplicate existing, whereas our approach only
		// walks twice if there is a duplicate - which should never happen if
		// the code has no bugs.
		Path key = n.getPath();
		INode oldValue = nodes.put(key, n);
		if(oldValue != null) {
			nodes.put(key, oldValue);
			
			throw new IllegalArgumentException(
				"Bug, please report: INode already contained for path: " + key);
		}
	}

	@Override public synchronized void save(Path checkpointDir,
			boolean isComplete) throws IOException {
		
		// TODO: Use Files.createTempFile() and move it into place once we're
		// finished. This may ensure that intermediate saving will never result
		// in a corrupted file if the system crashes: Either the old file will
		// still be there, or the new one, or none.
		Path outputFilePath = checkpointDir.resolve("checkpoint.txt");
		// FIXME: Performance: Use a custom buffer size, default is 8192 which
		// is a bit small.
		BufferedWriter w = Files.newBufferedWriter(outputFilePath, UTF_8,
				CREATE, TRUNCATE_EXISTING, WRITE);
		try {
			for(INode n : nodes.values()) {
				w.write(n.getPath().toString());
				
				w.write("\0\t");
				w.write(n.getHash().toString());
				
				ITimestamps t = n.getTimetamps();
				
				w.write("\tBirth: ");
				Date btime = t.getBirthTime();
				// btime is currently not supported and will always be null,
				// see ITimestamps.
				w.write(btime == null ? "-" : dateFormat.format(btime));
				
				w.write("\tAccess: ");
				Date atime = t.getAccessTime();
				w.write(atime != null ? dateFormat.format(atime) : "-");
				
				w.write("\tModify: ");
				Date mtime = t.getModificationTime();
				w.write(mtime != null ? dateFormat.format(mtime) : "-");
				
				w.write("\tChange: ");
				Date ctime = t.getStatusChangeTime();
				w.write(ctime != null ? dateFormat.format(ctime) : "-");
				
				w.write('\n');
			}
			
			w.write(isComplete ? EOFMarkers.CheckpointComplete
			                   : EOFMarkers.CheckpointIncomplete);
		} finally {
			w.close();
		}
	}

	public static Checkpoint load(Path checkpointDir)
			throws IOException {
		
		Checkpoint result = new Checkpoint();
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
		Path inputFilePath = checkpointDir.resolve("checkpoint.txt");
		// FIXME: Performance: Use a custom buffer size, default is 8192 which
		// is a bit small.
		BufferedReader r = Files.newBufferedReader(inputFilePath, UTF_8);
		try {
			String l;
			while((l = r.readLine()) != null) {
				// Albeit save() separates all fields by \t we cannot use that
				// for splitting the whole line into tokens since Linux
				// filenames may contain \t.
				// So first split by the additional \0 which terminates the path
				// and then split the remainder of the line by \t.
				// Notice:
				// - nextToken("\t") updates the delimiter permanently.
				// - nextToken("\t") skips empty tokens, which we need for the
				//   "\0\t" preceding the hash.
				StringTokenizer t = new StringTokenizer(l, "\0");
				Path path = Paths.get(t.nextToken());
				ISHA256 hash = sha256fromString(t.nextToken("\t"));
				
				// TODO: Performance: Use ArrayMap from e.g. Apache Java Commons
				HashMap<String, Date> dates = new HashMap<>();
				while(t.hasMoreTokens()) {
					StringTokenizer key_value
						= new StringTokenizer(t.nextToken(), ":");
					
					// TODO: Performance: Java 11: Use stripLeading() instead of
					// trim().
					String dateName = key_value.nextToken();
					String date     = key_value.nextToken().trim();
					
					if(date.equals("-"))
						continue;
					
					dates.put(dateName, dateFormat.parse(date));
				}
				
				Date atime = dates.get("Access");
				Date btime = dates.get("Birth");
				Date ctime = dates.get("Modify");
				Date mtime = dates.get("Change");
				
				// FIXME: Add new Node() once Node is implemented.
				result.addNode(null);
			}
			
			return result;
		} catch(DecoderException | ParseException | RuntimeException e) {
			throw new IOException(e);
		} finally {
			r.close();
		}
	}

}