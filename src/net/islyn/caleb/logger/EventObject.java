package net.islyn.caleb.logger;

public class EventObject {

	private long timestamp;
	private String str;
	
	public EventObject(
			long timestamp,
			final String str) {
		this.timestamp = timestamp;
		this.str = str;
	}
	
	public final long getTimestamp() {
		return timestamp;
	}
	
	public final String getString() {
		return str;
	}
	
}
