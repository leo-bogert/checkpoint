package checkpoint.datamodel.implementation;

import static java.lang.System.err;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedList;

import checkpoint.datamodel.INode;
import checkpoint.datamodel.INodeFinder;

public final class NodeFinder extends SimpleFileVisitor<Path>
		implements INodeFinder {

	private static final Path currentDir = Paths.get(".");

	private Path              inputDir;
	private FileStore         inputDirFilesystem;
	private Collection<INode> result;

	@Override public Collection<INode> findNodes(Path inputDir)
			throws IOException {
		
		if(!Files.isDirectory(inputDir, NOFOLLOW_LINKS))
			throw new IOException("Input path is not a directory: " + inputDir);
		
		// Sanitize path to ease the work of adjustPath() which we will call a
		// lot. TODO: Performance: Check with debugger if this actually makes
		// sense.
		this.inputDir           = inputDir.toAbsolutePath().normalize();
		this.inputDirFilesystem = Files.getFileStore(inputDir);
		// TODO: Performance: Try different data structures.
		this.result             = new LinkedList<INode>();
		
		Files.walkFileTree(inputDir, this);
		
		Collection<INode> result = this.result;
		// Null our members, especially result, to prevent huge memory leak
		// which would occur if a caller kept the NodeFinder object alive after
		// we've returned.
		this.inputDir = null;
		this.inputDirFilesystem = null;
		this.result = null;
		
		return result;
	}

	private boolean isOnInputDirFilesystem(Path p) throws IOException {
		return Files.getFileStore(p).equals(inputDirFilesystem);
	}

	private Path adjustPath(Path p) {
		// Strip "inputDir/" from front of p
		Path relative = inputDir.relativize(p);
		// Add "./" to front of p
		relative = currentDir.resolve(relative);
		return relative;
	}

	@Override public FileVisitResult preVisitDirectory(Path dir,
			BasicFileAttributes attrs) throws IOException {

		if(isOnInputDirFilesystem(dir)) {
			result.add(Node.constructNode(adjustPath(dir), true));
			return CONTINUE;
		} else {
			err.println("Ignoring different filesystem: " + dir);
			return SKIP_SUBTREE;
		}
	}

	@Override public FileVisitResult visitFile(Path file,
			BasicFileAttributes attrs) throws IOException {
		
		if(attrs.isRegularFile()) {
			// While popular Linux knowledge is that mount points are usually a
			// directory I have in fact encountered *files* in /tmp being a
			// mount point on my live system, so we need to check
			// isOnInputDirFilesystem() here as well.
			// FIXME: Performance: This is actually even ignored by
			// "find -mount", and thus by the Python/Bash implementations,
			// perhaps do so here as well?
			if(isOnInputDirFilesystem(file))
				result.add(Node.constructNode(adjustPath(file), false));
			else
				err.println("Ignoring file on different filesystem: " + file);
		} else {
			// TODO: Log ignored special files somehow?
			// The Python/Bash implementations didn't either.
		}
		
		return CONTINUE;
	}

}
