# P2P File Transfer
Secure peer-to-peer file transfer, encrypted and scrambled over TCP/IP, written in core Java without third-party libraries.

## Introduction
Project Caleb was created as a peer-to-peer (‘P2P’) file transfer tool to work around restrictions in my operating environment.

When mainstream file transfer methods such as FTP, HTTP, shared folder, etc. weren’t an option, I resorted to writing my own Java code.

This P2P program transfers an entire directory contents including its sub-directories between a client and server securely using AES encryption. A new key is generated for each new session. Additionally, the server-client commands are also scrambled slightly differently for each new file, making data dump analysis difficult for network sniffers.

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

Note that you must start the Server part before Client.
