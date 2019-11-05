package checkpoint.ui.shell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents a single shell command which is executable via
 *  "checkpoint NAME_OF_COMMAND".
 *  
 *  Instances MUST BE stateless because they will be statically constructed by
 *  {@link Shell} and future implementations may thus re-use them.
 *  
 *  The class name MUST BE "NAME_OF_COMMAND" plus "Command" as
 *  {@link #getCommandName()} uses the class name to automatically compute the
 *  command name. The case needs not to be lower/upper, it will be ignored. */
abstract class Command {

	final String getCommandName() {
		return this.getClass().getSimpleName()
				.replace("Command", "").toLowerCase();
	}

	/** Returns a map where the key is the name of each Command and the value
	 *  is a new instance of it. */
	static Map<String, Command> getCommandMap(
			List<Class<? extends Command>> classes) {
		
		// TODO: Performance: Use ArrayMap once we have one from Java Commons
		HashMap<String, Command> map = new HashMap<>();
		for(Class<? extends Command> c : classes) {
			try {
				Command command = c.getDeclaredConstructor().newInstance();
				map.put(command.getCommandName(), command);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		return map;
	}

	/** Must return a single line consisting of:
	 *  - a '\t' character.
	 *  - the value of {@link #getCommandName()}.
	 *  - a space plus a human-readable list of arguments for the command.
	 *  
	 *  E.g:
	 *      return '\t' + getCommandName() + " INPUT_DIR [OUTPUT_DIR]"; */
	abstract String getShortSyntax();

	/**
	 * Must return 0 upon success, > 0 otherwise.
	 * Should catch all potential exceptions already and convert them into such
	 * a return value.
	 * 
	 * @param args The arguments of the command, with the command name already
	 *  	having been removed from the beginning of the list. */
	abstract int run(List<String> args);

}
