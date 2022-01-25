package net.islyn.caleb.miscellaneous;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import net.islyn.caleb.descriptor.ClientDescriptor;
import net.islyn.caleb.logger.ErrorLogger;

public class SocketTools {
	
	private ClientDescriptor 		cd;
	
	private Socket 					sd;
	private InputStream 			is;
	private OutputStream 			os;
	
	private long					la;
	private String 					li;
	private int						lp;
	private String					ri;
	private int						rp;
	
	private boolean					cl;
	
	/**
	 * Client job constructor.
	 * @param cd
	 */
	public SocketTools(
			final ClientDescriptor cd) {
		this.cd = cd;
		this.cl = true;
	}
	
	/**
	 * Server job constructor.
	 * @param sd
	 */
	public SocketTools(
			final Socket sd,
			final int tmo_read) throws Exception {
		this.sd = sd;
		this.is = sd.getInputStream();
		this.os = sd.getOutputStream();
		this.cl = false;
		
		// Create a working descriptor.
		this.cd = new ClientDescriptor();
		cd.read_timeout = tmo_read;
		
		// Capture remote IP and port, if possible.
		try {
			ri = sd.getInetAddress().getHostAddress();
			rp = sd.getPort();
		} catch (Exception ex) {}
		
		// Update last activity timestamp.
		la = System.currentTimeMillis();
	}
	
	public final void connect() throws Exception {
		// Sanity check.
		if (!cl) throw new UnsupportedOperationException();
		
		// Clean up existing connection.
		close();
		
		// Create a new socket.
		sd = new Socket();
		
		// Set lingering option.
		sd.setSoLinger(true, 5);
		// Set keep alive option.
		sd.setKeepAlive(true);
		// Set address reusable.
		sd.setReuseAddress(true);
		// Set whether to receive urgent data.
		sd.setOOBInline(false);
		
		// Sanity check.
		final String remoteIP 	= SocketTools.sanitizeRemoteIP(cd.remote_ip);
		final String bindIP		= SocketTools.sanitizeLocalIP(cd.local_ip);
		
		// Check for local binding.
		if ((remoteIP != null) && (remoteIP.equalsIgnoreCase("127.0.0.1"))) {
			try {
				sd.bind(new InetSocketAddress("127.0.0.1", 0));
			} catch (Exception ex) {
				ErrorLogger.log(ex.getMessage(), ex, null);
			}
		
		} else if (bindIP != null) {
			if ((!bindIP.equalsIgnoreCase("127.0.0.1")) && (!bindIP.equalsIgnoreCase("0.0.0.0"))) {
				try {
					sd.bind(new InetSocketAddress(bindIP, 0));
				} catch (Exception ex) {
					ErrorLogger.log(ex.getMessage(), ex, null);
				}
			}
		}
				
		// Set connect timeout value.
		sd.setSoTimeout(cd.connect_timeout);
		
		// Attempt to connect to remote host.
		if (remoteIP == null) {
			sd.connect(new InetSocketAddress(InetAddress.getLocalHost(), cd.remote_port));
		} else if ((remoteIP != null) && (remoteIP.equalsIgnoreCase("all"))) {
			sd.connect(new InetSocketAddress((InetAddress) null, cd.remote_port));	
		} else {
			sd.connect(new InetSocketAddress(remoteIP, cd.remote_port));
		}
		
		// Initialize streams.
		is = sd.getInputStream();
		os = sd.getOutputStream();
		
		// Capture local information.
		li = this.sd.getInetAddress().getHostAddress();
		lp = this.sd.getLocalPort();
		
		// Update last activity timestamp.
		la = System.currentTimeMillis();
	}
	
	public final void close() {
		// Close input stream.
		if (is != null) {
			try {
				is.close();
			} catch (Exception ex) {}
			is = null;
		}
		
		// Close output stream.
		if (os != null) {
			try {
				os.close();
			} catch (Exception ex) {}
			os = null;
		}
		
		// Close socket.
		if (sd != null) {
			try {
				sd.close();
			} catch (Exception ex) {}
			sd = null;
		}
		
		// Update last activity timestamp.
		la = -1;
	}
	
	public final byte[] read(
			final int ilen,
			final int tmo_read_cont) throws Exception {
		// Prepare variables.
		byte[] b = new byte[ilen];
		int rc = 0;
		int ofs = 0;
		int len = b.length;
		int nloop = 0;
		
		// Reset waiting time.
		sd.setSoTimeout(cd.read_timeout);
		
		// DDOS attack handling.
		long wait = sd.getSoTimeout();
		long start = System.currentTimeMillis();
		
		// Read from socket.
		try {
			while ((rc = is.read(b, ofs, len)) > 0) {
				long now = System.currentTimeMillis();
				ofs += rc;
				len -= rc;
				nloop++;
								
				// DDOS attack handling.
				if ((nloop == 1) && (len > 0)) {
					sd.setSoTimeout(tmo_read_cont);
				} else {
					long elapsed = (now - start);
					if ((elapsed > wait) && (ofs < ilen)) {
						throw new Exception("Waited too long: " + Long.toString(elapsed) + "ms for "
								+ Integer.toString(ofs) + " bytes. "
								+ "Expecting " + Integer.toString(ilen) + " bytes. "
								+ "Looped " + Integer.toString(nloop) + "x.");
					}
				}
			}
			
			// Update last activity timestamp.
			la = System.currentTimeMillis();
		
		} catch (SocketTimeoutException ex) {
			// Socket read has timed out.
			if (ofs > 0) {
				// We had partial read. Could be error or could be a DDOS attack.
				long now = System.currentTimeMillis();
				long elapsed = (now - start);
				throw new Exception("Incomplete data. Waited " + Long.toString(elapsed) + "ms for "
						+ Integer.toString(ofs) + " bytes. "
						+ "Expecting " + Integer.toString(ilen) + " bytes.");
				
			} else {
				// We read nothing at all. Throw error.
				throw ex;
			}
			
		} catch (Exception ex) {
			// Generic error. Throw to caller.
			throw ex;
		
		} finally {
			// Send to logger.
		}
		
		// Sanity check.
		if (rc < 0) {
			return null;
		} else if (ofs < ilen) {
			byte[] b_ = new byte[ofs];
			System.arraycopy(b, 0, b_, 0, b_.length);
			return b_;
		} else {
			return b;
		}
	}
	
