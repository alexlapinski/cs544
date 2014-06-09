// Author: Alex Lapinski
// Date: June 7th, 2014

import java.lang.Integer;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.*;
import java.io.*;

public class ClientApplication implements PacketReceiver.INotifyPacketArrived {

    private static String _emulatorHostname;
    private static int _sendPort;
    private static int _receivePort;
    private static String _filenameToTransfer;

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
            ClientApplication instance = new ClientApplication(_emulatorHostname, _sendPort, _receivePort, _filenameToTransfer);
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

    public ClientApplication(String destinationHostname, int sendPort, int receivePort, String filenameToTransfer) {
        _fileChunker = new FileChunker(_filenameToTransfer, 30);
        _dataSender = new PacketSender(_emulatorHostname, _sendPort);

        _gbnProtocol = new GoBackNProtocol(3, _fileChunker, _dataSender);

        while( _fileChunker.hasMoreChunks() ) {
            _gbnProtocol.sendPacket();
        }
    }

    public void notifyPacketArrived(packet p) {
        // TODO: Handle the arrival of an ACK
    }
}
