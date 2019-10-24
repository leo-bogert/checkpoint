package checkpoint.datamodel.implementation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

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

	/** Used by {@link #save(Path, boolean)} and {@link #load(Path)}.
	 *  WARNING: SimpleDateFormat is NOT thread-safe! Synchronize upon this
	 *  Checkpoint when using this! */
	private final SimpleDateFormat dateFormat
		= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

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
				
				FileTimeToDateAdapter t
					= new FileTimeToDateAdapter(n.getTimetamps());
				
				w.write("\tBirth: ");
				Date btime = t.getBirthTime();
				w.write(btime == null ? "-" : dateFormat.format(btime));
				
				w.write("\tAccess: ");
				w.write(dateFormat.format(t.getAccessTime()));
				
				w.write("\tModify: ");
				w.write(dateFormat.format(t.getModificationTime()));
				
				w.write("\tChange: ");
				w.write(dateFormat.format(t.getStatusChangeTime()));
				
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
		
		throw new UnsupportedOperationException("FIXME: Implement!");
	}

}