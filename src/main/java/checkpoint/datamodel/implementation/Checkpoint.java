package checkpoint.datamodel.implementation;

import static checkpoint.datamodel.implementation.Node.constructNode;
import static checkpoint.datamodel.implementation.SHA256.sha256fromString;
import static checkpoint.datamodel.implementation.Timestamps.timestampsFromDates;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
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
import checkpoint.generation.ConcurrentCheckpointGenerator;

public final class Checkpoint implements ICheckpoint {

	/** Storage of the {@link INode}s which have been added via
	 *  {@link #addNode(INode)}.
	 *  Beyond storage this also has the purpose of implicitly sorting them by
	 *  their {@link INode#getPath()} since our human readable file format
	 *  of {@link #save(Path)} will sort by path.
	 *  
	 *  WARNING: You must synchronize upon this Checkpoint when using this!
	 * 
	 *  We explicitly set our own {@link Comparator} because the ordering of
	 *  {@link Path#compareTo(Path)} is platform specific.
	 *  The comparator we use will produce the same order on any system.
	 *  
	 *  FIXME: Performance: Replace with data structure which supports fast
	 *  concurrent adding so our many generator threads can deal with the
	 *  sorting in parallel. Perhaps {@link ConcurrentSkipListMap}?
	 *  Do first investigate if size() is constant-time:
	 *  If it is not then we cannot use that map class here because the
	 *  {@link ConcurrentCheckpointGenerator} will call our
	 *  {@link #getNodeCount()} every second to print progress to stdout.
	 *  A workaround may be to use a different data structure to keep track of
	 *  the count, which we will need anyway due to {@link #getNodeSize()}
	 *  already being tracked separately. */
	private final TreeMap<Path, INode> nodes
		= new TreeMap<>(new PathComparator());

	/** Our Python/Bash reference implementations use the shell command
	 *  "LC_ALL=C sort --zero-terminated" for sorting paths.
	 *  
	 *  This comparator emulates the "LC_ALL=C" part, i.e. sorting paths in a
	 *  way which:
	 *  - ensures files in the same directory are sorted next to each other,
	 *    which was the main goal of LC_ALL=C:
	 *    The "sort" command without LC_ALL=C would ignore the "/" in paths for
	 *    its comparisons under certain conditions which would cause files of
	 *    the same directory not be listed next to each other in the sorted
	 *    output.
	 *  - as a bonus is constant independent of system language configuration.
	 *  
	 *  "--zero-terminated" needs not be emulated since our Java code tracks the
	 *  paths as separate objects each.
	 *  
	 * TODO: Convert to standalone class now that it is also used by 
	 * ConcurrentCheckpointGenerator */
	public static final class PathComparator implements Comparator<Path> {
		@Override public int compare(Path p1, Path p2) {
			// The manpage of sort as of GNU coreutils 8.28 states:
			//     Set LC_ALL=C to get the traditional sort order that uses
			//     native byte values.
			// So converting the path strings to byte[] and sorting on that is
			// likely the right thing to do...
			byte[] a = p1.toString().getBytes(UTF_8);
			byte[] b = p2.toString().getBytes(UTF_8);
			// ...BUT: byte is signed but LC_ALL=C does unsigned comparison so
			// we must do that too.
			return compareUnsigned(a, b);
		}

		/** TODO: Java 9: Replace with Arrays.compareUnsigned() */
		private static int compareUnsigned(byte[] a, byte[] b) {
			if(a == b)
				return 0;
			
			int mismatch = -1;
			int len = min(a.length, b.length);
			for(int i = 0; i < len; ++i) {
				if(a[i] != b[i]) {
					mismatch = i;
					break;
				}
			}
			
			if(mismatch != -1) {
				// Same as Byte.toUnsignedInt() but compatible with Java 7.
				int unsigned1 = a[mismatch] & 0xFF;
				int unsigned2 = b[mismatch] & 0xFF;
				return Integer.compare(unsigned1, unsigned2);
			}
			
			return a.length - b.length;
		}
	}

	/** @see ICheckpoint#isComplete() */
	private boolean complete = false;

	/** @see ICheckpoint#getNodeSize() */
	private long nodeSize = 0;

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
		
		nodeSize += n.getSize();
	}

	@Override public synchronized void save(Path checkpointDir)
			throws IOException {
		
		// FIXME: The creation of the dir and setting of its permissions likely
		// is not safe against race conditions caused by malicious processes
		// which have e.g. group or others write permissions to the dir.
		// Either fix that or document it.
		
		// Don't pass the permissions to it but manually set them later to
		// ensure they also get set when rewriting an existing checkpoint.
		Files.createDirectories(checkpointDir);
		
		// createDirectories() does not guarantee to throw if it exists as
		// a non-dir already so do that first to ensure we don't change
		// permissions of it if it is a file.
		if(!Files.isDirectory(checkpointDir, NOFOLLOW_LINKS)) {
			throw new FileAlreadyExistsException(
				"Is not a directory, should be a non-symlink dir or not exist: "
				+ checkpointDir.toString());
		}
		
		Files.setPosixFilePermissions(checkpointDir,
			PosixFilePermissions.fromString("rwx------"));
		
		// TODO: Use Files.createTempFile() and move it into place once we're
		// finished. This may ensure that intermediate saving will never result
		// in a corrupted file if the system crashes: Either the old file will
		// still be there, or the new one, or none.
		Path outputFilePath = checkpointDir.resolve("checkpoint.txt");
		if(Files.exists(outputFilePath, NOFOLLOW_LINKS) &&
				!Files.isRegularFile(outputFilePath, NOFOLLOW_LINKS)) {
			
			throw new FileAlreadyExistsException(
				"Is not a file, should be a non-symlink file or not exist: "
				+ outputFilePath.toString());
		}
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
				
				s.useDelimiter(lineDelimiter);
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
					
					int splitAt = timestampToken.indexOf(':');
					String dateName = timestampToken.substring(0, splitAt);
					String date     = timestampToken.substring(splitAt + 2);
					
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
				
				// Not currently included in the file format. Once you implement
				// that please process the related TODO and documentation at the
				// INode interface and at ICheckpoint.getNodeSize().
				long size = 0;
				
				result.addNode(
					constructNode(path, isDirectory, size, hash, timestamps));
			}
			
			if(s.ioException() != null)
				throw s.ioException();
			
			return result;
		} catch(DecoderException | ParseException | RuntimeException
				| OutOfMemoryError e) {
			
			// Free up some memory before we try to construct the IOException to
			// ensure we don't get OOM again due to the constructing.
			result = null;
			System.gc();
			
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

	@Override public synchronized int getNodeCount() {
		return nodes.size();
	}

	@Override public synchronized long getNodeSize() {
		return nodeSize;
	}

	@Override public synchronized int getHashingFailureCount() {
		int count = 0;
		for(INode n : nodes.values()) {
			if(!n.isDirectory() && n.getHash() == null)
				++count;
		}
		return count;
	}

	@Override public synchronized int getTimestampingFailureCount() {
		int count = 0;
		for(INode n : nodes.values()) {
			if(n.getTimetamps() == null)
				++count;
		}
		return count;
	}

}