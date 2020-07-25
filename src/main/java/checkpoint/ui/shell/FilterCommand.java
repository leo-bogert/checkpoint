package checkpoint.ui.shell;

import static java.lang.System.err;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
final class FilterCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName() +
			" [options] INPUT_CHECKPOINT_DIR [OUTPUT_CHECKPOINT_DIR]";
	}

	private static final class Options {
		@Parameter(names = { "--remove-timestamps" }, description =
		    "Can be a combination of the letters 'a', 'b', 'c', 'm' without "
		  + "any separators to remove the atime, btime and so on. "
		  + "Example: '--remove-timestamps bc'.")
		String removeTimestamps = ""; // Default is non-null to simplify code.

		@Parameter(description = "INPUT_CHECKPOINT_DIR [OUTPUT_CHECKPOINT_DIR]")
		List<String> args = new ArrayList<>(2);

		void validate() throws IllegalArgumentException {
			if(!removeTimestamps.matches("[abcm]{1,4}")) {
				throw new IllegalArgumentException(
					"Invalid argument for --remove-timestamps: '"
					+ removeTimestamps + "'");
			}
			
			// TODO: As of 2019-11-11 with JCommander 1.71 @Parameter(arity = 2)
			// doesn't work for unnamed parameters it seems so we check it
			// manually, try again in some years.
			if(args.size() < 1)
				throw new IllegalArgumentException("Missing input dir!");
			else if(args.size() > 2) {
				throw new IllegalArgumentException(
					"Too many/unknown arguments: " + args);
			}
		}
	}

	@Override int run(List<String> args) {
		Options o = new Options();
		JCommander jc = new JCommander();
		jc.addObject(o);
		jc.setProgramName(getCommandName());
		try {
			jc.parse(args.toArray(new String[args.size()]));
			o.validate();
			args = null; // Prevent accidental usage instead of Options.args
		} catch(ParameterException | IllegalArgumentException e) {
			err.println(e.getMessage());
			err.println();
			printUsage(jc);
			return 1;
		}
		// FIXME: Implement
		return 1;
	}

	private static void printUsage(JCommander jc) {
		// TODO: As of 2019-11-11 with JCommander 1.71 JCommander.usage()
		// will print to stdout, not stderr, which is bad. So we fix that by
		// using the version which consumes a StringBuilder and printing on our
		// own. Try again in some years, file a bug if it still is wrong then.
		StringBuilder sb = new StringBuilder();
		jc.usage(sb);
		err.println(sb);
		
		err.println(
		    "Loads a checkpoint from INPUT_CHECKPOINT_DIR, applies the "
		  + "specified filters to it, and saves it to OUTPUT_CHECKPOINT_DIR if "
		  + "given or INPUT_CHECKPOINT_DIR otherwise.");
	}

}
