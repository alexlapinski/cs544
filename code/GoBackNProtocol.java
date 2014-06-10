public class GoBackNProtocol {
    
    private int _indexOfNextPacketToSend;
    private int _indexOfFirstOutstandingPacket;
    private final int WINDOW_MAX_SIZE;
    private final int MODULUS;

    private packet[] _sendBuffer;

    private FileChunker _dataChunker;
    private PacketSender _dataSender;
    private SimpleFileWriter _seqNumLogger;
    private Thread _ackListener;

    public GoBackNProtocol(int m, FileChunker dataChunker, PacketSender dataSender, SimpleFileWriter seqNumLogger) {
        WINDOW_MAX_SIZE = (int) Math.pow(2, m) - 1;
        MODULUS = (int) Math.pow(2, m);

        _indexOfFirstOutstandingPacket = 0;
        _indexOfNextPacketToSend = 0;

        _sendBuffer = new packet[WINDOW_MAX_SIZE + 1];

        _dataChunker = dataChunker;
        _dataSender = dataSender;
        _seqNumLogger = seqNumLogger;
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
            _sendBuffer[i] = null;
        }
    }

    public void notifyAckArrived(packet ackPacket) {
        
        int ackNumber = ackPacket.getSeqNum();

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
        _seqNumLogger.appendToFile(sequenceNumber + "\n");
        _sendBuffer[sequenceNumber] = p; // store a copy
        _dataSender.sendPacket(p); // send packet
        _indexOfNextPacketToSend = (_indexOfNextPacketToSend + 1) % MODULUS; // increment next expected packet to send
    }

    public void sendEOTPacket() {
        _seqNumLogger.appendToFile(_indexOfNextPacketToSend + "\n");
        _dataSender.sendPacket(new packet(PacketHelper.PacketType.ClientToServerEOT.getValue(), _indexOfNextPacketToSend, 0, null));
    }
}