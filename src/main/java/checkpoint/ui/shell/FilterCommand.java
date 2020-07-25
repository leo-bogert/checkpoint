package checkpoint.ui.shell;

import java.util.List;

final class FilterCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName() +
			" [options] INPUT_CHECKPOINT_DIR [OUTPUT_CHECKPOINT_DIR]";
	}

	@Override int run(List<String> args) {
		// FIXME: Implement
		return 1;
	}

}
