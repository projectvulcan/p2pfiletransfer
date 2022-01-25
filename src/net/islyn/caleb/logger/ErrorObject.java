package net.islyn.caleb.logger;

public class ErrorObject {

	private String msg;
	private Exception exp;
	private String nam;
	private long id;
	private String ref;
	
	public ErrorObject(
			final String msg,
			final Exception exp,
			final String nam,
			final long id,
			final String ref) {
		this.msg = msg;
		this.exp = exp;
		this.nam = nam;
		this.id = id;
		this.ref = ref;
	}
	
	public final String getMessage() {
		return msg;
	}
	
	public final Exception getException() {
		return exp;
	}
	
	public final String getThreadName() {
		return nam;
	}
	
	public final long getThreadId() {
		return id;
	}
	
	public final String getReference() {
		return ref;
	}
	
}
