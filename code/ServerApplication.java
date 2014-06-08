// Author: Alex Lapinski
// Date: June 7th, 2014

import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Arrays;

public class ServerApplication {

    private static String _emulatorHostname;
    private static int _sendPort;
    private static int _receivePort;
    private static String _filenameToWrite;

    private static ServerSocket _transferService;

    public static void main(String [ ] args) {
        
        if( args.length == 0 ) {
            _printHelp();
            return;
        } else {
            _parseArguments(args);
            _beginTransfer(_emulatorHostname, _sendPort, _receivePort, _filenameToWrite);
        }
        
        System.out.println("File transfer complete, Shutting down the server");
    }

    /**
     * @brief Print the CLI Usage information
     * @details Print the simple CLI Usage information to std out
     */
    private static void _printHelp() {
        System.out.println("Use the following format to specify commands: <emulator_hostname> <send_port> <receive_port> <filename>");
    }

    /**
     * @brief Parse the CLI arguments
     * @details Parse the arguments, just assume position in the args array
     * 
     * @param args CLI Arguments
     */
    private static void _parseArguments(String[] args) {
        _emulatorHostname = args[0];
        _sendPort = Integer.parseInt(args[1], 10);
        _receivePort = Integer.parseInt(args[2], 10);
        _filenameToWrite = args[3];

        System.out.println("Arguments Supplied");
        System.out.println("     Emulator Hostname: " + _emulatorHostname);
        System.out.println("     Send to Emulator Port: " + _sendPort);
        System.out.println("     Receive from Emulator Port: " + _receivePort);
        System.out.println("     Filename To Write: " + _filenameToWrite);
    }

    /**
     * @brief Listen for the client to connect over the transfer port and handle transfer of file
     * @details Handle the transfer phase of the server
     * 
     * @param emulatorHostname The hostname of the network emulator to connect to
     * @param sendPort UDP port to use for sending acks to the emulator
     * @param receivePort UDP port to use for receiving data from the emulator
     * @param filename The filename to write the received data to
     */
    private static void _beginTransfer(String emulatorHostname, int sendPort, int receivePort, String filename) {
    
        // setup the file writer
        File receivedFile = new File(filename);
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
                    
        // setup the server socket for receiving the file & sending ACKs
        DatagramSocket receiveSocket;
        DatagramSocket sendSocket;
        try {
            receiveSocket = new DatagramSocket(receivePort);
            sendSocket = new DatagramSocket(sendPort);
        } catch(SocketException se) {
            System.out.println(se);
            return;
        }
        boolean transferingFileInProgress = true;

        System.out.println("Waiting for file transfer");
        while(transferingFileInProgress) {

            // Setup the Receive Datagram
            byte[] receivedData = new byte[PacketHelper.PACKET_MAX_CHARACTERS];
            DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);

            try {
                receiveSocket.receive(receivePacket);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            // Get the transfer message and unbox it
            packet receivedMessage = null;
            try {
                receivedMessage = PacketHelper.deserialize(receivedData);
                receivedMessage.printContents();
            } catch(IOException ioe) {
                System.out.println(ioe); // Lazy Error Handling
            }
            
            // Write the chunk out to file
            String payloadMessage = receivedMessage.getData();
            try {
                fout.write(payloadMessage, 0, payloadMessage.length());   
                fout.flush();
            } catch(IOException ioe) {
                System.out.println(ioe);
            }
            System.out.println("Wrote payload to file.");

            // Build the ACK datagram
            InetAddress clientAddress = receivePacket.getAddress();
            byte[] ackData = null;
            packet ackPacket = new packet(PacketHelper.PacketType.ACK.getValue(), -1/*TODO: Get from received packet*/, 0, null);

            try {
                ackData = PacketHelper.serialize(ackPacket);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            DatagramPacket ackUDPPacket = new DatagramPacket(ackData, ackData.length, clientAddress, sendPort);
                       
            // Send the ACK & notifiy user on CLI
            System.out.println("Sending ACK");
            try {
                sendSocket.send(ackUDPPacket);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            // Detect end of transfer
            //if( receivedMessage.getIsLastMessage() ) {
              //  try {
                //    fout.flush();
                  //  fout.close();
                //} catch(IOException ioe) {
                  //  System.out.println(ioe);
                //}
                //transferingFileInProgress = false;
                //System.out.println("Received EOF message.");
            //}
        }
    }
}
