# P2P File Transfer (Project Caleb)
Secure client-server file transfer an entire folder encrypted and scrambled over TCP/IP, written in core Java without third-party library.

## Introduction
Project Caleb was created as a peer-to-peer (‘P2P’) file transfer tool to work around restrictions in my operating environment.

When mainstream file transfer methods such as FTP, HTTP, shared folder, etc. weren’t an option, I resorted to writing my own Java code.

This P2P program transfers an entire directory contents including its sub-directories between a client and server securely using AES encryption. A new key is generated for each new session. Additionally, the server-client commands are also scrambled slightly differently for each new file, making data dump analysis difficult for network sniffers. To ensure data integrity, MD5 checksums are verified for each file transfer.

Project Caleb was built upon three personal objectives:
1. The performance must be reasonably fast and stable to transfer over 50GB of data
2. The transfer stream must be secure and not easily be recognized by monitoring tools
3. The supported files system must be compatible for use between Windows and Mac.

I hope this tool benefits your project and/or personal use as it did for me.

## Usage in Code
To create the Server part in Java:
```
// Create server descriptor
ServerDescriptor ss = new ServerDescriptor();
ss.listen_ip = "127.0.0.1";
ss.listen_port = 21001;
ss.accept_timeout = 1000;
ss.read_timeout = 5000;

// Create receive descriptor
FileTransferDescriptor ft = new FileTransferDescriptor();
ft.dir = "/users/islyn/recv";

// Run the server
ListenerJob server = new ListenerJob(ss, ft);
server.listen();
```

To create the Client part in Java:
```
// Create client descriptor
ClientDescriptor cd = new ClientDescriptor();
cd.remote_ip = "127.0.0.1";
cd.remote_port = 21001;
cd.connect_timeout = 5000;
cd.read_timeout = 5000;

// Create send descriptor
FileTransferDescriptor ft = new FileTransferDescriptor();
ft.dir = "/users/islyn/send";

// Start file transfer
ClientJob cj = new ClientJob(cd, ft);
cj.sendFiles();

// Terminate file transfer
cj.shutdown();
```

## Usage in Command Line
For Server:
```
java -cp pcaleb.jar net.islyn.caleb.main.StartServer [listen_ip] [listen_port] [receiving_directory]
```

For Client:
```
java -cp pcaleb.jar net.islyn.caleb.main.StartClient [listen_ip] [listen_port] [sending_directory]
```

Note that you must start the Server component before Client.

## Audit Logs
Both Client and Server components will generate errors and events log in directories named "pcalebsendlogs" and "pcalebrecvlogs" respectively. The file name formats are ER_yyyyMMdd.txt and EV_yyyyMMdd.txt. Whereas the error log will only be populated should an error is thrown from the program, the events log will record connectivity and file transfer details.
