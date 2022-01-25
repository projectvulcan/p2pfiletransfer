package net.islyn.caleb.main;

import net.islyn.caleb.core.ClientJob;
import net.islyn.caleb.descriptor.ClientDescriptor;
import net.islyn.caleb.descriptor.FileTransferDescriptor;

public class StartClient {

	/**
	 * Print system information.
	 */
	public static void printSystemInformation() {
		System.out.println("\n\n" +
				"Project Caleb: P2P File Transfer Client.\n");
		
		System.out.println("Operating system: " + System.getProperty("os.name") + " " +
				System.getProperty("os.version"));
		System.out.println("Java version: " + System.getProperty("java.vendor") + " " +
				System.getProperty("java.vm.version") + " " +
				System.getProperty("java.version"));
		System.out.println("Character Encoding: " + System.getProperty("file.encoding") + " " +
				System.getProperty("file.encoding.pkg") + "\n");
	}
	
	public static void main(String[] args) {
		// Print system information.
		printSystemInformation();
		
		// Print usage information.
		System.out.println("Parameter usage: remote_ip, remote_port, sending_directory");
		
		try {
			// Sanity check.
			if (args.length < 3) throw new Exception("Insufficient parameters");
			
			// Parse parameters.
			final String remote_ip = args[0];
			final int remote_port = Integer.parseInt(args[1]);
			final String base_dir = args[2];
			
			ClientDescriptor cd = new ClientDescriptor();
			cd.remote_ip = remote_ip;
			cd.remote_port = remote_port;
			cd.connect_timeout = 5000;
			cd.read_timeout = 5000;
			
			FileTransferDescriptor ft = new FileTransferDescriptor();
			ft.dir = base_dir;
			
			ClientJob cj = new ClientJob(cd, ft);
			cj.sendFiles();
			cj.shutdown();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
