package checkpoint.ui.shell;

import static java.lang.System.err;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import checkpoint.generation.ConcurrentCheckpointGenerator;

final class CreateCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName() + " INPUT_DIR OUTPUT_CHECKPOINT_DIR";
	}

	@Override int run(List<String> args) {
		if(args.size() != 2) {
			err.println("Syntax:");
			err.println(getShortSyntax());
			err.println();
			err.println(
				"Searches all files and directories in the INPUT_DIR and " +
				"writes a checkpoint for them to the OUTPUT_CHECKPOINT_DIR.");
			return 1;
		}
		
		Path input;
		Path output;
		try {
			input = Paths.get(args.get(0));
			output = Paths.get(args.get(1));
		} catch(InvalidPathException e) {
			err.println("Invalid path: " + e.getMessage());
			return 1;
		}
		
		try {
			new ConcurrentCheckpointGenerator(input, output).run();
			return 0;
		} catch (IOException | InterruptedException e) {
			err.println("Generating checkpoint failed:");
			e.printStackTrace(err);
			return 1;
		}
	}

}
