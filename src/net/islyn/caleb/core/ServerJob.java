package net.islyn.caleb.core;

import java.io.File;
import java.io.FileOutputStream;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.islyn.caleb.descriptor.FileTransferDescriptor;
import net.islyn.caleb.logger.ErrorLogger;
import net.islyn.caleb.logger.EventLogger;
import net.islyn.caleb.miscellaneous.Constants;
import net.islyn.caleb.miscellaneous.FileTools;
import net.islyn.caleb.miscellaneous.SocketTools;
import net.islyn.caleb.miscellaneous.StringTools;

/**
 * Server job does the actual client processing.
 */
public class ServerJob extends Thread {

	private ListenerJobIntf				ls;
	private int							id;
	private SocketTools					sd;
	private FileTransferDescriptor		ft;
	
	private int							md;
	
	private String						uuid;
	private volatile boolean			isEnd;

	/**
	 * Constructor.
	 * @param ss
	 * @param ft
	 * @param qi
	 */
	public ServerJob(
			final ListenerJobIntf ls,
			final int id,
			final SocketTools sd,
			final FileTransferDescriptor ft) {
		this.ls = ls;
		this.id = id;
		this.sd = sd;
		this.ft = ft;
	}
	
	public void run() {
		// Set thread priority.
		try {
			setPriority(Constants.THREAD_PRIORITY_SOCKET);	
		} catch (Exception ex) {
			ErrorLogger.log(ex.getMessage(), ex, null);
		}
		
		// Generate UUID.
		uuid = UUID.randomUUID().toString();
		
		// Log client processing.
		EventLogger.info(uuid + " Client connected from " + sd.getRemoteIP() + ":" + sd.getRemotePort() +
				" on " + sd.getLocalIP() + ":" + sd.getLocalPort());
		
		try {
			processClient();
			
		} catch (SocketTimeoutException ex) {
			// Ignore this error.
		
		} catch (Exception ex) {
			ErrorLogger.log(uuid + " " + ex.getMessage(), ex, null);
		
		} finally {
			if (sd != null) {
				// Log client terminating.
				EventLogger.info(uuid + " Client disconnected from " + sd.getRemoteIP() + ":" + sd.getRemotePort());
				
				// Close socket.
				try {
					sd.close();
				} catch (Exception ex) {}
				sd = null;
			}
		}
		
		// Notify parent thread shutting down.
		ls.shutdownCallback(id);
	}
	
	/**
	 * Process client request.
	 * @throws Exception
	 */
	private final boolean processClient() throws Exception {
		// Prepare variables.
		boolean isValidClient = false;
		md = getModifier(Integer.toString(sd.getRemotePort()));
		
		// Get request length.
		byte[] breqs_len = sd.read(4, Constants.SOCKET_SUBS_READ_WAIT);
		int ireqs_len = sd.getMessageLength(breqs_len);
		
		// Get request data.
		int p = 0;
		byte[] breqs_dat = scramble(sd.read(ireqs_len, Constants.SOCKET_SUBS_READ_WAIT), md);
		
		byte[] bhelo = new byte[4];
		System.arraycopy(breqs_dat, p, bhelo, 0, bhelo.length);
		p += bhelo.length;
		
		if ((bhelo[0] == (byte) 0x07) &&
				(bhelo[1] == (byte) 0x04) &&
				(bhelo[2] == (byte) 0x17) &&
				(bhelo[3] == (byte) 0x76)) {
			// Valid hello. Get IV.
			byte[] brand = new byte[16];
			System.arraycopy(breqs_dat, p, brand, 0, brand.length);
			p += brand.length;
			IvParameterSpec iv = new IvParameterSpec(brand);
			
			// Generate random cipher key.
			byte[] bekey = getKey(brand);
			SecretKey sk = new SecretKeySpec(bekey, Constants.CIPHER_KEY_ALG);
			
			// Response client.
			byte[] bokay = new byte[] {0x20, 0x21};
			byte[] bresp = new byte[(bokay.length + bekey.length)];
			System.arraycopy(bokay, 0, bresp, 0, bokay.length);
			System.arraycopy(bekey, 0, bresp, bokay.length, bekey.length);
			sd.write(sd.getMessageLength(bresp.length), scramble(bresp, md));
			
			// Flag valid client.
			isValidClient = true;
			
			// Generate thumbprint.
			final String sign = StringTools.unpackHex(MessageDigest
					.getInstance("SHA-1")
					.digest(scramble(bekey, md)));
			EventLogger.info(uuid + " Session thumbprint " + sign);
			
			// Process file transfer.
			receiveFiles(sd, sk, iv);
		
		} else {
			throw new Exception("Invalid or unexpected message.");
		}
		
		// Return valid client flag.
		return isValidClient;
	}
	
	/**
	 * Receive files from client.
	 * @param sd
	 * @param sk
	 * @param iv
	 * @throws Exception
	 */
	private final void receiveFiles(
			final SocketTools sd,
			final SecretKey sk,
			final IvParameterSpec iv) throws Exception {		
		// Loop until client terminates.
		while (!isEnd) {
			// Get request length.
			byte[] breqs_len = sd.read(4, Constants.SOCKET_SUBS_READ_WAIT);
			int ireqs_len = sd.getMessageLength(breqs_len);
			
			// Get request data.
			byte[] breqs_dat = decrypt(sd.read(ireqs_len, Constants.SOCKET_SUBS_READ_WAIT), sk, iv);
			
			if (breqs_dat[0] == (byte) 0x04) {
				// Terminate client gracefully.
				isEnd = true;
			
			} else if (breqs_dat[0] == (byte) 0x02) {
				// Prepare variable.
				int p = 0;
				String fnam = null;
				long flen = 0;
			
				// Get file name.
				for (int i = 1; i < breqs_dat.length; i++) {
					if (breqs_dat[i] == (byte) 0x1f) {
						byte[] bfnam = new byte[(i - 1)];
						System.arraycopy(breqs_dat, 1, bfnam, 0, bfnam.length);
						fnam = StringTools.toString(bfnam);
						p = i;
						break;
					}
				}
				
				// Get file size.
				for (int i = (p + 1); i < breqs_dat.length; i++) {
					if (breqs_dat[i] == (byte) 0x1f) {
						byte[] bflen = new byte[(i - p - 1)];
						System.arraycopy(breqs_dat, (p + 1), bflen, 0, bflen.length);
						flen = Long.parseLong(StringTools.toString(bflen));
						p = i;
						break;
					}
				}
				
				// Process file.
				String fnam_ = makeDirectory(fnam);
				receiveFile(fnam_, sd, sk, iv, flen);
				
			} else {
				throw new Exception("Invalid or unexpected message.");
			}	
		}
	}
	
