package checkpoint.ui.shell;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import checkpoint.datamodel.implementation.Checkpoint;

final class InspectCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName() + " CHECKPOINT";
	}

	@Override int run(List<String> args) {
		if(args.size() != 1) {
			err.println("Syntax:");
			err.println(getShortSyntax());
			err.println();
			err.println("- Tests if the file format of a checkpoint is valid.");
			err.println("- Prints info about its contents.");
			return 1;
		}
		
		try {
			Path p = Paths.get(args.get(0));
			Checkpoint cp = Checkpoint.load(p);
			out.println("Checkpoint loaded successfully.");
			out.println("Date estimate (latest contained timestamp): "
				+ cp.getDateEstimate());
			out.println("Number of nodes: " + cp.getNodeCount());
			out.println("Hashing failed for: " + cp.getHashingFailureCount());
			out.println("Timestamping failed for: "
					+ cp.getTimestampingFailureCount());
			out.println("Is checkpoint complete: " + cp.isComplete());
			return 0;
		} catch(InvalidPathException e) {
			err.println("Invalid path: " + e.getMessage());
			return 1;
		} catch(IOException e) {
			err.println("Checkpoint cannot be loaded:");
			e.printStackTrace(err);
			return 1;
		}
	}

}
