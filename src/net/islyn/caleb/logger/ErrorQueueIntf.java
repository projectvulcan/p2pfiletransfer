package net.islyn.caleb.logger;

import java.util.ArrayList;

public interface ErrorQueueIntf extends AbstractQueueIntf {
	
	public void enqueue(ErrorObject data) throws Exception;
	
	public ErrorObject dequeue(int timeout) throws Exception;
	
	public ArrayList<ErrorObject> flush() throws Exception;
	
}
