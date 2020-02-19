package checkpoint.checking;

/** Thrown when an {@link ICheck} fails.  
 *  The {@link #getMessage()} is suitable for printing to stderr. */
@SuppressWarnings("serial")
final class CheckFailedException extends Exception {

	CheckFailedException(String message) {
		// TODO: Performance: Check if omitting the stack trace by using this
		// appropriate super constructor yields any noticeable benefit for
		// checkpoints with many failed checks:
		/*     super(message, null, true, false); */
		// Notice: Perhaps overriding fillInStackTrace() is required instead
		// because fillInStackTrace() contains an alternate code path which
		// may still run even if we tell the above constructor that the
		// stack trace is not writable - I did not check in detail what runs
		// that code path.
		super(message);
	}

}