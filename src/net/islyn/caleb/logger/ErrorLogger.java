package net.islyn.caleb.logger;

public class ErrorLogger {

	private static ErrorQueueIntf mi;
	
	public static final void setMessageQueue(final ErrorQueueIntf mi) {
		ErrorLogger.mi = mi;
	}
	
	public static final void log(
			final String msg,
			final Exception exp,
			final String ref) {
		log(msg, exp, Thread.currentThread().getName(), Thread.currentThread().getId(), ref);
	}
	
	public static final void log(
			final String msg,
			final Exception exp,
			final String nam,
			final long id,
			final String ref) {
		log(new ErrorObject(
				msg,
				exp,
				nam,
				id,
				ref));
	}
	
	public static final void log(final ErrorObject oerror) {
		try {
			if (mi != null) {
				mi.enqueue(oerror);
			} else {
				if (oerror.getMessage() != null) System.out.println(oerror.getMessage());
				if (oerror.getException() != null) oerror.getException().printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
