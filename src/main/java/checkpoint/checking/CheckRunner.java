package checkpoint.checking;

import checkpoint.datamodel.ICheckpoint;
import checkpoint.datamodel.INode;
import checkpoint.generation.ConcurrentCheckpointGenerator;
import checkpoint.ui.shell.CheckCommand;

/** Used by {@link CheckCommand} to run all {@link ICheck} implementations
 *  upon a pair of checkpoints.
 *  
 *  TODO: Performance: Multi-threading: As the checks operate on read-only
 *  data (= the input checkpoints) we could easily run them in parallel.
 *  Work-units could be coarsely or finely granular: We could run each check
 *  class in a thread, or we could even change all check classes to considering
 *  each {@link INode} being checked as a work-unit to run in parallel.  
 *  Add a class ConcurrentCheckRunner to do so, use
 *  {@link ConcurrentCheckpointGenerator} as inspiration. */
public final class CheckRunner {

	private final ICheckpoint oldCheckpoint;
	private final ICheckpoint newCheckpoint;

	public CheckRunner(ICheckpoint oldCheckpoint, ICheckpoint newCheckpoint) {
		this.oldCheckpoint = oldCheckpoint;
		this.newCheckpoint = newCheckpoint;
	}

	/** Returns true if all {@link ICheck}s succeeded, false if any of them
	 *  indicated failure. */
	public boolean run() {
		throw new UnsupportedOperationException("FIXME: Not implemented yet!");
	}

}