	public final void write(
			final byte[] blen,
			final byte[] bdata) throws Exception {
		// Sanity check.
		if (blen != null) {
			// Concatenate length and body before writing to socket.
			byte[] bmessage = new byte[(blen.length + bdata.length)];
			System.arraycopy(blen, 0, bmessage, 0, blen.length);
			System.arraycopy(bdata, 0, bmessage, blen.length, bdata.length);
			os.write(bmessage);
		
		} else if (bdata != null) {
			// Write direct to socket.
			os.write(bdata);
		}
		
		// Update last activity timestamp.
		la = System.currentTimeMillis();
	}
	
	public static final String sanitizeServerIP(
			final String givenIP) {
		// Prepare variables.
		String sanitizedIP = givenIP;
		
		// Check replacement values.
		if (givenIP != null) {
			if (givenIP.trim().length() == 0) {
				sanitizedIP = "127.0.0.1";
			} else if (givenIP.trim().equalsIgnoreCase("localhost")) {
				sanitizedIP = "127.0.0.1";
			} else if (givenIP.trim().equalsIgnoreCase("default")) {
				sanitizedIP = "0.0.0.0";
			} else if (givenIP.trim().equalsIgnoreCase("all")) {
				sanitizedIP = "0.0.0.0";
			}
		}
		
		// Return sanitized IP.
		return (sanitizedIP == null) ? sanitizedIP : sanitizedIP.trim();
	}
		
	public static final String sanitizeRemoteIP(
			final String givenIP) {
		// Prepare variables.
		String sanitizedIP = givenIP;
		
		// Check replacement values.
		if (givenIP != null) {
			if (givenIP.trim().length() == 0) {
				sanitizedIP = "127.0.0.1";
			} else if (givenIP.trim().equalsIgnoreCase("localhost")) {
				sanitizedIP = "127.0.0.1";
			} else if (givenIP.trim().equalsIgnoreCase("default")) {
				sanitizedIP = "127.0.0.1";
			} else if (givenIP.trim().equalsIgnoreCase("0.0.0.0")) {
				sanitizedIP = "127.0.0.1";
			}
		}
		
		// Return sanitized IP.
		return (sanitizedIP == null) ? sanitizedIP : sanitizedIP.trim();
	}
	
	public static final String sanitizeLocalIP(
			final String givenIP) {
		// Prepare variables.
		String sanitizedIP = givenIP;
		
		// Check replacement values.
		if (givenIP != null) {
			if (givenIP.trim().equalsIgnoreCase("localhost")) {
				sanitizedIP = "127.0.0.1";
			} else if (givenIP.trim().equalsIgnoreCase("default")) {
				sanitizedIP = "127.0.0.1";
			}
		}
		
		// Return sanitized IP.
		return (sanitizedIP == null) ? sanitizedIP : sanitizedIP.trim();
	}
	
	public final int getMessageLength(
			final byte[] blength) {
		// Prepare variables.
		int ilen = -1;
		
		// Attempt to convert value.
		try {
			ilen = cvBytesToInteger(blength);
		} catch (Exception ex) {}
		
		// Return value.
		return ilen;
	}
	
	public final byte[] getMessageLength(
			final int ilen) {
		// Return value.
		return cvIntegerToBytes(ilen);
	}
	
	public final String getLocalIP() {
		if (cl) {
			return li;	
		} else {
			return sd.getLocalAddress().getHostAddress();
		}
	}
	
	public final int getLocalPort() {
		if (cl) {
			return lp;
		} else {
			return sd.getLocalPort();
		}
	}
	
	public final String getRemoteIP() {
		String ip = null;
		try {
			ip = (ri != null) ? ri : sd.getInetAddress().getHostAddress();
		} catch (Exception ex) {}
		return ip;
	}
	
	public final int getRemotePort() {
		int pr = 0;
		try {
			pr = (rp > 0) ? rp : sd.getPort();
		} catch (Exception ex) {}
		return pr;
	}
	
	/**
	 * Determine whether socket is connected.
	 * @return
	 */
	public final boolean isConnected() {
		// Prepare variables.
		boolean isConnected = false;
		
		try {
			// Check whether socket descriptor says still connected.
			isConnected = (sd != null) ? sd.isConnected() : false;
			// Check whether connection has been idle for more specified period.
			if (isConnected) {
				long now = System.currentTimeMillis();
				long elp = (now - la);
				isConnected = (elp <= Constants.SOCKET_MAX_IDLE);
			}
		} catch (Exception ex) {}
		
		// Return connected flag.
		return isConnected;
	}
	
	public final byte[] cvIntegerToBytes(final int val) {
		return new byte[] {((byte) (val >>> 24)), ((byte) (val >>> 16)),
				((byte) (val >>> 8)), ((byte) val)};
	}
	
	public final int cvBytesToInteger(final byte[] val) {
		if (val.length == 2) 
			return (((val[0] & 0xff) << 8) + (val[1] & 0xff));
		
		else if (val.length == 4)
			return (((val[0] & 0xff) << 24) + ((val[1] & 0xff) << 16) +
					((val[2] & 0xff) << 8) + (val[3] & 0xff));
		
		else return 0;
	}
	
}
