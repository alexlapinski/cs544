// Author: Alex Lapinski
// Date: June 7th, 2014

import java.lang.Integer;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.*;
import java.io.*;

public class ClientApplication {

    private static String _emulatorHostname;
    private static int _sendPort;
    private static int _receivePort;
    private static String _filenameToTransfer;
    private static byte[] _fileContents;

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
            
            _fileContents = _readContentsOfFile(_filenameToTransfer);
            _transferDataToServer(_emulatorHostname, _sendPort, _receivePort, _fileContents);
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

    /**
     * @brief Read the contents of the specified file and return as a string
     * @details Read in the contents of a text file and return it as a string
     * 
     * @param filename Filename of a plain text ASCII file
     * @return contents of the file as a byte array, null if an error has occured
     */
    private static byte[] _readContentsOfFile(String filename) {

        ArrayList<Byte> buffer = new ArrayList<Byte>();
        
        // Check validity of filename
        if( filename == null || filename.length() == 0 ) {
            return null;
        }

        // setup reader
        FileReader fin = null;
        
        try {
            fin = new FileReader(filename);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        if( fin == null ) {
            return null;
        }

        // read the file
        try {
            int c;

            while((c = fin.read()) != -1) {

                buffer.add(new Byte((byte)c));
            }
            
            fin.close();

        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy Error Handling
        }

        // Convert to simple byte array
        byte[] fileContents = new byte[buffer.size()];
        for(int i = 0; i < buffer.size(); i++ ) {
            fileContents[i] = buffer.get(i);
        }

        return fileContents;
    } 

    /**
     * @brief Transfer String Data to the server specified at the port specified
     * @details Transfers String Data to the server via UDP at the port specified, if data is null, the transfer does not occur
     * 
     * @param emulatorHostname The emulator hostname
     * @param sendPort The UDP port to use for sending data to the emulator
     * @param receivePort THe UDP port to use for receiving ACKs from the emulator
     * @param data The data to transfer, must be non-null
     */
    private static void _transferDataToServer(String emulatorHostname, int sendPort, int receivePort, byte[] data) {

        System.out.println("Sending file to server");
        final int WORD_SIZE = 4;

        DatagramSocket sendSocket = null;
        DatagramSocket receiveSocket = null;
        InetAddress localAddress = null;
        
        try {
            sendSocket = new DatagramSocket(sendPort);
            receiveSocket = new DatagramSocket(receivePort);
        } catch(SocketException se) {
            System.out.println(se);
            return;
        }

        try {
            localAddress = InetAddress.getByName(emulatorHostname);
        } catch(UnknownHostException uhe) {
            System.out.println(uhe);
            return;
        }

        byte[] totalDataToSend = data;

        int remainingBytes = totalDataToSend.length;
        int currentChunk = 0;
        
        while(remainingBytes > 0) {

            // TODO: Create message "Structure" that has a flag to indicate more data available, or to indicate last message

            int startIndex = (currentChunk * TransferMessage.PAYLOAD_SIZE);
            int endIndex = ((currentChunk+1) * TransferMessage.PAYLOAD_SIZE);

            byte[] chunkOfData = Arrays.copyOfRange(totalDataToSend, startIndex, endIndex);

            TransferMessage message = null;
            try {
                message = new TransferMessage(endIndex >= totalDataToSend.length, chunkOfData);
            } catch(Exception e) {
                System.out.println(e);
            }

            byte[] messageAsBytes = message.getBytes();

            DatagramPacket dataPacket = new DatagramPacket(messageAsBytes, messageAsBytes.length, localAddress, sendPort);
    
            System.out.println("Sending Message to Server: { isLastMessage: '"+message.getIsLastMessage()+"', payload: '"+ message.getPayloadAsString() + "'}");
            try {
                sendSocket.send(dataPacket);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            byte[] ackData = new byte[TransferMessage.MESSAGE_SIZE];
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
            
            try {
                receiveSocket.receive(ackPacket);
            } catch(IOException ioe) {
                System.out.println(ioe); // Lazy Error Handling
            }
            System.out.println("Received ACK '" + new String(ackPacket.getData()) + "'");

            currentChunk++;
            remainingBytes -= chunkOfData.length;
        }

        System.out.println("Sending file to server Complete");
    }
}
