package net.islyn.caleb.logger;

import java.util.ArrayList;

public interface EventQueueIntf extends AbstractQueueIntf {
	
	public void enqueue(EventObject data) throws Exception;
	
	public EventObject dequeue(int timeout) throws Exception;

	public ArrayList<EventObject> flush() throws Exception;
	
}
