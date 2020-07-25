package checkpoint.ui.shell;

import static java.lang.System.err;
import static java.lang.System.exit;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** The terminal user interface. */
public final class Shell {

	/** To add a new shell {@link Command} all you need to do is to add its
	 *  class to this list. The following code will automatically construct an
	 *  object of it and register its name for usage. */
	private static final List<Class<? extends Command>> commandClasses = asList(
		CheckFsFeaturesCommand.class,
		CreateCommand.class,
		FilterCommand.class,
		InspectCommand.class,
		RewriteCommand.class);

	private static final Map<String, Command> commandMap
		= Command.getCommandMap(commandClasses);

	// TODO: Replace args processing with JCommander, CreateCommand already
	// uses it and it seems like a nice library.
	public static void main(String[] args) {
		try { // Ensure we exit(1) upon any exception.
			Command command;
			
			if(args.length < 1 || (command = commandMap.get(args[0])) == null) {
				printSyntax();
				exit(1);
				return; // Satisfy compiler
			}
			
			// asList() doesn't support remove(0) so wrap it in an ArrayList.
			List<String> commandArgs = new ArrayList<>(asList(args));
			commandArgs.remove(0);
			
			exit(command.run(commandArgs));
		} catch(Throwable t) {
			// Command.run() shouldn't throw so there was a serious problem,
			// e.g. OutOfMemoryError.
			// Thus:
			// - tell the user
			// - exit(1) in the finally{} to ensure we can exit(1) even if we
			//   get another OutOfMemoryError.
			err.println("Command failed fatally, please report this:");
			t.printStackTrace(err);
		} finally {
			exit(1);
		}
	}

	private static void printSyntax() {
		err.println("Syntax:");
		err.println("\tcheckpoint COMMAND");
		err.println();
		err.println("COMMAND can be:");
		SortedMap<String, Command> sortedCommands = new TreeMap<>(commandMap);
		for(Command c : sortedCommands.values())
			err.println(c.getShortSyntax());
		err.println();
		err.println(
			"To get more help about a COMMAND run it without any arguments.");
	}

}
