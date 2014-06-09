import java.io.*;
import java.net.*;

public class PacketReceiver implements Runnable {

    public interface INotifyPacketArrived {
        void notifyPacketArrived(packet p);
    }

    private DatagramSocket _listeningSocket;
    private INotifyPacketArrived _arrivalListener;

    public PacketReceiver(int listeningPort, INotifyPacketArrived arrivalListener) {
        _arrivalListener = arrivalListener;

        try {
            _listeningSocket = new DatagramSocket(listeningPort);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }

    public void run() {
        byte[] receivedData = new byte[1024];
        DatagramPacket receivedUDPPacket = new DatagramPacket(receivedData, receivedData.length);
        packet receivedPacket = null;

        try {
            _listeningSocket.receive(receivedUDPPacket);
            receivedPacket = PacketHelper.deserialize(receivedData);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }

        _arrivalListener.notifyPacketArrived(receivedPacket);
    }
}