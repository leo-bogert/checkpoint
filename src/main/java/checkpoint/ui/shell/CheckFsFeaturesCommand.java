package checkpoint.ui.shell;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import checkpoint.datamodel.implementation.Timestamps;

final class CheckFsFeaturesCommand extends Command {

	@Override String getCommandName() {
		return "check-fs-features";
	}

	@Override String getShortSyntax() {
		return '\t' + getCommandName() + " PATH_OF_FILESYSTEM";
	}

	@Override int run(List<String> args) {
		if(args.size() != 1) {
			err.println("Syntax:");
			err.println(getShortSyntax());
			err.println();
			err.println(
				"Prints the features of your Java version for reading file " +
				"timestamps on the given filesystem.");
			return 1;
		}
		
		Path p;
		FileSystem fs;
		try {
			p = Paths.get(args.get(0));
			fs = p.getFileSystem();
		} catch(InvalidPathException e) {
			err.println("Invalid path given:");
			e.printStackTrace(err);
			return 1;
		}
		
		if(!fs.supportedFileAttributeViews().contains("unix")) {
			err.println(
				"ERROR: Your Java does not support the 'unix' " +
				"FileAttributeView so checkpoint likely will not work!");
		} else {
			out.println(
				"Good: The 'unix' FileAttributeView is supported, this is " +
				"the view which checkpoint uses currently.");
		}
		out.println();
		
		// TODO: Allow enabling this with --verbose.
		/*
		for(String view : fs.supportedFileAttributeViews()) {
			out.println("Supported timestamps for FileAttributeView '"
				+ view + "':");
			
			try {
				SortedMap<String, Object> attrs
					= new TreeMap<>( // Wrap Map in TreeMap to sort it.
						Files.readAttributes(p, view + ":*"));
				
				for(Entry<String, Object> e : attrs.entrySet()) {
					if(e.getValue() instanceof FileTime)
						out.println('\t' + e.getKey());
				}
			} catch (IOException e) {
				err.println("Error while determining supported timestamps:");
				e.printStackTrace(err);
			}
			out.println();
		}
		*/
		
		out.println("Trying to actually read Unix timestamps of " + p + " ...");
		try {
			Timestamps.readTimestamps(p);
			out.println("OK! Checkpoint will likely work.");
			out.println("WARNING: " +
				"Nevertheless please manually validate the timestamps in a "  +
				"checkpoint at least once because some Java implementations " +
				"return a different timestamp for ones for which they have "  +
				"no code to read them from the OS.");
			return 0;
		} catch(IOException | RuntimeException e) {
			err.println("Reading timestamps failed:");
			e.printStackTrace(err);
			return 1;
		}
	}

}
