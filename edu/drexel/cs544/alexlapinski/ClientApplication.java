package edu.drexel.cs544.alexlapinski;

import java.lang.Integer;
import java.net.*;
import java.io.*;

public class ClientApplication {

    private static String _serverAddress;
    private static int _fixedPort;
    private static String _filenameToTransfer;
    private static int _transferPort;
    private static String _fileContents;

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
            _transferPort = _negotiateTransferPort(_serverAddress, _fixedPort);
            _fileContents = _readContentsOfFile(_filenameToTransfer);
            System.out.println("Read File");
            
            _transferDataToServer(_serverAddress, _transferPort, _fileContents);
        }
    }

    /**
     * @brief Print the CLI Usage information
     * @details Print the simple CLI Usage information to std out
     */
    private static void _printHelp() {
        System.out.println("Use the following format to specify commands: <server_address> <f_port> <filename>");
    }

    /**
     * @brief Parse the CLI arguments
     * @details Parse the arguments, just assume position in the args array
     * 
     * @param args CLI Arguments
     */
    private static void _parseArguments(String[] args) {
        _serverAddress = args[0];
        _fixedPort = Integer.parseInt(args[1], 10);
        _filenameToTransfer = args[2];

        System.out.println("Arguments Supplied");
        System.out.println("     ServerAddress: " + _serverAddress);
        System.out.println("     FixedPort: " + _fixedPort);
        System.out.println("     FilenameToTransfer: " + _filenameToTransfer);
    }

    /**
     * @brief negotiate the random port to use for transfer with the server
     * @details Set up the socket, input/output data streams for communication, communicates with the server and receives the random port.
     * @return port to use for file transfer as determined by the server
     */
    private static int _negotiateTransferPort(String serverAddress, int serverPort) {
        final int NEGOTIATION_MESSAGE = 42;

        Socket negotiationSocket;
        DataOutputStream negotiationOutputStream;
        DataInputStream negotiationInputStream;
        int transferPort = -1;

        // Setup our socket and in/out streams for port negotiation
        try {
            negotiationSocket = new Socket(serverAddress, serverPort);
            negotiationOutputStream = new DataOutputStream(negotiationSocket.getOutputStream());
            negotiationInputStream = new DataInputStream(negotiationSocket.getInputStream());
        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy handle the error
            return -1;
        }

        // Ensure we have a valid socket
        if( negotiationSocket == null ) {
            System.out.println("Negotiation Service did not setup correctly");
            System.exit(-1);
        }

        // Ensure we have a valid input stream
        if( negotiationInputStream == null ) {
            System.out.println("Negotiation Service Input Stream did not setup correctly");
            System.exit(-1);
        }

        // Ensure we have a valid output stream
        if( negotiationOutputStream == null ) {
            System.out.println("Negotiation Service Output Stream did not setup correctly");
            System.exit(-1);
        }

        try {

            // Send our Int to indicate to the server that we want a random port for file transfer
            negotiationOutputStream.writeInt(NEGOTIATION_MESSAGE);

            // infinite loop to ensure we read the server response
            while( true ) {
                
                int message;

                try {
                    // read the message from the server, we expect an int and this int to be the random port to be used for file transfer
                    message = negotiationInputStream.readInt();
                } catch(EOFException eofe) {
                    break; // End of stream, exit infinite loop
                } catch(IOException ioe) {
                    throw ioe; // throw it to the outer catch handler
                }

                transferPort = message; // assume message received is a valid port
                System.out.println("Received the port value '" + transferPort + "' to use for file transfer");

                // we got the port, so exit this listening stream
                break;
            }

            // cleanup our socket and in/out stream
            negotiationOutputStream.close();
            negotiationInputStream.close();
            negotiationSocket.close();

        } catch(UnknownHostException uhe) {
            System.out.println(uhe); // Lazy error handling
        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy error handling
        }

        return transferPort; // 
    }

    /**
     * @brief Read the contents of the specified file and return as a string
     * @details Read in the contents of a text file and return it as a string
     * 
     * @param filename Filename of a plain text ASCII file
     * @return contents of the file as a String, null if an error has occured
     */
    private static String _readContentsOfFile(String filename) {

        String fileContents = null;

        // Check validity of filename
        if( filename == null || filename.length() == 0 ) {
            return fileContents;
        }

        // setup reader
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(filename);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        if( fileReader == null ) {
            return fileContents;
        }

        BufferedReader bufferedReader = new BufferedReader(fileReader);

        // read the file
        String line = null;
        try {
            while( (line = bufferedReader.readLine()) != null ) {
                fileContents += line;
            }

            bufferedReader.close();
        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy Error Handling
        }

        return fileContents;
    } 

    /**
     * @brief Transfer String Data to the server specified at the port specified
     * @details Transfers String Data to the server via UDP at the port specified, if data is null, the transfer does not occur
     * 
     * @param serverAddress The server address
     * @param transferPort The transfer port to use
     * @param data The data to transfer, must be non-null
     */
    private static void _transferDataToServer(String serverAddress, int transferPort, String data) {
        DatagramSocket clientSocket = null;
        InetAddress localAddress = null;
        
        try {
            clientSocket = new DatagramSocket();
        } catch(SocketException se) {
            System.out.println(se);
            return;
        }

        try {
            localAddress = InetAddress.getByName(serverAddress);
        } catch(UnknownHostException uhe) {
            System.out.println(uhe);
            return;
        }

        byte[] sendData = new byte[1024];
        sendData = data.getBytes();

        // TODO: Chunking the file and sending packet by packet, also reciveing ACK from server

        DatagramPacket dataPacket = new DatagramPacket(sendData, sendData.length, localAddress, transferPort);
    
        try {
            System.out.println("Sending File");
            clientSocket.send(dataPacket);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }
}
