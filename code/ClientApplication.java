// Author: Alex Lapinski
// Date: June 7th, 2014

import java.lang.Integer;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.*;
import java.io.*;

public class ClientApplication {

    public static class AckListener implements Runnable {

        private DatagramSocket _listeningSocket;
        private packet _receivedPacket;

        public AckListener(DatagramSocket listeningSocket) {
            _listeningSocket = listeningSocket;
        }

        public void run() {
            byte[] ackData = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
            
            try {
                _listeningSocket.receive(ackPacket);
                _receivedPacket = PacketHelper.deserialize(ackData);
            } catch(IOException ioe) {
                System.out.println(ioe); // Lazy Error Handling
            }
        
            System.out.println("Received ACK ! Next SeqNum Expected: '" + _receivedPacket.getSeqNum() + "'");

            ClientApplication.setSn(_receivedPacket.getSeqNum());
        }
    }

    private static String _emulatorHostname;
    private static int _sendPort;
    private static int _receivePort;
    private static String _filenameToTransfer;
    private static char[] _fileContents;

    private static int _s_n = 0;
    private static int _s_f = 0;
    private static synchronized void setSn(int value) {
        _s_f = value - 1;
        _s_n = value;
    }

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
            
            _fileContents = _readContentsOfFile(_filenameToTransfer);
            
            _transferDataToServer(_emulatorHostname, _sendPort, _receivePort, _fileContents);
        }
    }

    public static void notifyAckReceived(int ackNumber) {
        System.out.println("Received Ack for " + ackNumber);
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
     * @return contents of the file as a char array, null if an error has occured
     */
    private static char[] _readContentsOfFile(String filename) {

        ArrayList<Character> buffer = new ArrayList<Character>();
        
        // Check validity of filename
        if( filename == null || filename.length() == 0 ) {
            return null;
        }

        // setup reader
        FileInputStream fin = null;
        InputStreamReader inStream = null;
        BufferedReader reader = null;
        
        try {
            fin = new FileInputStream(filename);
        } catch(FileNotFoundException fnfe) {
            System.out.println(fnfe);
        }

        if( fin == null ) {
            return null;
        }
        inStream = new InputStreamReader(fin);
        reader = new BufferedReader(inStream);
        
        // read the file
        try {
            int c;

            while((c = reader.read()) != -1) {
                buffer.add(new Character((char)c));
            }
            
            fin.close();

        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy Error Handling
        }

        // Convert to simple byte array
        char[] fileContents = new char[buffer.size()];
        for(int i = 0; i < buffer.size(); i++ ) {
            fileContents[i] = buffer.get(i).charValue();
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
    private static void _transferDataToServer(String emulatorHostname, int sendPort, int receivePort, char[] data) {

        System.out.println("Sending file to server");
        
        // GBN variables
        int m = 3; // used to calculate window size
        int window_size = (int)Math.pow(2, m) - 1; // (2^m) - 1
        int modulus = (int)Math.pow(2, m); // used in modulus divison
        
        // Transmission Variables
        DatagramSocket receiveSocket = null;
        InetAddress localAddress = null;
        packet[] sendWindow = new packet[window_size];
        
        try {
            receiveSocket = new DatagramSocket(receivePort);
        } catch(SocketException se) {
            System.out.println(se);
            return;
        }

        try {
            localAddress = InetAddress.getByName(emulatorHostname);
            System.out.println("Connecting to '"+localAddress+"'");
        } catch(UnknownHostException uhe) {
            System.out.println(uhe);
            return;
        }

        // Chunking Variables
        char[] totalDataToSend = data;
        int remainingCharacters = totalDataToSend.length;
        int currentChunk = 0;

        // Infinite Loop until file has been entirely sent (TODO: make sure ALL acks received)
        while(remainingCharacters > 0) {
            
            // Create a packet, and send it
            if( (_s_n != (_s_f + window_size) % modulus) || (_s_n == 0 && _s_f == 0) ) {            

                System.out.println("Creating Data Packet");
                packet p = _createDataPacket(currentChunk, totalDataToSend, _s_n);
                System.out.println("DataPacket: ");

                _sendPacketToServer(p, localAddress, sendPort);
                sendWindow[_s_n] = p;

                System.out.println("Listening for ACK");
                (new Thread(new AckListener(receiveSocket))).start();
                
                // Increment Next to send
                _s_n = (_s_n + 1) % modulus;

                // Update counters for chunking
                remainingCharacters -= p.getData().length();
                currentChunk++;
            }
            else {
                // send window is full, wait for successful ACKs
                // System.out.println("Send Window Full");

            }

            // TODO: What happens when we get an ACK
                // mark all items in window with index less than ACK'd number as 'null'
                // update firstOutstanding Packet to equal the ACK'd number
            
        }

        System.out.println("Sending file to server Complete");
    }

    private static packet _createDataPacket(int currentChunk, char[] sourceData, int s_n) {
        int startIndex = (currentChunk * PacketHelper.PACKET_MAX_CHARACTERS);
        int endIndex = ((currentChunk+1) * PacketHelper.PACKET_MAX_CHARACTERS);

        char[] chunkOfData = Arrays.copyOfRange(sourceData, startIndex, endIndex);
        String packetPayload = new String(chunkOfData);

        return new packet(PacketHelper.PacketType.DATA.getValue(), s_n, packetPayload.length(), packetPayload);
    }

    private static void _sendPacketToServer(packet p, InetAddress localAddress, int sendPort) {
        byte[] dataToSend = null;

        try {
            dataToSend = PacketHelper.serialize(p);
        } catch(IOException ioe) {
            System.out.println("Error Serializing packet to byte array");
            System.out.println(ioe);
        }

        DatagramPacket dataPacket = new DatagramPacket(dataToSend, dataToSend.length, localAddress, sendPort);
        
        System.out.println("Sending Message to Server:");
        p.printContents();
        try {
            DatagramSocket sendSocket = new DatagramSocket();
            sendSocket.send(dataPacket);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }
}
