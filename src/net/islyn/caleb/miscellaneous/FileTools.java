package net.islyn.caleb.miscellaneous;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class FileTools {
	
	public static final void writeToFile(
			final String p,
			final byte[] b) throws Exception {
		// If byte array is empty, we may have to create file manually.
		File f = null;
		if ((b == null) || ((b != null) && (b.length == 0))) {
			f = new File(p);
			f.createNewFile();
		}
		
		// Write to output stream.
		FileOutputStream fos = null;
		try {
			if (f == null) fos = new FileOutputStream(p);
			else fos = new FileOutputStream(f);
			fos.write(b);
			fos.close();
			
		} catch (Exception ex) {
			throw ex;
			
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception ex) {}
				fos = null;
			}
			
			if (f != null) {
				f = null;
			}
		}
	}
	
	public static final String getCRC32(
			final String path) throws Exception {
		// Prepare variables.
		FileInputStream fis = null;
		byte[] buff = new byte[8192];
		int rc = -1;
		String crc32 = null;
		
		try {
			Checksum chks = new CRC32();
			fis = new FileInputStream(path);
			while ((rc = fis.read(buff, 0, buff.length)) >= 0) {
				chks.update(buff, 0, rc);
			}
			crc32 = Long.toString(chks.getValue());
			chks = null;
			
		} catch (Exception ex) {
			throw ex;
			
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception ex) {}
				fis = null;
			}
		}
		
		return crc32;
	}
	
	public static final void makeDirectory(
			final String path) throws Exception {
		// Define directory.
		File f = new File(path);
		
		// Create directory.
		if (!f.isDirectory()) f.mkdirs();
		
		// Nullify file.
		f = null;
	}
	
	public static final void renameFile(
			final String path_old,
			final String path_new) throws Exception {
		// Define directory.
		File f_old = new File(path_old);
		File f_new = new File(path_new);
		
		// Copy file.
		f_old.renameTo(f_new);
		
		// Nullify file.
		f_old = null;
		f_new = null;
	}
	
	public static final void deleteDirectory(
			final String path) throws Exception {
		// Define directory.
		File f = new File(path);
		
		if (f.exists()) {
			if (f.isDirectory()) {
				for (File f_ : listDirectory(f)) {
					deleteDirectory(f_.getAbsolutePath());
				}
				f.delete();
				
			} else {
				f.delete();
			}	
		}
	}
	
	public static final void deleteFile(
			final String path) throws Exception {
		// Define directory.
		File f = new File(path);
		
		// Delete file.
		if (f.isFile()) f.delete();
		
		// Nullify file.
		f = null;
	}
	
	public static final String[] listDirectoryAsString(
			final String path) throws Exception {
		File[] files = listDirectory(path);
		String[] paths = new String[files.length];
		
		for (int i = 0; i < paths.length; i++) {
			paths[i] = files[i].getPath();
		}
		
		return paths;
	}
	
	public static final File[] listDirectory(
			final String path) throws Exception {
		// Define directory.
		File f = new File(path);
		
		// Return file list.
		return listDirectory(f);
	}
	
	public static final File[] listDirectory(
			final File f) throws Exception {
		// Return file list.
		return f.listFiles();
	}
	
	public static final File[] listDirectory(
			final String path,
			final String filter) throws Exception {
		// Define directory.
		File f = new File(path);
		
		// Return file list.
		return listDirectory(f, filter);
	}
	
	public static final File[] listDirectory(
			final File f,
			final String filter) throws Exception {
		// Return file list.
		return f.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return (pathname.getName().endsWith(filter));
			}
		});
	}
	
	public static final void updateTextFile(
			final String path, byte[] data, boolean append) throws Exception {
		// Define file.
		File f = new File(path);
		
		// Open file for write.
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f, append));
		bos.write(data);
		bos.flush();
		
		// Close file.
		bos.close();
		bos = null;
		f = null;
	}

	public static final byte[] readFromTextFile(
			final String path) throws Exception {
		// Define file.
		File f = new File(path);
		
		// Check existence.
		if (!f.exists()) return null;
	
		// Read file.
		return readFromTextFile(f);
	}
	
	public static final byte[] readFromTextFile(
			File f) throws Exception {
		// Prepare variables.
		RandomAccessFile ra = null;
		FileChannel in = null;
		ByteBuffer buff = null;
		
		try {
			ra = new RandomAccessFile(f, "r");
			in = ra.getChannel();
			
			buff = ByteBuffer.allocate((int) in.size());
			in.read(buff);
			buff.rewind();
			
		} catch (Exception ex) {
			throw ex;
		
		} finally {
			// Close all streams.
			if (ra != null) {
				try {
					ra.close();	
					ra = null;
				} catch (Exception ex) {}
			}
			if (in != null) {
				try {
					in.close();	
					in = null;
				} catch (Exception ex) {}
			}
			
			// Helps lazy JVM to cleanup.			
			f = null;
		}
		
		return ((buff != null) ? buff.array() : null);
	}
	
	public static final long getLastModified(
			final String path) throws Exception {
		// Define file.
		File f = new File(path);
		// Get timestamp.
		return f.lastModified();
	}
	
	public static final long getFileSize(
			final String path) throws Exception {
		// Define file.
		File f = new File(path);
		// Get size.
		return f.length();
	}
	
	public static final boolean isLatest(
			final String path,
			final long tstm) throws Exception {
		// Define file.
		File f = new File(path);
		// Compare timestamp.
		return (f.lastModified() == tstm);
	}
	
	public static final boolean isDirectoryExists(
			final String path) throws Exception {
		// Define directory.
		File f = new File(path);
		// Verify existence.
		return (f.isDirectory() && (f.exists()));
	}
	
	public static final String[] isFileBeginWithExists(
			final String path) throws Exception {
		// Prepare variables.
		ArrayList<String> aps_ = new ArrayList<String>();
		int n = path.lastIndexOf("/");
		String p = path.substring(0, n);
		String fn = path.substring((n + 1));
		
		// Look up directory.
		File[] afiles = listDirectory(p, ".xml");
		for (File f : afiles) {
			if (f.getName().startsWith(fn)) {
				aps_.add(f.getPath());	
			}
		}
		
		// Prepare response.
		String[] aps = new String[aps_.size()];
		aps_.toArray(aps);
		
		// Return list of paths.
		return aps;
	}
	
	public static final boolean isFileExists(
			final String path) throws Exception {
		// Define file.
		File f = new File(path);
		// Verify existence.
		return (f.isFile() && (f.exists()));
	}
	
	public static final String getSize(long size) {
		// Check if size is less than a KB.
		if (size < 1024) return (Long.toString(size) + " &nbspB");

		// Check if size is less than a MB.
		BigDecimal kb = getCalculatedSize(size);
		if (kb.compareTo(new BigDecimal(1024)) < 0) return (kb.toPlainString() + " KB");

		// Check if size is less than a GB.
		BigDecimal mb = getCalculatedSize(kb);
		if (mb.compareTo(new BigDecimal(1024)) < 0) return (mb.toPlainString() + " MB");

		// Otherwise just quote the size in GB.
		return (getCalculatedSize(mb).toPlainString() + " GB");
	}

	private static final BigDecimal getCalculatedSize(long b) {
		return getCalculatedSize(new BigDecimal(b));
	}

	private static final BigDecimal getCalculatedSize(BigDecimal bd) {
		return bd.divide(new BigDecimal(1024), 2, RoundingMode.HALF_UP);
	}
	
}
