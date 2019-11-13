package checkpoint.generation;

import static java.lang.Math.min;
import static java.lang.System.err;
import static java.lang.System.out;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import checkpoint.datamodel.INode;
import checkpoint.datamodel.implementation.Checkpoint;
import checkpoint.datamodel.implementation.NodeFinder;
import checkpoint.datamodel.implementation.Timestamps;

public final class ConcurrentCheckpointGenerator
		implements ICheckpointGenerator {

    /** A HDD typically only has 1 head so to avoid seeking we only run 1
     *  thread.
     *  Notice that this applies even to disks with multiple platters:
     *  The ones I've disassembled with multiple heads, one for each platter,
     *  could typically only move them all together, not separately.
     *  I.e. they would be oriented in a parallel stack and moving them would
     *  move the whole stack at once. */
	public static final int DEFAULT_THREAD_COUNT_HDD = 1;
	/**  FIXME: Performance: Determine a reasonable value. */
	public static final int DEFAULT_THREAD_COUNT_SSD = 1024;

	private final Path       inputDir;
	private final Path       outputDir;
	private final Checkpoint checkpoint;

	/** Is the disk we read from a SSD?
	 *  If false it is assumed to be a rotational disk.
	 *  
	 *  The way we process files needs to be different for rotational disks due
	 *  to their limitation of random access being very slow. */
	private final boolean solidStateDrive;

	/** The value may be decreased by {@link #run()} if there is less work
	 *  available than the desired amount of threads. */
	private int threadCount;

	/** Each thread will generate a JavaSHA256Generator instance, which by
	 *  default allocates 1 MiB of RAM as buffer for reading the input file.
	 *  This can be overriden by "--buffer" on the command line, which is
	 *  passed into this variable as bytes. */
	private final int readBufferBytes;


	public ConcurrentCheckpointGenerator(Path inputDir, Path outputDir,
			boolean solidStateDrive, Integer threads, int readBufferBytes) {
		
		// Convert paths to clean absolute dirs since I suspect their usage
		// might be faster with the lots of processing we'll do with those paths
		// TODO: Performance: Validate that.
		this.inputDir
			= requireNonNull(inputDir).toAbsolutePath().normalize();
		this.outputDir
			= requireNonNull(outputDir).toAbsolutePath().normalize();
		this.solidStateDrive = solidStateDrive;
		if(threads != null)
			this.threadCount = threads;
		else {
			this.threadCount = solidStateDrive ?
				DEFAULT_THREAD_COUNT_SSD : DEFAULT_THREAD_COUNT_HDD;
		}
		this.readBufferBytes = readBufferBytes;
		
		// FIXME: Allow resuming an incomplete one.
		this.checkpoint = new Checkpoint();
	}

	/** Our worker threads run these Runnables. */
	private final class Worker implements Runnable {

		private final ArrayList<INode> work;

		Worker(ArrayList<INode> work) {
			this.work = work;
		}

		@Override public void run() {
			Thread.currentThread().setName(
				"ConcurrentCheckpointGenerator.Worker");
			
			// Re-use across whole lifetime of thread to prevent memory
			// allocation churn since the buffer size we pass it was given by
			// the user and may be very large to match the typical size of files
			// to expect.
			// We don't put this into a member variable intentionally:
			// The loop which creates the Worker objects is single-threaded so
			// allocating lots of memory may take longer there than having each
			// Worker do it concurrently on their thread in run().
			JavaSHA256Generator hasher
				= new JavaSHA256Generator(readBufferBytes);
			
			for(INode node : work) {
				// INode.getPath() is relative to the inputDir so we must
				// prefix it with the inputDir.
				Path pathOnDisk = inputDir.resolve(node.getPath());
				
				if(!node.isDirectory()) {
					try {
						node.setHash(hasher.sha256ofFile(pathOnDisk));
					} catch(IOException e) {
						// Set hash to null to mark computation as failed.
						// This must be done explicitly instead of just leaving
						// it at the default because we might be resuming an
						// existing checkpoint where it wasn't null.
						node.setHash(null);
						
						err.println("SHA256 computation failed for '"
							+ node.getPath() + "': " + e);
					} catch(InterruptedException e) {
						// Shutdown requested, exit thread.
						// Return without adding the INode to the Checkpoint
						// because we would have to keep the hash at null, which
						// would cause "(sha256sum failed!)" to be written to
						// the output file, which would be wrong - it didn't
						// fail, we just didn't try.
						return;
					}
				}
				
				// Read timestamps after hash computation because computing the
				// hash can take a long time so there is plenty of time for the
				// timestamps to be modified.
				// TODO: Read them twice - before hash computation and after -
				// and recompute if they have changed in between.
				// Or perhaps just store the current time before hash
				// computation and compare it against the timestamps?
				try {
					node.setTimestamps(Timestamps.readTimestamps(pathOnDisk));
				} catch(IOException e) {
					// Same as for the hash.
					node.setTimestamps(null);
					
					err.println("Reading timestamps failed for '"
						+ node.getPath() + "': " + e);
				}
				
				// This is thread-safe by contract of ICheckpoint.
				checkpoint.addNode(node);
			}
		}
	}

	/** Function for splitting a set of work, in our case of files/directories
	 *  to include in the checkpoint, into a set of batches for submission to a
	 *  different {@link Worker} thread each.
	 *  
	 *  Removes all work from the given ArrayList and returns an ArrayList which
	 *  contains the given batch-amount (or less for small work-sets) of
	 *  sub-ArrayLists where each contains an approximately equal amount of
	 *  work.
	 *  
	 *  FIXME: The below is outdated, whether we do this depends on the
	 *  solidStateDrive parameter.
	 *  
	 *  The work is randomly distributed across the batches and randomly ordered
	 *  inside each batch.
	 *  (This is also why the input must be an ArrayList: It implements
	 *  RandomAccess so we can call {@link Collections#shuffle(java.util.List)}
	 *  upon it without causing that function to copy the data to an array
	 *  internally.)
	 *  
	 *  The random distribution is ideal for processing our input files and
	 *  directories as work with many threads in parallel:
	 *  Since we don't know where files are located on disk it is best to
	 *  concurrently access a large, random set of files so the IO-performance
	 *  can fully benefit from the kernel using the "elevator algorithm" upon
	 *  the hard disk heads:
	 *  The heads should be constantly moved back and forth and data should be
	 *  requested by us from the kernel across most of the disc surface area so
	 *  in each sweep the heads can get as much data as possible.
	 *  Our random distribution of work = files/directories guarantees that this
	 *  applies to each sweep. */
	private static ArrayList<ArrayList<INode>>
			removeAndDivideWork(ArrayList<INode> removeFrom, int batches,
			boolean solidStateDrive) {
		
		if(solidStateDrive) {
			// If I remember correctly SSDs are organized into cells where each
			// cell is like a "thread" which can read independently of the
			// others. So to utilize all of them we need to query files from
			// random locations. Hence randomize the work.
			// TODO: Validate the above.
			Collections.shuffle(removeFrom);
		} else {
			// A hard disk usually has a single head which is slow to move
			// around. So ideally a single worker thread would read files which
			// are close to each other on disk.
			// Checkpoint.PathComparator sorts in a way such that files in the
			// same directory are next to each other in the sorted output.
			// This is what we want for ext4 as it puts files in the same dir
			// close to each other on disk.
			final Comparator<Path>  pathCmp = new Checkpoint.PathComparator();
			      Comparator<INode> nodeCmp = new Comparator<INode>() {
				@Override public int compare(INode n1, INode n2) {
					return pathCmp.compare(n1.getPath(), n2.getPath());
				}
			};
			Collections.sort(removeFrom, nodeCmp);
		}
		
		// Add 1 to ensure the remainder of the division will also get
		// distributed across the batches.
		// This works because the 1 multiplied by each batch is == the count of
		// batches. And the division remainder of dividing by the batch count
		// here cannot be larger than that.
		int workPerBatch = (removeFrom.size() / batches) + 1;
		ArrayList<ArrayList<INode>> result = new ArrayList<>(batches);
		
		for(int batch = 0; batch < batches; ++batch) {
			int availableWork = removeFrom.size();
			int batchSize = min(workPerBatch, availableWork);
			if(batchSize == 0)
				break;
			
			ArrayList<INode> batchWork = new ArrayList<>(batchSize);
			for(int workPiece = 0; workPiece < batchSize; ++workPiece) {
				// Must remove() the last, otherwise all would be copied around.
				batchWork.add(removeFrom.remove(removeFrom.size() - 1));
			}
			
			result.add(batchWork);
		}
		
		// Self-test for if workPerBatch was calculated correctly.
		// TODO: Performance: Convert to assert().
		if(removeFrom.size() != 0) {
			throw new RuntimeException("BUG: Work remaining: "
				+ removeFrom.size() + ". Please report this!");
		}
		
		return result;
	}

	@Override public void run() throws InterruptedException, IOException {
		// FIXME: Handle Thread.interrupt() gracefully, i.e. save the current
		// progress.
		// FIXME: Save every 15 minutes.
		
		out.print("Finding input files and directories in '"
			+ inputDir + "'... ");
		// Convert to ArrayList since removeAndDivideWork() does shuffle() which
		// needs a list which implements RandomAccess.
		ArrayList<INode> nodes
			= new ArrayList<INode>(new NodeFinder().findNodes(inputDir));
		final int nodeCount = nodes.size();
		out.println(nodeCount);
		
		out.println("Dividing into up to " + threadCount
			+ " batches of work...");
		ArrayList<ArrayList<INode>> work
			= removeAndDivideWork(nodes, threadCount, solidStateDrive);
		nodes = null;
		
		threadCount = work.size();
		out.println("Divided into " + threadCount +
			" batches, creating as many threads...");
		// TODO: Performance: Try newWorkStealingPool().
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		
		out.println("Submitting work to threads...");
		ArrayList<Future<?>> workResults = new ArrayList<>(threadCount);
		for(ArrayList<INode> batch : work)
			workResults.add(executor.submit(new Worker(batch)));
		
		out.println("Working...");
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		
		out.println("Work finished, checking results...");
		for(Future<?> result : workResults) {
			try {
				// Returning null means success w.r.t. the submit() version we
				// used.
				if(result.get() != null) {
					throw new RuntimeException("BUG: Worker thread failed! "
						+ "Future<?>.get() value: " + result.get());
				}
			} catch(ExecutionException e) {
				throw new RuntimeException(
					"BUG: Worker thread threw! Please report this!",
					e.getCause());
			} catch(CancellationException e) {
				throw new RuntimeException(
					"BUG: Worker thread cancelled! Please report this!", e);
			}
		}
		
		if(checkpoint.getNodeCount() != nodeCount) {
			throw new RuntimeException(
				"BUG: Workers submitted an unexpected result count! " +
				checkpoint.getNodeCount() + " != " + nodeCount + ". " +
				"Please report this!");
		} else
			checkpoint.setCompleteFlag(true);
		
		out.println("Saving checkpoint to '" + outputDir + "'...");
		checkpoint.save(outputDir);
		out.println("Done.");
	}

}