	/**
	 * Receive a file from client.
	 * @param fn
	 * @param sd
	 * @param sk
	 * @param iv
	 * @param len
	 * @throws Exception
	 */
	private final void receiveFile(
			final String fn,
			final SocketTools sd,
			final SecretKey sk,
			final IvParameterSpec iv,
			final long len) throws Exception {
		// Prepare variables.
		FileOutputStream fos = null;
		MessageDigest digest = null;
		long rcv = 0;
		
		// Log file processing.
		EventLogger.info(uuid + " Receiving file " + fn);
		
		try {
			// Initialize.
			fos = new FileOutputStream(fn);
			digest = MessageDigest.getInstance(Constants.CIPHER_DIG_ALG);
			
			// Get modifier.
			int mod = getModifier(Long.toString(len));
			
			// Loop until we receive file in full.
			while (rcv < len) {
				// Get request length.
				byte[] breqs_len = sd.read(4, Constants.SOCKET_SUBS_READ_WAIT);
				int ireqs_len = sd.getMessageLength(breqs_len);
				
				// Get request data.
				byte[] breqs_dat = decrypt(sd.read(ireqs_len, Constants.SOCKET_SUBS_READ_WAIT), sk, iv);
				fos.write(breqs_dat);
				digest.update(breqs_dat);
				
				// Update counter.
				rcv += breqs_dat.length;
				
				// Send completion acknowledgement.
				sd.write(null, scramble(new byte[] {0x06}, mod));
			}
			
			// Client will not send next file until we return completion acknowledgement.
			sd.write(null, scramble(new byte[] {0x03}, mod));
			
			// Response with digest.
			final byte[] bcks = digest.digest();
			sd.write(sd.getMessageLength(bcks.length), scramble(bcks, md));
			
			// Log file successfully received.
			EventLogger.info(uuid + " Received file " + fn + " MD5 " + StringTools.unpackHex(bcks) + " " + FileTools.getSize(len));
		
		} catch (Exception ex) {
			throw ex;
		
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception ex) {}
				fos = null;
			}
			
			if (digest != null) {
				digest = null;
			}
		}
	}
	
	/**
	 * Decipher data.
	 * @param b
	 * @param sk
	 * @param iv
	 * @return
	 * @throws Exception
	 */
	private final byte[] decrypt(
			final byte[] b,
			final SecretKey sk,
			final IvParameterSpec iv) throws Exception {
		Cipher cipher = Cipher.getInstance(Constants.CIPHER_ENC_ALG);
		cipher.init(Cipher.DECRYPT_MODE, sk, iv);
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
	 * Generate random cipher key.
	 * @param iv
	 * @return
	 * @throws Exception
	 */
	private final byte[] getKey(
			final byte[] iv) throws Exception {
		// Prepare variables.
		byte[] ky = new byte[iv.length];
		
		// Generate random IV.
		Random r = new Random(System.currentTimeMillis());
		r.nextBytes(ky);
		
		// Perform XOR against given IV.
		for (int i = 0; i < ky.length; i++) {
			ky[i] = (byte) (ky[i] ^ iv[i]);
		}
		
		return ky;
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
	 * Prepare directory structure.
	 * @param path
	 * @return
	 * @throws Exception
	 */
	private final String makeDirectory(
			final String path) throws Exception {
		String p = new String(path.trim());
		
		int n = p.indexOf(":");
		if (n >= 0) {
			p = p.substring((n + 1));
		}
		
		int x = p.indexOf(File.separator);
		if (x < 0) {
			if (File.separator.equals("\\")) {
				p = p.replace('/', '\\');
			} else if (File.separator.equals("/")) {
				p = p.replace('\\', '/');
			}
		}
		
		if ((p.startsWith("/")) || (p.startsWith("\\"))) {
			p = p.substring(1).trim();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(ft.dir.endsWith(File.pathSeparator) ? ft.dir.substring(0, ft.dir.length() - 1) : ft.dir);
		String[] paths = StringTools.tokenize(p, File.separator);
		for (int i = 0; i < (paths.length - 1); i++) {
			sb.append("/" + paths[i]);
			makeSubDirectory(sb.toString());
		}
		
		return sb.toString() + "/" + paths[(paths.length - 1)];
	}
	
	/**
	 * Create sub directory.
	 * @param path
	 * @throws Exception
	 */
	private final void makeSubDirectory(
			final String path) throws Exception {
		// Define directory.
		File f = new File(path);
		
		// Create directory.
		if (!f.isDirectory()) f.mkdirs();
		
		// Nullify file.
		f = null;
	}
	
	/**
	 * Shutdown this thread.
	 */
	public final synchronized void shutdown() {
		try {
			isEnd = true;
			interrupt();	
		} catch (Exception ex) {}
	}
	
}
