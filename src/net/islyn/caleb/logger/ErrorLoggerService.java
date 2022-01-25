package net.islyn.caleb.logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.islyn.caleb.miscellaneous.Constants;
import net.islyn.caleb.miscellaneous.FileTools;
import net.islyn.caleb.miscellaneous.LoggerHandler;
import net.islyn.caleb.miscellaneous.Masking;
import net.islyn.caleb.miscellaneous.StringTools;

public class ErrorLoggerService extends Thread {

	private ErrorQueueIntf mi;
	private String path;
	private boolean console;
	
	private Logger logger;
	private int cnt;
	private int max;
	private String sep;
	private int date;
	private volatile boolean isEnd;
	
	private static final int COUNT_MAX_LENGTH = 8;
	private static SimpleDateFormat DATE_FORMAT_0 = new SimpleDateFormat("[yyyy-MM-dd-HH.mm.ss.SSS] ");
	private static SimpleDateFormat DATE_FORMAT_9 = new SimpleDateFormat("yyyyMMdd");
	
	public ErrorLoggerService(
			final ErrorQueueIntf mi,
			final String path,
			final boolean console,
			final int maxkeep,
			final boolean append) throws Exception {
		super();
		
		this.mi = mi;
		this.path = path;
		this.console = console;
		
		// Sanity check.
		String dirPath = this.path + "logs";
		if (!FileTools.isDirectoryExists(dirPath)) {
			FileTools.makeDirectory(dirPath);
		}
				
		// Initialize logger.
		logger = Logger.getLogger("ErrorLogger");
		logger.setLevel(Level.INFO);
		logger.setUseParentHandlers(false);
		logger.addHandler(new LoggerHandler(
				"ER",
				dirPath,
				new ErrorLoggerFormatter(),
				maxkeep,
				append));
		
		// Initialize date.
		date = Integer.parseInt(DATE_FORMAT_9.format(new Date()));
		
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

	public void run() {
		// Set thread priority.
		try {
			setPriority(Constants.THREAD_PRIORITY_LOGGER);	
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		}
		
		while (!isEnd) {
			try {
				ErrorObject oerror = null;
				oerror = mi.dequeue(Constants.QUEUE_WAIT);
				
				// Has message?
				if (oerror != null) {					
					// Attempt to flush more data.
					ArrayList<ErrorObject> oerror_ = mi.flush();
					
					// Get logging timestamp.
					final long tstm_ = System.currentTimeMillis();
					final String tstm = DATE_FORMAT_0.format(tstm_);
					
					// Output data.
					if ((oerror_ == null) || (oerror_.size() <= 0)) {
						// One data.
						String count = "[" + getCount() + "] ";
						logger.info(toString(oerror, tstm, count));
						if (console) System.out.println(toString(oerror, tstm, count));
					
					} else {
						// Multiple data.
						String count = "[" + getCount() + "] ";
						logger.info(toString(oerror, tstm, count));
						if (console) System.out.println(toString(oerror, tstm, count));
						
						for (ErrorObject o : oerror_) {
							count = "[" + getCount() + "] ";
							logger.info(toString(o, tstm, count));
							if (console) System.out.println(toString(o, tstm, count));
						}
					}
				}
				
				// Date changed?
				int date_ = Integer.parseInt(DATE_FORMAT_9.format(new Date()));
				if (date_ != date) {
					date = date_;
					for (Handler h : logger.getHandlers()) {
						h.close();
					}
				}
				
			} catch (Exception ex) {
				ErrorLogger.log(ex.getMessage(), ex, null);
			}	
		}
	}

	private final String toString(
			final ErrorObject oerror,
			final String tstm,
			final String count) throws Exception {
		StringBuilder sb = new StringBuilder(32766);
		
		// Header.
		sb.append("[ERROR] " + tstm + count + sep);
		String v = oerror.getThreadName() 
				+ " : " 
				+ Long.toString(oerror.getThreadId())
				+ " : "
				+ ((oerror.getReference() != null) ? oerror.getReference() : "");
		sb.append(v + sep);
		
		// Write message.
		if (oerror.getMessage() != null) {
			sb.append(oerror.getMessage() + sep);
		}
		
		// Write stack trace.
		if (oerror.getException() != null) {
			sb.append("[STACKTRACE]" + sep);
			ByteArrayOutputStream baos = new ByteArrayOutputStream(6144);
			oerror.getException().printStackTrace(new PrintStream(baos));
			sb.append(StringTools.toString(baos.toByteArray()));
			sb.append(sep);
		}
		
		// Terminate logging.
		sb.append("[END]" + sep);
		
		return sb.toString();
	}
	
	private final String getCount() {		
		// Get masked count.
		String v = Masking.mask(
				false,
				COUNT_MAX_LENGTH, 0, 
				StringTools.ZEROS.substring(0, COUNT_MAX_LENGTH - 1) + "#",
				Integer.toString(cnt));
		
		// Increase log use count.
		cnt++;
		if (cnt > max) cnt = 0;
		
		// Return masked count.
		return v;
	}
	
	/**
	 * Shutdown this thread.
	 */
	public final synchronized void shutdown() {
		try {
			isEnd = true;
			interrupt();	
		} catch (Exception ex) {}
	}
	
	/**
	 * Finalize.
	 */
	public final void finalize() {
		// In the event of shutdown, delay this thread destruction to ensure critical logging.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex) {}
	}
	
}
