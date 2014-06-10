// Author: Alex Lapinski
// Date: June 7th, 2014

import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Arrays;

public class server implements PacketReceiver.INotifyPacketArrived {

    private static String _emulatorHostname;
    private static int _sendPort;
    private static int _receivePort;
    private static String _filenameToWrite;

    public static void main(String [ ] args) {
        
        if( args.length == 0 ) {
            _printHelp();
            return;
        } else {
            _parseArguments(args);
            server aServer = new server(_emulatorHostname, _sendPort, _receivePort, _filenameToWrite);
        }
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
    


    private PacketSender _ackSender;
    private DatagramSocket _dataSocket;
    private packet _receivedPacket;
    private SimpleFileWriter _outputWriter;
    private SimpleFileWriter _arrivalLogger;
    private final int MODULUS;
    private boolean _hasEOTArrived;

    public server(String destinationName, int sendPort, int receivePort, String outputFilename) {
        
        try {
            _dataSocket = new DatagramSocket(receivePort);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        _ackSender = new PacketSender(destinationName, sendPort);
        _outputWriter = new SimpleFileWriter(outputFilename);
        _arrivalLogger = new SimpleFileWriter("arrival.log");
        
        int m = 3;
        MODULUS = (int) Math.pow(2, m);

        System.out.println("Listening for Packets");
        while(!_hasEOTArrived) {
            byte[] receivedData = new byte[1024];
            DatagramPacket receivedUDPPacket = new DatagramPacket(receivedData, receivedData.length);
            packet receivedPacket = null;

            try {
                _dataSocket.receive(receivedUDPPacket);
                receivedPacket = PacketHelper.deserialize(receivedData);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            notifyPacketArrived(receivedPacket);
        }
    }

    public void notifyPacketArrived(packet p) {

        _arrivalLogger.appendToFile(p.getSeqNum() + "\n");
        System.out.println("Packet with Sequence Number '"+p.getSeqNum()+"' arrived.");

        if( p.getType() == PacketHelper.PacketType.DATA.getValue() ) {
            _outputWriter.appendToFile(p.getData());

            int nextSequenceNumberExpected = (p.getSeqNum() + 1) % MODULUS;

            _ackSender.sendPacket(new packet(PacketHelper.PacketType.ACK.getValue(), nextSequenceNumberExpected, 0, null));

        }
        else if( p.getType() == PacketHelper.PacketType.ClientToServerEOT.getValue() ) {
            _hasEOTArrived = true;
            _outputWriter.closeFile();

            _ackSender.sendPacket(new packet(PacketHelper.PacketType.ServerToClientEOT.getValue(), 0, 0, null));

            System.out.println("EOT received from client, sending EOT");
            System.out.println("File transfer successful, exiting.");
        }
    }

}
