package net.islyn.caleb.core;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import net.islyn.caleb.descriptor.FileTransferDescriptor;
import net.islyn.caleb.descriptor.ServerDescriptor;
import net.islyn.caleb.logger.ErrorLogger;
import net.islyn.caleb.logger.ErrorLoggerService;
import net.islyn.caleb.logger.ErrorQueue;
import net.islyn.caleb.logger.ErrorQueueIntf;
import net.islyn.caleb.logger.EventLogger;
import net.islyn.caleb.logger.EventLoggerService;
import net.islyn.caleb.logger.EventQueue;
import net.islyn.caleb.logger.EventQueueIntf;
import net.islyn.caleb.miscellaneous.Constants;
import net.islyn.caleb.miscellaneous.SocketTools;

/**
 * This class creates a server and listens for client connection.
 */
public class ListenerJob implements ListenerJobIntf {
	
	private ServerDescriptor			ss;
	private FileTransferDescriptor		ft;
	
	private volatile ServerJob[]		sr;
	private volatile int				sc;
	
	private ErrorLoggerService 			er;
	private EventLoggerService 			ev;
	
	private volatile boolean			isEnd;
	
	public ListenerJob(
			final ServerDescriptor ss,
			final FileTransferDescriptor ft) throws Exception {
		this.ss = ss;
		this.ft = ft;
		this.sr = new ServerJob[Constants.SOCKET_BACKLOG];
		this.sc = 0;
		
		// Create error logger.
		ErrorQueueIntf qerr = new ErrorQueue("ErrorQueue");
		ErrorLogger.setMessageQueue(qerr);
		er = new ErrorLoggerService(qerr, "pcalebrecv", true, 30, true);
		er.start();
		
		// Create event logger.
		EventQueueIntf qevt = new EventQueue("EventQueue");
		EventLogger.setMessageQueue(qevt);
		ev = new EventLoggerService(qevt, "pcalebrecv", true, 30, true);
		ev.start();
		
		// Pause to allow logger threads to start.
		try {
			Thread.sleep(100);
		} catch (InterruptedException ex) {}
	}
	
	/**
	 * Listen for client connection and handle file transfer.
	 */
	public final void listen() {
		// Prepare variables.
		ServerSocket sv = null;
		
		try {			
			// Create server.
			sv = new ServerSocket();
			
			// Attempt to bind.
			String bindIP = SocketTools.sanitizeServerIP(ss.listen_ip);
			
			try {
				if (bindIP == null) {
					sv.bind(new InetSocketAddress(
							InetAddress.getLocalHost(),
							ss.listen_port),
							Constants.SOCKET_BACKLOG);
					
				} else if (bindIP.equalsIgnoreCase("127.0.0.1")) {
					sv.bind(new InetSocketAddress(
							"127.0.0.1",
							ss.listen_port),
							Constants.SOCKET_BACKLOG);
				
				} else if (bindIP.equalsIgnoreCase("0.0.0.0")) {
					sv.bind(new InetSocketAddress(
							(InetAddress) null,
							ss.listen_port), 
							Constants.SOCKET_BACKLOG);
				
				} else {
					sv.bind(new InetSocketAddress(
							bindIP,
							ss.listen_port),
							Constants.SOCKET_BACKLOG);
				}	
			
			} catch (UnknownHostException ex) {
				sv.bind(new InetSocketAddress(
						InetAddress.getLocalHost(),
						ss.listen_port),
						Constants.SOCKET_BACKLOG);
				
			} catch (BindException ex) {
				sv.bind(new InetSocketAddress(
						InetAddress.getLocalHost(),
						ss.listen_port),
						Constants.SOCKET_BACKLOG);
			}
			
			// Set address reusable.
			sv.setReuseAddress(true);
			// Set timeout value.
			sv.setSoTimeout(ss.accept_timeout);
			// Set performance preferences.
			sv.setPerformancePreferences(0, 2, 1);
			
			// Log server started.
			EventLogger.info("Server started on " 
					+ sv.getInetAddress().getHostAddress() + ":" 
					+ sv.getLocalPort());
			EventLogger.info("Local directory path at " + ft.dir);
			
			// Listen for incoming client.
			while (!isEnd) {
				try {
					Socket sd = sv.accept();
					if (sd != null) {
						if (!assignThread(sd, ft, ss.read_timeout)) {
							if (sd != null) {
								try {
									EventLogger.warn("Server busy. Client "
											+ sd.getInetAddress().getHostAddress() + ":"
											+ sd.getPort() + " rejected.");
									sd.close();
								} catch (Exception ex) {}
								sd = null;	
							}
						}
					}
					
				} catch (SocketTimeoutException ex) {
					// Ignore this error.
				
				} catch (Exception ex) {
					throw ex;
				}
			}
			
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		
		} finally {
			// Shutdown clients.
			if ((sr != null) && (sr.length > 0)) {
				for (int i = 0; i < sr.length; i++) {
					sr[i].shutdown();
				}
			}
			
			// Shutdown server.
			if (sv != null) {
				try {
					sv.close();
				} catch (Exception ex) {}
				sv = null;
			}
			
			// Shutdown logger threads.
			if (ev != null) ev.shutdown();
			if (er != null) er.shutdown();
		}
	}
	
	private final synchronized boolean assignThread(
			final Socket sd,
			final FileTransferDescriptor ft,
			final int tmo_read) throws Exception {
		// Prepare variables.
		boolean isAssigned = false;
		
		// Scan through array for available slot.
		if (sc < sr.length) {
			for (int i = 0; i < sr.length; i++) {
				if (sr[i] == null) {
					sr[i] = new ServerJob(this, i, new SocketTools(sd, tmo_read), ft);
					sr[i].start();
					isAssigned = true;
					break;
				}
			}	
		}
		
		// Return assigned flag.
		return isAssigned;
	}

	@Override
	public final synchronized void shutdownCallback(
			final int id) {
		// Nullify position where thread is being removed.
		sr[id] = null;
	}
	
	/**
	 * Shutdown this thread.
	 */
	public final synchronized void shutdown() {
		try {
			isEnd = true;
		} catch (Exception ex) {}
	}

}
