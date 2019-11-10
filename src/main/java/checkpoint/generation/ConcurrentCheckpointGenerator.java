package checkpoint.generation;

import static java.lang.Math.min;
import static java.lang.System.err;
import static java.lang.System.out;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import checkpoint.datamodel.INode;
import checkpoint.datamodel.implementation.Checkpoint;
import checkpoint.datamodel.implementation.JavaSHA256;
import checkpoint.datamodel.implementation.NodeFinder;
import checkpoint.datamodel.implementation.Timestamps;

public final class ConcurrentCheckpointGenerator
		implements ICheckpointGenerator {

	// FIXME: Performance: Make thread count configurable
	// Once you do that make sure to adapt the README's statements about how
	// much free RAM is needed. The 1 GiB IO cache it mentions was calculated
	// from the fact that each thread will generate a JavaSHA256 instance, which
	// allocates 1 MiB of RAM as buffer for reading the input file.
	private static final int THREAD_COUNT = 1024;

	private final Path       inputDir;
	private final Path       outputDir;
	private final Checkpoint checkpoint;

	public ConcurrentCheckpointGenerator(Path inputDir, Path outputDir) {
		// Convert paths to clean absolute dirs since I suspect their usage
		// might be faster with the lots of processing we'll do with those paths
		// TODO: Performance: Validate that.
		this.inputDir
			= requireNonNull(inputDir).toAbsolutePath().normalize();
		this.outputDir
			= requireNonNull(outputDir).toAbsolutePath().normalize();
		
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
			
			for(INode node : work) {
				// INode.getPath() is relative to the inputDir so we must
				// prefix it with the inputDir.
				Path pathOnDisk = inputDir.resolve(node.getPath());
				
				if(!node.isDirectory()) {
					try {
						node.setHash(JavaSHA256.sha256ofFile(pathOnDisk));
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
	private static <WorkType> ArrayList<ArrayList<WorkType>>
			removeAndDivideWork(ArrayList<WorkType> removeFrom, int batches) {
		
		Collections.shuffle(removeFrom);
		
		// Add 1 to ensure the remainder of the division will also get
		// distributed across the batches.
		// This works because the 1 multiplied by each batch is == the count of
		// batches. And the division remainder of dividing by the batch count
		// here cannot be larger than that.
		int workPerBatch = (removeFrom.size() / batches) + 1;
		ArrayList<ArrayList<WorkType>> result = new ArrayList<>(batches);
		
		for(int batch = 0; batch < batches; ++batch) {
			int availableWork = removeFrom.size();
			int batchSize = min(workPerBatch, availableWork);
			if(batchSize == 0)
				break;
			
			ArrayList<WorkType> batchWork = new ArrayList<>(batchSize);
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
		
		out.println("Dividing into up to " + THREAD_COUNT
			+ " batches of work...");
		ArrayList<ArrayList<INode>> work
			= removeAndDivideWork(nodes, THREAD_COUNT);
		nodes = null;
		
		final int threadCount = work.size();
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
