package net.islyn.caleb.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.islyn.caleb.descriptor.ClientDescriptor;
import net.islyn.caleb.descriptor.FileTransferDescriptor;
import net.islyn.caleb.logger.ErrorLogger;
import net.islyn.caleb.logger.ErrorLoggerService;
import net.islyn.caleb.logger.ErrorQueue;
import net.islyn.caleb.logger.ErrorQueueIntf;
import net.islyn.caleb.logger.EventLogger;
import net.islyn.caleb.logger.EventLoggerService;
import net.islyn.caleb.logger.EventQueue;
import net.islyn.caleb.logger.EventQueueIntf;
import net.islyn.caleb.miscellaneous.Constants;
import net.islyn.caleb.miscellaneous.FileTools;
import net.islyn.caleb.miscellaneous.SocketTools;
import net.islyn.caleb.miscellaneous.StringTools;

/**
 * This class creates a clients and attempts to connect to server.
 */
public class ClientJob {

	private ClientDescriptor			cd;
	private FileTransferDescriptor		ft;
	
	private SocketTools					sd;
	private SecretKey					sk;
	private IvParameterSpec				iv;
	
	private int							md;
	
	private ErrorLoggerService 			er;
	private EventLoggerService 			ev;
	
	/**
	 * Constructor.
	 * @param cd
	 * @param ft
	 */
	public ClientJob(
			final ClientDescriptor cd,
			final FileTransferDescriptor ft) throws Exception {
		this.cd = cd;
		this.ft = ft;
		
		// Create error logger.
		ErrorQueueIntf qerr = new ErrorQueue("ErrorQueue");
		ErrorLogger.setMessageQueue(qerr);
		er = new ErrorLoggerService(qerr, "pcalebsend", true, 30, true);
		er.start();
		
		// Create event logger.
		EventQueueIntf qevt = new EventQueue("EventQueue");
		EventLogger.setMessageQueue(qevt);
		ev = new EventLoggerService(qevt, "pcalebsend", true, 30, true);
		ev.start();
		
		// Pause to allow logger threads to start.
		try {
			Thread.sleep(100);
		} catch (InterruptedException ex) {}
	}
	
	/**
	 * Send files to remote host.
	 */
	public final void sendFiles() {
		// Initialie socket tools.
		sd = new SocketTools(cd);
		
		try {
			// Send files to remote host.
			readDirectory(ft.dir);
			
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		
		} finally {
			// Log client terminating.
			EventLogger.info("Client terminating connection to " + sd.getRemoteIP() + ":" + sd.getRemotePort());
			
			// Ensure resources are closed.
			sd.close();
		}
	}
	
