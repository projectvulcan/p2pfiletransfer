package net.islyn.caleb.logger;

public class EventLogger {

	private static EventQueueIntf mi;
	
	public static final void setMessageQueue(final EventQueueIntf mi) {
		EventLogger.mi = mi;
	}
	
	public static final void info(final String info) {
		try {
			if (mi != null) mi.enqueue(new EventObject(System.currentTimeMillis(), "[INFO ] " + info));	
			else System.out.println(info);
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		}
	}

	public static final void warn(final String warn) {
		try {
			if (mi != null) mi.enqueue(new EventObject(System.currentTimeMillis(), "[WARN ] " + warn));	
			else System.out.println(warn);
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		}
	}	
	
	public static final void debug(final String debug) {
		try {
			if (mi != null) mi.enqueue(new EventObject(System.currentTimeMillis(), "[DEBUG] " + debug));	
			else System.out.println(debug);
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		}
	}
	
}
