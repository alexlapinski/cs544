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
    private PacketReceiver _dataReceiver;
    private packet _receivedPacket;
    private SimpleFileWriter _outputWriter;
    private final int MODULUS;

    public server(String destinationName, int sendPort, int receivePort, String outputFilename) {
        _ackSender = new PacketSender(destinationName, sendPort);
        _dataReceiver = new PacketReceiver(receivePort, this);
        _outputWriter = new SimpleFileWriter(outputFilename);
        
        int m = 3;
        MODULUS = (int) Math.pow(2, m);

        _dataReceiver.run();
    }

    public void notifyPacketArrived(packet p) {

        if( p.getType() == PacketHelper.PacketType.DATA.getValue() ) {
            _outputWriter.appendToFile(p.getData());

            int nextSequenceNumberExpected = (p.getSeqNum() + 1) % MODULUS;

            _ackSender.sendPacket(new packet(PacketHelper.PacketType.ACK.getValue(), nextSequenceNumberExpected, 0, null));
        }
        else if( p.getType() == PacketHelper.PacketType.ClientToServerEOT.getValue() ) {
            _outputWriter.closeFile();

            _ackSender.sendPacket(new packet(PacketHelper.PacketType.ServerToClientEOT.getValue(), 0, 0, null));
        }

        // queue up the listener again
        _dataReceiver.run();
    }

}
