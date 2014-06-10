// Author: Alex Lapinski
// Date: June 7th, 2014

import java.lang.Integer;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.*;
import java.io.*;

public class client implements PacketReceiver.INotifyPacketArrived {

    private static String _emulatorHostname;
    private static int _sendPort;
    private static int _receivePort;
    private static String _filenameToTransfer;

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
            client instance = new client(_emulatorHostname, _sendPort, _receivePort, _filenameToTransfer);
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
        _filenameToTransfer = args[3];

        System.out.println("Arguments Supplied");
        System.out.println("     Emulator Hostname: " + _emulatorHostname);
        System.out.println("     Send to Emulator Port: " + _sendPort);
        System.out.println("     Receive from Emulator Port: " + _receivePort);
        System.out.println("     Filename To Transfer: " + _filenameToTransfer);
    }




    private PacketSender _dataSender;
    private FileChunker _fileChunker;
    private GoBackNProtocol _gbnProtocol;
    private DatagramSocket _receiveSocket;

    private SimpleFileWriter _seqNumLogger;
    private SimpleFileWriter _ackLogger;

    public client(String destinationHostname, int sendPort, int receivePort, String filenameToTransfer) {
        final int M_VALUE = 3;
        final int MAX_CHARS_PER_PACKET = 30;

        _seqNumLogger = new SimpleFileWriter("seqnum.log");
        _ackLogger = new SimpleFileWriter("ack.log");

        _fileChunker = new FileChunker(_filenameToTransfer, MAX_CHARS_PER_PACKET);
        _dataSender = new PacketSender(_emulatorHostname, _sendPort);

        _gbnProtocol = new GoBackNProtocol(M_VALUE, _fileChunker, _dataSender, _seqNumLogger);

        try {
            _receiveSocket = new DatagramSocket(receivePort);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        (new Thread(new PacketReceiver(_receiveSocket, this))).start();

        while( _fileChunker.hasMoreChunks() ) {
            _gbnProtocol.sendPacket();
        }

        _gbnProtocol.sendEOTPacket();
    }

    public void dispose() {
        _ackLogger.closeFile();
        _seqNumLogger.closeFile();
    }

    public void notifyPacketArrived(packet p) {

        if( p.getType() == PacketHelper.PacketType.ACK.getValue() ) {
            _ackLogger.appendToFile(p.getSeqNum() + "\n");
            _gbnProtocol.notifyAckArrived(p);
            (new Thread(new PacketReceiver(_receiveSocket, this))).start();
        } else if( p.getType() == PacketHelper.PacketType.ServerToClientEOT.getValue() ) {
            System.out.println("Recevived EOT from server; exiting");
        } else {
            System.out.println("Unexpected Response Packet of Type: " + p.getType());
        }
    }
}
