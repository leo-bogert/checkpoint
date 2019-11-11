package checkpoint.ui.shell;

import static java.lang.System.err;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import checkpoint.generation.ConcurrentCheckpointGenerator;

final class CreateCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName() + " INPUT_DIR OUTPUT_CHECKPOINT_DIR";

	private class Options {
		@Parameter(names = { "--threads" }, order = 0, description =
			  "Number of threads to process files/directories with. "
			+ "Each thread will use as much memory as given via --buffer. "
			+ "Must be at least 1.")
		private int threads = 1024;

		@Parameter(names = { "--buffer" }, order = 1, description =
			  "I/O buffer per thread, in bytes. Must at least 4096. "
			+ "Making it divisible by 4096 is a good idea.")
		private int buffer = 1024*1024;

		@Parameter(description =
			"INPUT_DIR OUTPUT_CHECKPOINT_DIR")
		private List<String> args = new ArrayList<>(2);
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
