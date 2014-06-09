public class GoBackNProtocol {
    
    private int _indexOfNextPacketToSend;
    private int _indexOfFirstOutstandingPacket;
    private final int WINDOW_MAX_SIZE;
    private final int MODULUS;

    private packet[] _sendWindow;

    private FileChunker _dataChunker;
    private PacketSender _dataSender;
    private Thread _ackListener;

    public GoBackNProtocol(int m, FileChunker dataChunker, PacketSender dataSender) {
        WINDOW_MAX_SIZE = (int) Math.pow(2, m) - 1;
        MODULUS = (int) Math.pow(2, m) - 1;

        _indexOfFirstOutstandingPacket = 0;
        _indexOfNextPacketToSend = 0;

        _sendWindow = new packet[WINDOW_MAX_SIZE];

        _dataChunker = dataChunker;
        _dataSender = dataSender;
    }

    private boolean isBlocking() {
        return (_indexOfFirstOutstandingPacket + WINDOW_MAX_SIZE) == _indexOfNextPacketToSend;
    }

    private void stopTimer() {
        // TODO
    }

    private void restartTimer() {
        // TODO
    }

    private void startTimer() {
        // TODO
    }

    private void handleTimerExpired() {
        // TODO

        // Resend All outstanding packets

        // Restart the timer
    }

    private void purgeValuesFromWindow(int fromIndex, int toIndex) {
        for(int i = fromIndex; i <= toIndex; i++) {
            _sendWindow[i] = null;
        }
    }

    public void notifyAckArrived(packet ackPacket) {
        
        int ackNumber = ackPacket.getSeqNum();

        System.out.println("Received Packet With Ack # = " + ackNumber);
        ackPacket.printContents();

        if( ackNumber >= _indexOfFirstOutstandingPacket && ackNumber < _indexOfNextPacketToSend ) {
            purgeValuesFromWindow(_indexOfFirstOutstandingPacket, ackNumber);
            _indexOfFirstOutstandingPacket = ackNumber;

            if( ackNumber == _indexOfNextPacketToSend ) {
                stopTimer();
            }

        } else {
            // Discard ACK
        }

    }

    public void sendPacket() {
        if( isBlocking() ) {
            return; // Do nothing, we're in a blocking state
        }

        // Determine Sequence Number
        int sequenceNumber = _indexOfNextPacketToSend;
        String payload = _dataChunker.getNextChunk();

        packet p = new packet(PacketHelper.PacketType.DATA.getValue(), sequenceNumber, payload.length(), payload);
        _sendWindow[_indexOfNextPacketToSend] = p; // store a copy

        System.out.println("Sending Packet w/ Sequence # = " + sequenceNumber);
        p.printContents();

        _dataSender.sendPacket(p); // send packet
        _indexOfNextPacketToSend = (_indexOfNextPacketToSend + 1) % MODULUS; // increment next expected packet to send
    }
}