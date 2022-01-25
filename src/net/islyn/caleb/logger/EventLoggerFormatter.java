package net.islyn.caleb.logger;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import net.islyn.caleb.miscellaneous.Masking;
import net.islyn.caleb.miscellaneous.StringTools;

public class EventLoggerFormatter extends Formatter {

	private int cnt;
	private int max;
	private String sep;

	private static final int COUNT_MAX_LENGTH = 10;
	private static SimpleDateFormat DATE_FORMAT_0 = new SimpleDateFormat("[yyyy-MM-dd-HH.mm.ss.SSS] ");
	
	public EventLoggerFormatter() {
		// Get line separator.
		sep = System.getProperty("line.separator");
		// Initialize counter.
		cnt = 0;
		// Initialize max counter.
		max = 10;
		for (int i = 0; i < (COUNT_MAX_LENGTH - 1); i++) {
			max = (max * 10);
		}
	}
	
	@Override
	public String format(LogRecord record) {
		// Prepare record.
		String msg = "[" + getCount(cnt) + "] "
				+ DATE_FORMAT_0.format(record.getMillis())
				+ record.getMessage();
		
		// Increase log use count.
		cnt++;
		if (cnt > max) cnt = 0;
		
		// Return log record.
		return msg + sep;
	}

	public static final String getCount(final int cnt) {
		return Masking.mask(
				false,
				COUNT_MAX_LENGTH, 0, 
				StringTools.ZEROS.substring(0, COUNT_MAX_LENGTH - 1) + "#",
				Integer.toString(cnt));
	}
	
}
