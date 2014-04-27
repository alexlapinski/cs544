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

        int transferPort = _negotateTransferPort(negotiationPort);
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
            System.out.println("Listening on port " + negotiationPort + " for the client to request transferPort");
            while( true ) {

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

                    // send port to client
                    negotationOutputStream.writeInt(randomPort);
                    System.out.println("Negotiated use of port '" + randomPort + "' with client for file transfer.");
                    

                    File receivedFile = new File("received.txt");
                    if( receivedFile.exists() ) {
                        receivedFile.delete();
                    } else {
                        receivedFile.createNewFile();
                    }

                    FileWriter fout = new FileWriter(receivedFile);
                    
                    DatagramSocket serverSocket = new DatagramSocket(randomPort);
                    boolean transferingFile = true;

                    while(transferingFile) {
                        byte[] receivedData = new byte[TransferMessage.MESSAGE_SIZE];
                        DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
                        serverSocket.receive(receivePacket);

                        TransferMessage receivedMessage = null;

                        try {
                            receivedMessage = TransferMessage.fromBytes(receivePacket.getData());
                        } catch(Exception e) {
                            System.out.println(e); // Lazy Error Handling
                        }
                        
                        String payloadMessage = receivedMessage.getPayloadAsString();
                        fout.write(payloadMessage, 0, payloadMessage.length());   
                        fout.flush();

                        System.out.println("Received Message Payload: '" + receivedMessage.getPayloadAsString() + "'");
                        System.out.println("Received Message isLastMessage?: '" + receivedMessage.getIsLastMessage() + "'");
                       
                        InetAddress clientAddress = receivePacket.getAddress();
                        int clientPort = receivePacket.getPort();
                        String ackMessage = receivedMessage.getPayloadAsString().toUpperCase();
                        byte[] ackData = ackMessage.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
                       
                        System.out.println("Sending Ack: '" + ackMessage + "'");
                        serverSocket.send(ackPacket);

                        // Detect end of transfer
                        if( receivedMessage.getIsLastMessage() ) {
                            fout.flush();
                            fout.close();
                            transferingFile = false;
                        }
                    }

                    System.out.println("File transfer complete, listening again for new clients");
                }
            }
            
            // Cleanup is handled on the client

        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        return randomPort;
    }
}
