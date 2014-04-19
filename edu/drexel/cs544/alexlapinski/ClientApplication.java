package edu.drexel.cs544.alexlapinski;

import java.lang.Integer;
import java.net.*;
import java.io.*;

public class ClientApplication {

    private static String _serverAddress;
    private static int _fixedPort;
    private static String _filenameToTransfer;
    private static int _transferPort;

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
            _transferPort = _negotiateTransferPort(_serverAddress, _fixedPort);
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
}
