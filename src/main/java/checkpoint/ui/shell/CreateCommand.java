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
		return '\t' + getCommandName()
			+ " [options] INPUT_DIR OUTPUT_CHECKPOINT_DIR";
	}

	/** TODO: As of 2019-11-11 Travis CI uses JCommander 1.48 which does not
	 *  support "order = integer" to specify the order in which the Parameters
	 *  appear in the help, try again in some years.
	 *  See the commit which added this comment for what the orders were. */
	private static final class Options {
		@Parameter(names = { "--threads" }, description =
			  "Number of threads to process files/directories with. "
			+ "Each thread will use as much memory as given via --buffer. "
			+ "Must be at least 1.")
		int threads = 1024;

		@Parameter(names = { "--buffer" }, description =
			  "I/O buffer per thread, in bytes. Must at least 4096. "
			+ "Making it divisible by 4096 (= x86 pagesize) is a good idea.")
		int buffer = 1024*1024;

		@Parameter(description =
			"INPUT_DIR OUTPUT_CHECKPOINT_DIR")
		List<String> args = new ArrayList<>(2);

		void validate() throws IllegalArgumentException {
			if(threads < 1)
				throw new IllegalArgumentException("--threads is too low!");
			
			if(buffer < 4096)
				throw new IllegalArgumentException("--buffer is too low!");
			
			// TODO: As of 2019-11-11 with JCommander 1.71 @Parameter(arity = 2)
			// doesn't work for unnamed parameters it seems so we check it
			// manually, try again in some years.
			if(args.size() != 2)
				throw new IllegalArgumentException("Missing input/ouput dir!");
		}
	}

	@Override int run(List<String> args) {
		Options o = new Options();
		JCommander jc = JCommander.newBuilder().addObject(o).build();
		jc.setProgramName(getCommandName());
		try {
			jc.parse(args.toArray(new String[args.size()]));
			o.validate();
		} catch(ParameterException | IllegalArgumentException e) {
			if(e instanceof IllegalArgumentException) {
				err.println(e.getMessage());
				err.println();
			}
			
			printUsage(jc);
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

	private static void printUsage(JCommander jc) {
		// TODO: As of 2019-11-11 with JCommander 1.71 JCommander.usage()
		// will print to stdout, not stderr, which is bad. So we fix that by
		// using the version which consumes a StrinBuilder and printing on our
		// own. Try again in some years, file a bug if it still is wrong then.
		StringBuilder sb = new StringBuilder();
		jc.usage(sb);
		err.println(sb);
		
		err.println(
			"Searches all files and directories in the INPUT_DIR and " +
			"writes a checkpoint for them to the OUTPUT_CHECKPOINT_DIR.");
	}

}
