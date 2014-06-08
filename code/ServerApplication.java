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
    private static FileWriter _fout;

    private static ServerSocket _transferService;

    public static void main(String [ ] args) {
        
        if( args.length == 0 ) {
            _printHelp();
            return;
        } else {
            _parseArguments(args);

            try {
                _fout = _setupOutputFile(_filenameToWrite);
            } catch(IOException ioe) {
                System.out.println(ioe);
                return; // Abort
            }

            _beginTransfer(_emulatorHostname, _sendPort, _receivePort, _fout);
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
     * @param fout The FileWriter to write the received data to
     */
    private static void _beginTransfer(String emulatorHostname, int sendPort, int receivePort, FileWriter fout) {
                    
        // setup the server socket for receiving the file & sending ACKs
        DatagramSocket receiveSocket;
        DatagramSocket sendSocket;
        try {
            receiveSocket = new DatagramSocket(receivePort);
            sendSocket = new DatagramSocket();
        } catch(SocketException se) {
            System.out.println(se);
            return;
        }

        InetAddress sendAddress;
        try {
            sendAddress = InetAddress.getByName(emulatorHostname);
        } catch(UnknownHostException uhe) {
            System.out.println(uhe);
            return;
        }


        boolean transferingFileInProgress = true;

        System.out.println("Waiting for file transfer");
        while(transferingFileInProgress) {

            System.out.println("Accepting Packet");
            packet receivedPacket = _acceptPacket(receiveSocket);

            System.out.println("Received Packet");
            receivedPacket.printContents();
            
            // Write the chunk out to file (TODO: only do this when we have data)
            System.out.println("Writing Packet to File");
            _writePacketToFile(receivedPacket, fout);

            // Build the ACK message
            System.out.println("Creating ACK Packet");
            packet ackPacket = _createAckPacket(receivedPacket);

            System.out.println("Sending ACK Packet");
            _sendAck(sendAddress, sendPort, ackPacket);
            
            // TODO: Detect end of transfer
        }
    }

    private static void _writePacketToFile(packet p, FileWriter fout) {
        String payloadMessage = p.getData();
        try {
            fout.write(payloadMessage, 0, payloadMessage.length());   
            fout.flush();
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
        System.out.println("Wrote payload to file.");
    }

    private static FileWriter _setupOutputFile(String filename) throws IOException {
        File receivedFile = new File(filename);
        
        if( receivedFile.exists() ) {
            receivedFile.delete();
        } else {
            receivedFile.createNewFile();
        }

        return new FileWriter(receivedFile);
    }

    private static packet _acceptPacket(DatagramSocket receiveSocket) {
        byte[] receivedData = new byte[1024];
        DatagramPacket receiveUDPPacket = new DatagramPacket(receivedData, receivedData.length);

        try {
            receiveSocket.receive(receiveUDPPacket);
            receivedData = receiveUDPPacket.getData();
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        System.out.println("Received Data: '"+receivedData+"'");

        packet receivedPacket = null;
        try {
            receivedPacket = PacketHelper.deserialize(receivedData);
        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy Error Handling
        }

        return receivedPacket;
    }

    private static packet _createAckPacket(packet receivedDataPacket) {
        int nextExpectedPacket = receivedDataPacket.getSeqNum() + 1;
        return new packet(PacketHelper.PacketType.ACK.getValue(), nextExpectedPacket, 0, null);
    }

    private static void _sendAck(InetAddress sendAddress, int sendPort, packet ackPacket) {
        
        byte[] ackData = null;
        try {
            ackData = PacketHelper.serialize(ackPacket);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        DatagramPacket ackUDPPacket = new DatagramPacket(ackData, ackData.length, sendAddress, sendPort);
        try {
            DatagramSocket sendSocket = new DatagramSocket();
            sendSocket.send(ackUDPPacket);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }
}
