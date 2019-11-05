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

	@Override public int run(List<String> args) {
		if(args.size() != 1) {
			err.println("Syntax:");
			err.println(getShortSyntax());
			return 1;
		}
		
		try {
			Path p = Paths.get(args.get(0));
			Checkpoint cp = Checkpoint.load(p);
			out.println("Checkpoint loaded successfully.");
			out.println("Is checkpoint complete: " + cp.isComplete());
			// FIXME: Add more features:
			// - print number of Nodes.
			// - print number of failed SHA256/Timestamps computations.
			return 0;
		} catch(InvalidPathException | IOException e) {
			err.println("Checkpoint cannot be loaded:");
			e.printStackTrace(err);
			return 1;
		}
	}

	@Override String getShortSyntax() {
		return '\t' + getCommandName() + " CHECKPOINT";
	}

}
