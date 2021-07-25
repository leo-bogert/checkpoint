package checkpoint.checking;

import static java.util.Objects.requireNonNull;

import checkpoint.datamodel.INode;

/** Thrown when an {@link ICheck} fails.  
 *  The {@link #getMessage()} is suitable for printing to stderr. */
@SuppressWarnings("serial")
public final class CheckFailedException extends Exception {

	/** @param message Must be suitable for printing to stderr. The path of the
	 *      affected {@link INode} must not be included, it will be printed by
	 *      the {@link CheckRunner}. */
	public CheckFailedException(String message) {
		// TODO: Performance: Check if omitting the stack trace by using this
		// appropriate super constructor yields any noticeable benefit for
		// checkpoints with many failed checks:
		/*     super(message, null, true, false); */
		// Notice: Perhaps overriding fillInStackTrace() is required instead
		// because fillInStackTrace() contains an alternate code path which
		// may still run even if we tell the above constructor that the
		// stack trace is not writable - I did not check in detail what runs
		// that code path.
		super(requireNonNull(message));
	}

}
