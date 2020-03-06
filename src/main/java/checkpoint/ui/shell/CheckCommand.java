package checkpoint.ui.shell;

import static java.lang.System.err;

import java.util.List;

import com.beust.jcommander.JCommander;

/** @see #printUsage(JCommander) */
public final class CheckCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName()
			+ " [options] OLD_CHECKPOINT NEW_CHECKPOINT";
	}

	// TODO: Make printUsage() non-static and a mandatory function of classes
	// which implement Command. This ensures they all contain a description
	// of what they do so we can have the class-level JavaDoc merely consist of
	// "@see #printUsage(JCommander)".
	private static void printUsage(JCommander jc) {
		// TODO: As of 2019-11-11 with JCommander 1.71 JCommander.usage()
		// will print to stdout, not stderr, which is bad. So we fix that by
		// using the version which consumes a StringBuilder and printing on our
		// own. Try again in some years, file a bug if it still is wrong then.
		StringBuilder sb = new StringBuilder();
		jc.usage(sb);
		err.println(sb);
		
		err.println(
			"Compares the given checkpoint of an old copy of your data " +
			"against the other given checkpoint of a new copy of your data " +
			"to validate the integrity of your files, in terms of the " +
			"following checks:");
		err.println("FIXME: Listing the checks not implemented yet!");
	}

	@Override int run(List<String> args) {
		err.println("FIXME: Not implemented yet!");
		
		// FIXME: Print checkpoint.getDateEstimate() for the given old
		// checkpoint as it is heuristically detected, not stored in the file,
		// and determines the behavior of MatureINodeChangedCheck.
		
		// FIXME: Return 0 once implemented.
		return 1;
	}

}
