package checkpoint.generation;

import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

import checkpoint.datamodel.implementation.Checkpoint;

public final class ConcurrentCheckpointGenerator
		implements ICheckpointGenerator {

	// FIXME: Performance: Make thread count configurable
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

	/** Function for splitting a set of work, in our case of files/directories
	 *  to include in the checkpoint, into a set of batches for submission to a
	 *  different thread each.
	 *  
	 *  Removes all work from the given ArrayList and returns an ArrayList which
	 *  contains the given batch-amount of sub-ArrayLists where each contains an
	 *  approximately equal amount of work.
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
		throw new UnsupportedOperationException("FIXME: Implement");
	}
}
