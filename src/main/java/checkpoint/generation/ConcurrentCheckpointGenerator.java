package checkpoint.generation;

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
		this.inputDir   = inputDir;
		this.outputDir  = outputDir;
		// FIXME: Allow re-using an incomplete one.
		this.checkpoint = new Checkpoint();
	}

	@Override public void run() throws InterruptedException, IOException {
		throw new UnsupportedOperationException("FIXME: Implement");
	}
}
