package net.islyn.caleb.logger;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ErrorLoggerFormatter extends Formatter {

	private String sep;

	public ErrorLoggerFormatter() {
		// Get line separator.
		sep = System.getProperty("line.separator");
	}
	
	@Override
	public String format(LogRecord record) {		
		// Return log record.
		return record.getMessage() + sep;
	}

}
