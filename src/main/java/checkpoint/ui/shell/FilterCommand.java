package checkpoint.ui.shell;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
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
		// FIXME: Implement
		return 1;
	}

}
