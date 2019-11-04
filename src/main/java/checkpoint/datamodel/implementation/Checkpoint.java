package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.JavaSHA256.sha256fromString;
import static checkpoint.datamodel.implementation.Node.constructNode;
import static checkpoint.datamodel.implementation.Timestamps.timestampsFromDates;
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
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;
import checkpoint.datamodel.ISHA256;
import checkpoint.datamodel.ITimestamps;

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

	/** @see ICheckpoint#isComplete() */
	private boolean complete = false;

	/** Used by {@link #dateFormat} and {@link #load(Path)}. */
	private static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss Z";
	
	/** Used by {@link #save(Path)}.
	 * 
	 *  WARNING: SimpleDateFormat is NOT thread-safe! Synchronize upon this
	 *  Checkpoint when using this!
	 *  
	 *  Not used by {@link #load(Path)} since that function is static and we
	 *  shouldn't require globally synchronizing all instances of Checkpoint
	 *  to access this concurrently. */
	private final SimpleDateFormat dateFormat
		= new SimpleDateFormat(DATE_FORMAT_STRING);

	private static final String SHA256SUM_OF_DIRECTORY = "(directory)";
	private static final String SHA256SUM_FAILED = "(sha256sum failed!)";
	private static final String STAT_FAILED = "(stat failed!)";

	/** These, plus an additional \0, mark the end of a Checkpoint file.
	 *  The additional \0 is so they can be parsed as if they were a path of
	 *  a regular {@link Node} in the checkpoint which keeps the code of
	 *  {@link Checkpoint#save(Path)} simple. */
	private static final class EOFPaths {
		static final String CheckpointComplete
			= "This checkpoint is complete.\n";
		static final String CheckpointIncomplete
			= "This checkpoint is INCOMPLETE but can be resumed.\n";
	}

	@Override public synchronized void addNode(INode n)
			throws IllegalArgumentException {
		
		// To catch concurrency issues, specifically computation threads still
		// running after the code which is supposed to call save() thought
		// they've already finished.
		if(complete) {
			throw new IllegalStateException(
				"Checkpoint was marked as complete already!");
		}
		
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

	@Override public synchronized void save(Path checkpointDir) throws IOException {
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
				if(n.isDirectory())
					w.write(SHA256SUM_OF_DIRECTORY);
				else {
					ISHA256 hash = n.getHash();
					w.write(hash != null ? hash.toString() : SHA256SUM_FAILED);
				}
				
				ITimestamps t = n.getTimetamps();
				
				if(t != null) {
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
				} else {
					w.write("\t");
					w.write(STAT_FAILED);
				}
				
				w.write('\n');
			}
			
			w.write(complete ? EOFPaths.CheckpointComplete
			                 : EOFPaths.CheckpointIncomplete);
			w.write('\0');
		} finally {
			w.close();
		}
	}

	public static Checkpoint load(Path checkpointDir)
			throws IOException {
		
		// Albeit save() separates all fields by \t we cannot use that for
		// splitting the whole line into tokens since Linux filenames may
		// contain \t and even \n.
		// So we first determine the file path by looking for \0, then the end
		// of the line by \n, and split the parts in between by \t.
		// TODO: Performance: Use something else than class Scanner for this
		// because regular expressions are overkill here, we merely need
		// splitting upon custom fixed strings or even characters.
		// E.g. some implementation of Reader which supports a readLine() which
		// allows specifying a fixed string as a custom end of line marker.
		Pattern pathDelimiter = Pattern.compile("\0");
		Pattern lineDelimiter = Pattern.compile("\n");
		
		Checkpoint result = new Checkpoint();
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
		Path inputFilePath = checkpointDir.resolve("checkpoint.txt");
		// FIXME: Performance: Use a custom buffer size, default is 8192 which
		// is a bit small.
		BufferedReader r = Files.newBufferedReader(inputFilePath, UTF_8);
		Scanner s = null;
		try {
			s = new Scanner(r);
			while(true) {
				s.useDelimiter(pathDelimiter);
				String pathStr = s.next();
				s.skip(pathDelimiter);
				if(!s.hasNext()) {
					if(pathStr.equals(EOFPaths.CheckpointComplete)) {
						result.complete = true;
						break;
					} else if(pathStr.equals(EOFPaths.CheckpointIncomplete)) {
						result.complete = false;
						break;
					} else
						throw new IOException(
							"Checkpoint is truncated, EOF marker is missing!");
				}
				Path path = Paths.get(pathStr);
				
				// TODO: Use longer variable names
				s.useDelimiter(lineDelimiter);
				String l = s.next();
				s.skip(lineDelimiter);
				
				StringTokenizer t = new StringTokenizer(l, "\t");
				String hashString = t.nextToken();
				boolean isDirectory = hashString.equals(SHA256SUM_OF_DIRECTORY);
				ISHA256 hash =
					(!isDirectory && !hashString.equals(SHA256SUM_FAILED))
					? sha256fromString(hashString)
					: null;
				
				// TODO: Performance: Use ArrayMap from e.g. Apache Java Commons
				HashMap<String, Date> dates = new HashMap<>();
				boolean noTimestampsAvailable = false;
				while(t.hasMoreTokens()) {
					String timestampToken = t.nextToken();
					noTimestampsAvailable = timestampToken.equals(STAT_FAILED);
					if(noTimestampsAvailable) {
						// The file timestamps are read all at once for a single
						// file upon Checkpoint creation, so reading them either
						// succeeded for all or for none of them - see
						// ITimestamps.readTimestamps().
						// So if it failed as indicated by STAT_FAILED then no
						// dates will be available at all for parsing so we must
						// break.
						break;
					}
					
					StringTokenizer key_value
						= new StringTokenizer(timestampToken, ":");
					
					// TODO: Performance: Java 11: Use stripLeading() instead of
					// trim().
					String dateName = key_value.nextToken();
					String date     = key_value.nextToken().trim();
					
					if(date.equals("-"))
						continue;
					
					dates.put(dateName, dateFormat.parse(date));
				}
				
				Timestamps timestamps = !noTimestampsAvailable
					? timestampsFromDates(
							dates.get("Access"),
							dates.get("Change"),
							dates.get("Modify"))
					: null;
				
				result.addNode(
					constructNode(path, isDirectory, hash, timestamps));
			}
			
			if(s.ioException() != null)
				throw s.ioException();
			
			return result;
		} catch(DecoderException | ParseException | RuntimeException e) {
			throw new IOException(e);
		} finally {
			if(s != null)
				s.close();
			r.close();
		}
	}

	@Override public synchronized void setCompleteFlag(boolean complete) {
		this.complete = complete;
	}

	@Override public synchronized boolean isComplete() {
		return complete;
	}

}