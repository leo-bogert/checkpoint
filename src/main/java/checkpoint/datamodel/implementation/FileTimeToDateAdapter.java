package checkpoint.datamodel.implementation;

import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

import checkpoint.datamodel.ITimestamps;

/** Java 7 does not yet support:
 *   - class Instant
 *   - {@link FileTime}.getInstant().
 *   - class DateTimeFormatter to consume objects of Instant.
 *  
 *  Thus the only way to convert a FileTime to a String seems to be to use
 *  {@link FileTime#toMillis()}, feed that into the legacy class
 *  {@link Date} and use {@link SimpleDateFormat} upon it.
 *  
 *  Thus this adapter converts the return values of {@link ITimestamps}' getters
 *  from {@link FileTime} to {@link Date} objects.
 *  
 *  TODO: Java 8: Get rid of this. */
final class FileTimeToDateAdapter {

	private final ITimestamps timestamps;

	FileTimeToDateAdapter(ITimestamps timestamps) {
		this.timestamps = timestamps;
	}

	Date getAccessTime() {
		return new Date(timestamps.getAccessTime().toMillis());
	}

	Date getBirthTime() {
		return null;
	}

	Date getStatusChangeTime() {
		return new Date(timestamps.getStatusChangeTime().toMillis());
	}

	Date getModificationTime() {
		return new Date(timestamps.getModificationTime().toMillis());
	}

}
