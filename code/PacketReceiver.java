import java.io.*;
import java.net.*;

public class PacketReceiver implements Runnable {

    public interface INotifyPacketArrived {
        void notifyPacketArrived(packet p);
    }

    private INotifyPacketArrived _arrivalListener;
    private DatagramSocket _listeningSocket;

    public PacketReceiver(DatagramSocket listeningSocket, INotifyPacketArrived arrivalListener) {
        _arrivalListener = arrivalListener;

        _listeningSocket = listeningSocket;
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