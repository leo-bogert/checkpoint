package checkpoint.generation;

import java.io.IOException;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;

/** TODO: Specify constructors. */
public interface ICheckpointGenerator {

	/** Searches for input files and directories on disk, computes their
	 *  {@link INode} data, and writes the {@link ICheckpoint} consisting of all
	 *  nodes to disk.
	 *  
	 *  @throws InterruptedException If shutdown has been requested by
	 *      {@link Thread#interrupt()}. The existing progress should have been
	 *      saved to the output checkpoint dir on disk. */
	void run() throws InterruptedException, IOException;

}