	/**
	 * Read directory and transfer files.
	 * @param dir
	 * @throws Exception
	 */
	private final void readDirectory(
			final String dir) throws Exception {
		// Scan directory.
		File d = new File(dir);
		File[] afiles = d.listFiles();

		// Sort objects.
		Arrays.sort(afiles, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				if (f1.isDirectory()) {
					if (!f2.isDirectory()) {
						return -1;
					} else {
						return f1.getName().compareTo(f2.getName());
					}
				} else if (f2.isDirectory()) {
					return 1;
				} else {
					return f1.getName().compareTo(f2.getName());
				}
			}
		});

		// Send objects one by one.
		for (File f : afiles) {
			if (f.isDirectory()) {
				readDirectory(f.getAbsolutePath());
				
			} else {
				// Prepare variables.
				final String id = UUID.randomUUID().toString();
				boolean isSuccess = false;
				
				try {
					// Check connection.
					if (!sd.isConnected()) {
						sd.connect();
						if (!initialize()) {
							throw new Exception("Protocol mismatched.");
						}
					}

					// Attempt to send file to remote host.
					isSuccess = sendRemote(f, id);	

				} catch (SocketException ex) {
					// If socket exception happened, attempt to recover.
					sd.close();

					// Pause before retrying.
					try {
						Thread.sleep(Constants.SOCKET_RECONNECT_WAIT);
					} catch (InterruptedException ex2) {}

					// Reconnect and reinitialize.
					sd.connect();
					if (!initialize()) {
						throw new Exception("Protocol mismatched.");
					}

					// Try send file to remote host again.
					isSuccess = sendRemote(f, id);	
				
				} catch (IOException ex) {
					// Failed to send this file across. Log error and ignore.
					ErrorLogger.log(ex.getMessage(), ex, id);
				
				} finally {
					// Log file report.
					if (!isSuccess) {
						EventLogger.warn("Checksum failed on " + f.getAbsolutePath());
					}
				}
			}
		}
		
		// Send terminate message.
		if (sd.isConnected()) {
			byte[] benc = encrypt(new byte[] {0x04});
			sd.write(sd.getMessageLength(benc.length), benc);
			try {
				Thread.sleep(500);
			} catch (InterruptedException ex) {}
		}
	}
		
	/**
	 * Send file to remote host.
	 * @param f
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private final boolean sendRemote(
			final File f,
			final String id) throws Exception {
		// Prepare variables.
		boolean isSuccess = false;
		FileInputStream fis = null;
		MessageDigest digest = null;
		int p = 0;
		
		// Log this event.
		EventLogger.info("Sending file " + f.getAbsolutePath());
		
		// Send file name and size.
		String sname = f.getAbsolutePath().trim();
		byte[] bfile = StringTools.toByteArray(sname);
		String ssize = Long.toString(f.length());
		byte[] bsize = StringTools.toByteArray(ssize);
		byte[] breqs = new byte[(bfile.length + bsize.length + 3)];
		
		// Get modifier.
		int mod = getModifier(ssize);
		
		breqs[0] = (byte) 0x02;
		p += 1;
		System.arraycopy(bfile, 0, breqs, p, bfile.length);
		p += bfile.length;
		breqs[p] = (byte) 0x1f;
		p += 1;
		System.arraycopy(bsize, 0, breqs, p, bsize.length);
		p += bsize.length;
		breqs[p] = (byte) 0x1f;
		
		byte[] benc = encrypt(breqs);
		sd.write(sd.getMessageLength(benc.length), benc);
		
		// Send object stream.
		try {
			fis = new FileInputStream(f);
			digest = MessageDigest.getInstance(Constants.CIPHER_DIG_ALG);
			byte[] b = new byte[Constants.SOCKET_BUFFER_SIZE];
			int rc = 0;
			
			while ((rc = fis.read(b, 0, b.length)) > 0) {
				byte[] bdat = Arrays.copyOfRange(b, 0, rc);
				digest.update(bdat);
				benc = encrypt(bdat);
				sd.write(sd.getMessageLength(benc.length), benc);
				
				// Read loop acknowledgement.
				byte[] back = sd.read(1, Constants.SOCKET_SUBS_READ_WAIT);
				if (scramble(back, mod)[0] != (byte) 0x06) {
					throw new Exception(id + "Unexpected server response.");
				}
			}
			
			// Read completion acknowledgement.
			byte[] back = sd.read(1, Constants.SOCKET_SUBS_READ_WAIT);
			if (scramble(back, mod)[0] != (byte) 0x03) {
				throw new Exception(id + "Unexpected server response.");
			}
						
			// Get response length.
			byte[] bresp_len = sd.read(4, Constants.SOCKET_SUBS_READ_WAIT);
			int iresp_len = sd.getMessageLength(bresp_len);
			
			// Get response data.
			byte[] bresp_dat = scramble(sd.read(iresp_len, Constants.SOCKET_SUBS_READ_WAIT), md);
			final String cks0 = StringTools.unpackHex(bresp_dat);
			final String cks1 = StringTools.unpackHex(digest.digest());
			
			// Log file successfully sent.
			EventLogger.info("Sent file " + f.getAbsolutePath() + " MD5 " + cks1 + " " + FileTools.getSize(f.length()));
		
			
			// If we get this far, flag success.
			isSuccess = (cks0.equals(cks1));
			
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, id);
		
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception ex) {}
				fis = null;
			}
			
			if (digest != null) {
				digest = null;
			}
		}
		
		// Return success indicator.
		return isSuccess;
	}
	
	/**
	 * Initialize connection with remote host.
	 * @return
	 * @throws Exception
	 */
	private final boolean initialize() throws Exception {
		// Prepare variables.
		boolean isSuccess = false;
		
		// Log this event.
		EventLogger.info("Connected to remote server " + sd.getRemoteIP() + ":" + sd.getRemotePort());
		
		// Get modifier for data scramble.
		md = getModifier(Integer.toString(sd.getLocalPort()));
		
		// Send hello.
		byte[] bhelo = new byte[] {0x07, 0x04, 0x17, 0x76};
		byte[] brand = getIV();
		byte[] breqs = new byte[(bhelo.length + brand.length)];
		System.arraycopy(bhelo, 0, breqs, 0, bhelo.length);
		System.arraycopy(brand, 0, breqs, bhelo.length, brand.length);
		sd.write(sd.getMessageLength(breqs.length), scramble(breqs, md));
		
		// Get response length.
		byte[] bresp_len = sd.read(4, Constants.SOCKET_SUBS_READ_WAIT);
		int iresp_len = sd.getMessageLength(bresp_len);
		
		// Check response data.
		if (iresp_len == 18) {
			byte[] bresp_dat = scramble(sd.read(iresp_len, Constants.SOCKET_SUBS_READ_WAIT), md);
			if ((bresp_dat[0] == (byte) 0x20) && (bresp_dat[1] == (byte) 0x21)) {
				byte[] ek = new byte[16];
				System.arraycopy(bresp_dat, 2, ek, 0, ek.length);
				sk = new SecretKeySpec(ek, Constants.CIPHER_KEY_ALG);
				isSuccess = true;
				
				// Generate thumbprint.
				final String sign = StringTools.unpackHex(MessageDigest
						.getInstance("SHA-1")
						.digest(scramble(ek, md)));
				EventLogger.info("Session thumbprint " + sign);
			}
		}
		
		// Return success indicator.
		return isSuccess;
	}
	
	/**
	 * Encrypt data.
	 * @param b
	 * @return
	 * @throws Exception
	 */
	private final byte[] encrypt(
			final byte[] b) throws Exception {
		Cipher cipher = Cipher.getInstance(Constants.CIPHER_ENC_ALG);
		cipher.init(Cipher.ENCRYPT_MODE, sk, iv);
		return cipher.doFinal(b);
	}
	
	/**
	 * Scramble data.
	 * @param b
	 * @param mod
	 * @return
	 * @throws Exception
	 */
	private final byte[] scramble(
			byte[] b,
			int mod) throws Exception {
		byte[] b_ = new byte[b.length];
		
		for (int i = 0; i < b_.length; i++) {
			int x = b[i] & 0xff;
			int y = x ^ mod;
			b_[i] = (byte) (y & 0xff);
		}
		
		return b_;
	}
	
	/**
	 * Get initialization vector.
	 * @return
	 * @throws Exception
	 */
	private final byte[] getIV() throws Exception {
		// Prepare variables.
		byte[] bv = new byte[16];
		
		// Generate random IV.
		Random r = new Random(System.currentTimeMillis());
		r.nextBytes(bv);
		iv = new IvParameterSpec(bv);
		
		return bv;
	}
	
	/**
	 * Get scramble modifier.
	 * @param num
	 * @return
	 * @throws Exception
	 */
	private final int getModifier(
			final String num) throws Exception {
		// Get number.
		String lastDigit = num.substring((num.length() - 1)).trim();
		int iDigit = Integer.parseInt(lastDigit);
		return (iDigit > 0) ? iDigit : 9;
	}
	
	/**
	 * Shutdown this thread.
	 */
	public final synchronized void shutdown() {
		// Shutdown logger threads.
		if (ev != null) ev.shutdown();
		if (er != null) er.shutdown();
	}
		
}
