package edu.drexel.cs544.alexlapinski;

import java.lang.Integer;

public class ClientApplication {

    private static String _serverAddress;
    private static int _fixedPort;
    private static String _filenameToTransfer;

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
        }
    }

    private static void _printHelp() {
        System.out.println("Use the following format to specify commands: <server_address> <f_port> <filename>");
    }

    private static void _parseArguments(String[] args) {
        _serverAddress = args[0];
        _fixedPort = Integer.parseInt(args[1], 10);
        _filenameToTransfer = args[2];

        System.out.println("Arguments Supplied");
        System.out.println("     ServerAddress: " + _serverAddress);
        System.out.println("     FixedPort: " + _fixedPort);
        System.out.println("     FilenameToTransfer: " + _filenameToTransfer);
    }
}
