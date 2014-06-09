import java.io.*;
import java.net.*;

public class PacketSender {

    private DatagramSocket _sendingSocket;
    private InetAddress _destinationAddress;
    private int _destinationPort;

    public PacketSender(String destinationHostname, int destinationPort) {
        _destinationPort = destinationPort;

        try {
            _destinationAddress = InetAddress.getByName(destinationHostname);
        } catch(UnknownHostException uhe) {
            System.out.println(uhe);
        }

        try {
            _sendingSocket = new DatagramSocket();
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }

    public void sendPacket(packet p) {
        byte[] dataToSend = null;
        
        try {
            dataToSend = PacketHelper.serialize(p);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        DatagramPacket udpPacket = new DatagramPacket(dataToSend, dataToSend.length, _destinationAddress, _destinationPort);

        try {
            _sendingSocket.send(udpPacket);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }
}