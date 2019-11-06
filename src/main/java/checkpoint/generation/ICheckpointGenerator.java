package checkpoint.generation;

import java.io.IOException;

import checkpoint.datamodel.INode;
import checkpoint.datamodel.implementation.Checkpoint;

/** TODO: Specify constructors. */
public interface ICheckpointGenerator {

	/** Searches for input files and directories on disk, computes their
	 *  {@link INode} data, and writes the {@link Checkpoint} consisting of all
	 *  nodes to disk.
	 *  
	 *  @throws InterruptedException If shutdown has been requested by
	 *      {@link Thread#interrupt()}. The existing progress should have been
	 *      saved to the output checkpoint dir on disk. */
	void run() throws InterruptedException, IOException;

}