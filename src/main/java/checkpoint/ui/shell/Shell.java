package checkpoint.ui.shell;

import static java.lang.System.err;
import static java.lang.System.exit;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import checkpoint.checking.ICheck;

/** The terminal user interface.
 * 
 *  FIXME: CreateCommand and CheckCommand support obeying
 *  {@link Thread#interrupt()} because they might take a very long time to
 *  execute.  
 *  Check if CTRL+C actually does interrupt their threads.  
 *  If not, register a SIGINT handler and deploy InterruptedException on our
 *  own to support graceful shutdown of these operations.  
 *  Notice: Some brief web searching shows that CTRL+C will just kill the JVM
 *  by default, so a SIGINT handler is probably necessary.  
 *  **EDIT:** A shutdown hook will likely be easier to add. The open question is
 *  if the JVM will give the hook infinite time to wait for it to terminate
 *  other threads, or if the JVM is killed after a short delay.
 *  Test that. */
public final class Shell {

	/** To add a new shell {@link Command} all you need to do is to add its
	 *  class to this list. The following code will automatically construct an
	 *  object of it and register its name for usage.
	 *  
	 *  TODO: Move to {@link Command}, call it IMPLEMENTATIONS there, wrap it
	 *  in unmodifiableList().  
	 *  Document it similar to {@link ICheck#IMPLEMENTATIONS}, also mention it
	 *  at the class-level JavaDoc like at ICheck. */
	private static final List<Class<? extends Command>> commandClasses = asList(
		CheckCommand.class,
		CheckFsFeaturesCommand.class,
		CreateCommand.class,
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
