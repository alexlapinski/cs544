import java.lang.Integer;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.*;
import java.io.*;

public class ClientApplication {

    private static String _serverAddress;
    private static int _fixedPort;
    private static String _filenameToTransfer;
    private static int _transferPort;
    private static byte[] _fileContents;

    public static void main(String [ ] args) {

        if( args.length == 0 ) {
            _printHelp();
        } else {
            _parseArguments(args);
            _transferPort = _negotiateTransferPort(_serverAddress, _fixedPort);
            _fileContents = _readContentsOfFile(_filenameToTransfer);
            _transferDataToServer(_serverAddress, _transferPort, _fileContents);
        }
    }

    /**
     * @brief Print the CLI Usage information
     * @details Print the simple CLI Usage information to std out
     */
    private static void _printHelp() {
        System.out.println("Use the following format to specify commands: <server_address> <f_port> <filename>");
    }

    /**
     * @brief Parse the CLI arguments
     * @details Parse the arguments, just assume position in the args array
     * 
     * @param args CLI Arguments
     */
    private static void _parseArguments(String[] args) {
        _serverAddress = args[0];
        _fixedPort = Integer.parseInt(args[1], 10);
        _filenameToTransfer = args[2];

        System.out.println("Arguments Supplied");
        System.out.println("     ServerAddress: " + _serverAddress);
        System.out.println("     FixedPort: " + _fixedPort);
        System.out.println("     FilenameToTransfer: " + _filenameToTransfer);
    }

    /**
     * @brief negotiate the random port to use for transfer with the server
     * @details Set up the socket, input/output data streams for communication, communicates with the server and receives the random port.
     * @return port to use for file transfer as determined by the server
     */
    private static int _negotiateTransferPort(String serverAddress, int serverPort) {
        final int NEGOTIATION_MESSAGE = 42;

        Socket negotiationSocket;
        DataOutputStream negotiationOutputStream;
        DataInputStream negotiationInputStream;
        int transferPort = -1;

        // Setup our socket and in/out streams for port negotiation
        try {
            negotiationSocket = new Socket(serverAddress, serverPort);
            negotiationOutputStream = new DataOutputStream(negotiationSocket.getOutputStream());
            negotiationInputStream = new DataInputStream(negotiationSocket.getInputStream());
        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy handle the error
            return -1;
        }

        // Ensure we have a valid socket
        if( negotiationSocket == null ) {
            System.out.println("Negotiation Service did not setup correctly");
            System.exit(-1);
        }

        // Ensure we have a valid input stream
        if( negotiationInputStream == null ) {
            System.out.println("Negotiation Service Input Stream did not setup correctly");
            System.exit(-1);
        }

        // Ensure we have a valid output stream
        if( negotiationOutputStream == null ) {
            System.out.println("Negotiation Service Output Stream did not setup correctly");
            System.exit(-1);
        }

        try {

            // Send our Int to indicate to the server that we want a random port for file transfer
            negotiationOutputStream.writeInt(NEGOTIATION_MESSAGE);

            // infinite loop to ensure we read the server response
            while( true ) {
                
                int message;

                try {
                    // read the message from the server, we expect an int and this int to be the random port to be used for file transfer
                    message = negotiationInputStream.readInt();
                } catch(EOFException eofe) {
                    break; // End of stream, exit infinite loop
                } catch(IOException ioe) {
                    throw ioe; // throw it to the outer catch handler
                }

                transferPort = message; // assume message received is a valid port
                System.out.println("Received the port value '" + transferPort + "' to use for file transfer");

                // we got the port, so exit this listening stream
                break;
            }

            // cleanup our socket and in/out stream
            negotiationOutputStream.close();
            negotiationInputStream.close();
            negotiationSocket.close();

        } catch(UnknownHostException uhe) {
            System.out.println(uhe); // Lazy error handling
        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy error handling
        }

        return transferPort; // 
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
     * @param serverAddress The server address
     * @param transferPort The transfer port to use
     * @param data The data to transfer, must be non-null
     */
    private static void _transferDataToServer(String serverAddress, int transferPort, byte[] data) {

        System.out.println("Sending file to server");
        final int WORD_SIZE = 4;

        DatagramSocket clientSocket = null;
        InetAddress localAddress = null;
        
        try {
            clientSocket = new DatagramSocket();
        } catch(SocketException se) {
            System.out.println(se);
            return;
        }

        try {
            localAddress = InetAddress.getByName(serverAddress);
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

            DatagramPacket dataPacket = new DatagramPacket(messageAsBytes, messageAsBytes.length, localAddress, transferPort);
    
            System.out.println("Sending Message to Server: { isLastMessage: '"+message.getIsLastMessage()+"', payload: '"+ message.getPayloadAsString() + "'}");
            try {
                clientSocket.send(dataPacket);
            } catch(IOException ioe) {
                System.out.println(ioe);
            }

            byte[] ackData = new byte[TransferMessage.MESSAGE_SIZE];
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
            
            try {
                clientSocket.receive(ackPacket);
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
