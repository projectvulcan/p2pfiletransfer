package net.islyn.caleb.miscellaneous;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import net.islyn.caleb.logger.ErrorLogger;

public class LoggerHandler extends StreamHandler {
	
	private String logname;
	private String path;
	private Formatter formatter;
	private int maxkeep;
	private boolean isAppend;
	
	private BufferedOutputStream bos;
	private FileOutputStream fos;
	
	private static SimpleDateFormat DATE_FORMAT_0 = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	private static SimpleDateFormat DATE_FORMAT_1 = new SimpleDateFormat("yyyyMMdd");
	
	public LoggerHandler(
			final String logname,
			final String path,
			final Formatter formatter,
			final int maxkeep,
			final boolean isAppend) throws Exception {
		this.logname = logname;
		this.path = path;
		this.formatter = formatter;
		this.maxkeep = maxkeep;
		this.isAppend = isAppend;
		
		open();
	}
	
	public synchronized void publish(LogRecord record) {
		// Prepare variables.
		String msg = null;
		
		// Attempt to get formatted message.
		try {
			msg = formatter.format(record);
		} catch (Exception ex) {
			msg = record.getMessage() + "\r\n";
		}
	
		// Prepare to write entry.
		try {
			open();	
			fos.write(StringTools.toByteArray(msg));
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		}
	}
	
	private synchronized void open() throws Exception {		
		if (bos == null) {			
			// Determine logging path.
			Date d = new Date();
			final String p = (path + "/" + logname + "_" + DATE_FORMAT_1.format(d) + ".txt");
			
			// Run housekeeping.
			housekeep(d);
			
			// Check if file already exists.
			if ((!isAppend) && (FileTools.isFileExists(p))) {
				final String p_ = (p + "." + DATE_FORMAT_0.format(d));
				FileTools.renameFile(p, p_);
			}
			
			// Open file for writing.
			fos = new FileOutputStream(p, isAppend);
			bos = new BufferedOutputStream(fos);
		}
	}
	
	private synchronized void housekeep(
			final Date d) {
		// Check files to delete.
		ArrayList<File> adeletes = new ArrayList<File>();
		LocalDateTime now = (d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		
		int nowYear = now.getYear();
		int nowMonth = now.getMonthValue();
		int nowDay = now.getDayOfMonth();
		int nowTotal = getDateTotal(nowDay, nowMonth, nowYear);
				
		try {	
			// List directory.
			File[] afiles = FileTools.listDirectory(path);
			for (File f : afiles) {
				String nam = f.getName();
				if ((nam.startsWith(logname)) && (nam.endsWith(".txt"))) {
					try {
						int x = nam.indexOf("_");
						int y = nam.indexOf(".", (x + 1));
						String old = nam.substring((x + 1), y);
						
						int oldYear = Integer.parseInt(old.substring(0, 4));
						int oldMonth = Integer.parseInt(old.substring(4, 6));
						int oldDay = Integer.parseInt(old.substring(6, 8));
						
						int diff = nowTotal - getDateTotal(oldDay, oldMonth, oldYear);
						
						if (diff >= maxkeep) {
							adeletes.add(f);
						}
					} catch (Exception ex) {
						ErrorLogger.log(ex.getMessage(), ex, null);
					}
				}
			}
			
			// Delete files.
			for (File f : adeletes) {
				f.delete();
			}
		
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		}
		
		// Clear memory.
		adeletes.clear();
		adeletes = null;
	}
	
	private int getDateTotal(int day, int month, int year) {
		return (int) Math.floor(365*year + year/4 - year/100 + year/400 + day + (153*month+8)/5);
	}
	
	public synchronized void close() {
		// Close buffered output stream.
		if (bos != null) {
			try {
				bos.close();
			} catch (Exception ex) {}
			bos = null;
		}
		
		// Close file output stream.
		if (fos != null) {
			try {
				fos.close();
			} catch (Exception ex) {}
			fos = null;
		}
	}
	
}
