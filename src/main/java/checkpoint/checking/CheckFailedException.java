package checkpoint.checking;

/** Thrown when an {@link ICheck} fails.  
 *  The {@link #getMessage()} is suitable for printing to stderr. */
@SuppressWarnings("serial")
final class CheckFailedException extends Exception {

	CheckFailedException(String message) {
		// TODO: Performance: Check if omitting the stack trace by using an
		// appropriate super constructor yields any noticeable benefit for
		// checkpoints with many failed checks.
		super(message);
	}

}
