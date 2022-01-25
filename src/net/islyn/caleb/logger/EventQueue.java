package net.islyn.caleb.logger;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventQueue implements EventQueueIntf {

	private String name;
	private LinkedBlockingQueue<EventObject> que;
	
	public EventQueue(final String name) {
		this.name = name;
		this.que = new LinkedBlockingQueue<EventObject>(16384);
	}
	
	public void enqueue(final EventObject data) throws Exception {
		boolean success = false;
		int retry = 0;
		
		if ((!(success = que.offer(data))) && (retry < 10)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {}
			retry++;
		}
		
		if (!success) throw new Exception("Enqueue not successful: " + que.size() + " " + name);
	}
	
	public EventObject dequeue(int timeout) throws Exception {
		try {
			return que.poll(timeout, TimeUnit.MILLISECONDS);	
		} catch (InterruptedException ex) {}
		return null;
	}
	
	public ArrayList<EventObject> flush() throws Exception {
		ArrayList<EventObject> data = new ArrayList<EventObject>();
		int rc = que.drainTo(data);
		if (rc <= 0) data = null;
		return data;
	}
	
	public String getName() {
		return name;
	}
	
	public int getCount() {
		return que.size();
	}
	
}
