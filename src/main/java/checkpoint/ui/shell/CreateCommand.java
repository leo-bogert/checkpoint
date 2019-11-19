package checkpoint.ui.shell;

import static java.lang.System.err;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import checkpoint.generation.ConcurrentCheckpointGenerator;
import checkpoint.generation.JavaSHA256Generator;

final class CreateCommand extends Command {

	@Override String getShortSyntax() {
		return '\t' + getCommandName()
			+ " [options] INPUT_DIR OUTPUT_CHECKPOINT_DIR";
	}

	/** TODO: As of 2019-11-11 Travis CI uses JCommander 1.48 which does not
	 *  support "order = integer" to specify the order in which the Parameters
	 *  appear in the help, try again in some years.
	 *  See the commit which added this comment for what the orders were. */
	private static final class Options {
		/** TODO: As of 2019-11-13 it does not seem trivially possible to query
		 *  this from Java. Try again in some years. */
		@Parameter(names = { "--ssd" }, description =
			  "Assume the input disk to be a Solid State Drive. If not given "
			+ "it is assumed to be a rotational disk instead. The way files "
			+ "are processed needs to be different for each in order to get "
			+ "good performance so you should ensure your choice is correct. "
			+ "This also affects the default thread count, see '--threads'.")
		boolean ssd = false;

		@Parameter(names = { "--threads" }, description =
			  "Number of threads to process files/directories with. "
			+ "Must be at least 1. Default for rotational disks: "
			+ ConcurrentCheckpointGenerator.DEFAULT_THREAD_COUNT_HDD + ". "
			+ "If --ssd is used, default is: " +
			+ ConcurrentCheckpointGenerator.DEFAULT_THREAD_COUNT_SSD + ". "
			+ "For RAID1 on rotational disks set this to the number of disks. "
			+ "For other RAID types which can read different data from "
			+ "different disks in parallel set this to the number of disks "
			+ "which can be used concurrently. "
			+ "Each thread will use as much memory as given via --buffer, in "
			+ "addition to about 1 MiB for Java's default stack size.")
		Integer threads = null; // No default because it depends on --ssd

		@Parameter(names = { "--buffer" }, description =
			  "I/O buffer per thread, in bytes. Must at least 4096. "
			+ "Making it divisible by 4096 (= x86 pagesize) is a good idea. "
			+ "The default matches Linux 4.15's default disk read ahead value "
			+ "at '/sys/block/DISK/queue/read_ahead_kb'. "
			+ "Setting this higher than your kernel's read ahead amount can "
			+ "result in poor performance for files larger than the buffer "
			+ "because the processing threads will do SHA256 hashing in "
			+ "between buffer reads so then the kernel won't read ahead enough "
			+ "data during that time to fill the next buffer instantly. "
			+ "Thus if you increase the buffer size consider setting the sysfs "
			+ "value at least as high. Also consider what is said about memory "
			+ "usage at '--threads' before increasing the buffer size! "
			+ "You may also have to allow Java to use more memory with -Xmx.")
		int buffer = JavaSHA256Generator.DEFAULT_READ_BUFFER_SIZE;

		@Parameter(description =
			"INPUT_DIR OUTPUT_CHECKPOINT_DIR")
		List<String> args = new ArrayList<>(2);

		void validate() throws IllegalArgumentException {
			if(threads != null && threads < 1)
				throw new IllegalArgumentException("--threads is too low!");
			
			if(buffer < 4096)
				throw new IllegalArgumentException("--buffer is too low!");
			
			// TODO: As of 2019-11-11 with JCommander 1.71 @Parameter(arity = 2)
			// doesn't work for unnamed parameters it seems so we check it
			// manually, try again in some years.
			if(args.size() < 2)
				throw new IllegalArgumentException("Missing input/output dir!");
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
		
		// TODO: Use IStringConverterFactory of JCommander instead of manually
		// processing paths.
		Path input;
		Path output;
		try {
			input = Paths.get(o.args.get(0));
			output = Paths.get(o.args.get(1));
		} catch(InvalidPathException e) {
			err.println("Invalid path: " + e.getMessage());
			return 1;
		}
		
		try {
			new ConcurrentCheckpointGenerator(input, output,
				o.ssd, o.threads, o.buffer).run();
			return 0;
		} catch (IOException | InterruptedException e) {
			err.println("Generating checkpoint failed:");
			e.printStackTrace(err);
			return 1;
		}
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
			"Searches all files and directories in the INPUT_DIR and " +
			"writes a checkpoint for them to the OUTPUT_CHECKPOINT_DIR.");
	}

}
