package net.islyn.caleb.main;

import net.islyn.caleb.core.ListenerJob;
import net.islyn.caleb.descriptor.FileTransferDescriptor;
import net.islyn.caleb.descriptor.ServerDescriptor;

public class StartServer {

	/**
	 * Print system information.
	 */
	public static void printSystemInformation() {
		System.out.println("\n\n" +
				"Project Caleb: P2P File Transfer Server.\n");
		
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
		System.out.println("Parameter usage: listen_ip, listen_port, receiving_directory");
		
		try {
			// Sanity check.
			if (args.length < 3) throw new Exception("Insufficient parameters");
			
			// Parse parameters.
			final String listen_ip = args[0];
			final int listen_port = Integer.parseInt(args[1]);
			final String base_dir = args[2];
			
			ServerDescriptor ss = new ServerDescriptor();
			ss.listen_ip = listen_ip;
			ss.listen_port = listen_port;
			ss.accept_timeout = 1000;
			ss.read_timeout = 5000;
			
			FileTransferDescriptor ft = new FileTransferDescriptor();
			ft.dir = base_dir;
			
			ListenerJob server = new ListenerJob(ss, ft);
			server.listen();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
