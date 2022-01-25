package net.islyn.caleb.logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.islyn.caleb.miscellaneous.Constants;
import net.islyn.caleb.miscellaneous.FileTools;
import net.islyn.caleb.miscellaneous.LoggerHandler;

public class EventLoggerService extends Thread {

	private EventQueueIntf mi;
	private String path;
	private boolean console;

	private Logger logger;
	private int date;
	private volatile boolean isEnd;
	
	private static SimpleDateFormat DATE_FORMAT_9 = new SimpleDateFormat("yyyyMMdd");
	
	public EventLoggerService(
			final EventQueueIntf mi,
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
		logger = Logger.getLogger("EventLogger");
		logger.setLevel(Level.INFO);
		logger.setUseParentHandlers(false);
		logger.addHandler(new LoggerHandler(
				"EV",
				dirPath,
				new EventLoggerFormatter(),
				maxkeep,
				append));
		
		// Initialize date.
		date = Integer.parseInt(DATE_FORMAT_9.format(new Date()));
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
				EventObject oEvent = null;
				oEvent = mi.dequeue(Constants.QUEUE_WAIT);
				
				// Has message?
				if (oEvent != null) {
					// Attempt to flush more data.
					ArrayList<EventObject> oEvents = mi.flush();
					
					// Output data.
					if ((oEvents == null) || (oEvents.size() <= 0)) {
						// One data.
						final String sdata = oEvent.getString();
						logger.info(sdata);
						if (console) System.out.println(sdata);
					
					} else {
						// Multiple data.
						final String sdata = oEvent.getString();
						logger.info(sdata);
						if (console) System.out.println(sdata);

						for (EventObject oEvent_ : oEvents) {
							final String s = oEvent_.getString();
							logger.info(s);
							if (console) System.out.println(s);
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
				// Log unexpected error.
				ErrorLogger.log(ex.getMessage(), ex, null);
			}
		}
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
