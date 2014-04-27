package edu.drexel.cs544.alexlapinski;

import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Arrays;

public class ServerApplication {

    private static final int FIXED_PORT = 6000; 
    private static final int NEGOTIATION_MESSAGE = 42;

    private static ServerSocket _transferService;

    public static void main(String [ ] args) {
        
        // Due to similataneous testing on tux, we need to allow for specifying a port at runtime, 
        // the default (6000) is used if not specified

        int negotiationPort = FIXED_PORT;

        if( args.length >= 1 ) {
            negotiationPort = Integer.parseInt(args[0], 10);
        }

        System.out.println("Listening on port " + negotiationPort + " for the client to request transferPort");
        int transferPort = _negotateTransferPort(negotiationPort);

        System.out.println("File transfer complete, Shutting down the server");
        _beginTransferPhase(transferPort);
    }

    /**
     * @brief Generate a random port
     * @details Generate a random integer bounded to valid ports
     * @return Random Port (as Int)
     */
    private static int _generateRandomPort() {
        final int MAX_PORT_VALUE = 65535;
        final int MIN_PORT_VALUE = 1024;

        Random randomInt = new Random();

        int port = randomInt.nextInt(MAX_PORT_VALUE - MIN_PORT_VALUE) + MIN_PORT_VALUE;

        return port;
    }


    /**
     * @brief Validate the message sent from the client
     * @details Simple check against expected message value
     * 
     * @param message Message to check
     * @return true if message is valid
     */
    private static boolean _isNegotiationMessage(int message) {
        return message == NEGOTIATION_MESSAGE;
    }

    /**
     * @brief listen for the client and negotiate random transfer port
     * @details Setup Socket communication and wait for client message, once negotiation message is received, generate a random port and send it to the client.
     * Return that random port for internal usage.
     * 
     * @param negotiationPort Pre-determined port to use for neogotiation of random port with the client
     * @return Random port to be used with file transfer or -1 if negotiation failed
     */
    private static int _negotateTransferPort(int negotiationPort) {

        ServerSocket negotiationService;
        Socket clientNegotiationSocket;
        DataInputStream negotationInputStream;
        DataOutputStream negotationOutputStream;
        int randomPort = -1;

        try {
            // create our service
            negotiationService = new ServerSocket(negotiationPort, 0, null);

            // Endless loop to use for communication with the client
            boolean negotiationInProgress = true;
            while( negotiationInProgress ) {

                // setup the client socket and communication streams
                clientNegotiationSocket = negotiationService.accept();
                negotationInputStream = new DataInputStream(clientNegotiationSocket.getInputStream());
                negotationOutputStream = new DataOutputStream(clientNegotiationSocket.getOutputStream());
            
                int message;

                // read an int from the input stream
                try {
                    message = negotationInputStream.readInt();
                } catch(EOFException eofe) {
                    break; // Reached end of file, exit out of endless loop
                } catch(IOException ioe) {
                    throw ioe;
                }

                // first check to see if the int is the expected value
                if( _isNegotiationMessage(message) )  {
                    
                    // generate random port number > 1024
                    randomPort = _generateRandomPort();

                    // send transfer port to client
                    negotationOutputStream.writeInt(randomPort);
                    System.out.println("Negotiated use of port '" + randomPort + "' with client for file transfer.");

                    // Cleanup Negotiation Phase
                    negotationOutputStream.close();
                    negotiationInProgress = false;
                }
            }
            
            // Cleanup of sockets is handled on the client end

        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        return randomPort; // todo, clean this up, it isn't really needed to return the random port since we just run the server once, 
        // its not a true server, as it is not a long-running process
    }

    /**
     * @brief Listen for the client to connect over the transfer port and handle transfer of file
     * @details Handle the transfer phase of the server
     * 
     * @param transferPort port to use for UDP socket transfer
     */
    private static void _beginTransferPhase(int transferPort) {
    
        // setup the file writer
        File receivedFile = new File("received.txt");
        if( receivedFile.exists() ) {
            receivedFile.delete();
        } else {
            try {
                receivedFile.createNewFile();
            } catch(IOException ioe) {
                System.out.println(ioe);
            }
        }
        FileWriter fout;

        try {
            fout = new FileWriter(receivedFile);
        } catch(IOException ioe) {
            System.out.println(ioe);
            return;
        }
                    
        // setup the server socket for receiving the file
        DatagramSocket serverSocket;
        try {
            serverSocket = new DatagramSocket(transferPort);
        } catch(SocketException se) {
            System.out.println(se);
            return;
        }
        boolean transferingFileInProgress = true;

        System.out.println("Waiting for file transfer");
        while(transferingFileInProgress) {

            // Setup the Receive Datagram
            byte[] receivedData = new byte[TransferMessage.MESSAGE_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);

            try {
                serverSocket.receive(receivePacket);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            // Get the transfer message and unbox it
            TransferMessage receivedMessage = null;
            try {
                receivedMessage = TransferMessage.fromBytes(receivePacket.getData());
                System.out.println("Received message: { isLastMessage: '" + receivedMessage.getIsLastMessage() + "', payload: '" +receivedMessage.getPayloadAsString() +"'}"); 
            } catch(Exception e) {
                System.out.println(e); // Lazy Error Handling
            }
            
            // Write the chunk out to file
            String payloadMessage = receivedMessage.getPayloadAsString();
            try {
                fout.write(payloadMessage, 0, payloadMessage.length());   
                fout.flush();
            } catch(IOException ioe) {
                System.out.println(ioe);
            }
            System.out.println("Wrote payload to file.");

            // Build the ACK datagram
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            String ackMessage = receivedMessage.getPayloadAsString().toUpperCase();
            byte[] ackData = ackMessage.getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
                       
            // Send the ACK & notifiy user on CLI
            System.out.println("Sending ACK");
            try {
                serverSocket.send(ackPacket);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            // Detect end of transfer
            if( receivedMessage.getIsLastMessage() ) {
                try {
                    fout.flush();
                    fout.close();
                } catch(IOException ioe) {
                    System.out.println(ioe);
                }
                transferingFileInProgress = false;
                System.out.println("Received EOF message.");
            }
        }
    }
}
