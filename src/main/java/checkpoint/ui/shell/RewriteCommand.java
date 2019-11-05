package checkpoint.ui.shell;

import static java.lang.System.err;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import checkpoint.datamodel.implementation.Checkpoint;

final class RewriteCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName()
			+ " CHECKPOINT [DIFFERENT_OUTPUT_PATH]";
	}

	@Override int run(List<String> args) {
		if(args.size() < 1 || args.size() > 2) {
			err.println("Syntax:");
			err.println(getShortSyntax());
			err.println();
			err.println(
				"Loads a checkpoint from disk and writes it to disk again, "
				+ "potentially modifying its data if the user chooses to do so."
				+ "\nFIXME: The latter is not implemented yet!"
			);
			return 1;
		}
		
		Path input;
		Path output;
		
		try {
			input = Paths.get(args.get(0));
			output = args.size() == 2 ? Paths.get(args.get(1)) : input;
		} catch(InvalidPathException e) {
			err.println("Invalid path:");
			e.printStackTrace(err);
			return 1;
		}
		
		Checkpoint cp;
		try {
			 cp = Checkpoint.load(input);
		} catch(IOException e) {
			err.println("Checkpoint file cannot be loaded:");
			e.printStackTrace(err);
			return 1;
		}
		
		try {
			cp.save(output);
			return 0;
		} catch(IOException e) {
			err.println("Checkpoint file cannot be saved:");
			e.printStackTrace(err);
			return 1;
		}
	}

}
