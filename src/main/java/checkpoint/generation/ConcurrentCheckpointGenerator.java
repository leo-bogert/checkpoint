package checkpoint.generation;

import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.nio.file.Path;

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

	@Override public void run() throws InterruptedException, IOException {
		throw new UnsupportedOperationException("FIXME: Implement");
	}
}
