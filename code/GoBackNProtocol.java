import java.util.Timer;
import java.util.TimerTask;

public class GoBackNProtocol implements PacketHelper.ITimerListener{
    
    private final class TimeoutTask extends TimerTask {

        private PacketHelper.ITimerListener _listener;

        public TimeoutTask(PacketHelper.ITimerListener listener) {
            _listener = listener;
        }

        public void run() {
            _listener.notifyTimeoutOccured();
        }
    }

    private final int TIMEOUT_DELAY = 800; // in milliseconds
    private int _indexOfNextPacketToSend;
    private int _indexOfFirstOutstandingPacket;
    private final int WINDOW_MAX_SIZE;
    private final int MODULUS;

    private packet[] _sendBuffer;

    private FileChunker _dataChunker;
    private PacketSender _dataSender;
    private SimpleFileWriter _seqNumLogger;
    private Timer _timeoutTimer;
    private boolean _isTimerRunning;
    private boolean _isBlocked;

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
        return _isBlocked;
    }

    private void _stopTimer() {
        synchronized(GoBackNProtocol.class) {
            if( _timeoutTimer == null ) {
                return;
            }
            _timeoutTimer.cancel();
            _timeoutTimer.purge();
            _timeoutTimer = null;
            _isTimerRunning = false;
        }
    }

    private void _restartTimer() {
        _stopTimer();
        _startTimer();
    }

    private void _startTimer() {
        synchronized(GoBackNProtocol.class) {
            _timeoutTimer = new Timer("_indexOfNextPacketToSend " + _indexOfNextPacketToSend);
            _timeoutTimer.schedule(new TimeoutTask(this), TIMEOUT_DELAY);
            _isTimerRunning = true;
        }
    }

    public void notifyTimeoutOccured() {
        _isTimerRunning = false;

        System.out.println("Timeout Occured, resending packets from [" + _indexOfFirstOutstandingPacket + " to " + _indexOfNextPacketToSend + ")");
        // Resend All outstanding packets
        for(int i = _indexOfFirstOutstandingPacket; i != _indexOfNextPacketToSend; i = (i + 1) % MODULUS ) {
            packet p = _sendBuffer[i];

            System.out.println("Re-Sending Packet at index: " + i + " with sequenceNumber: " + p.getSeqNum());
            _dataSender.sendPacket(p);
            _seqNumLogger.appendToFile(p.getSeqNum() + "\n");
        }

        // Restart the timer
        _restartTimer();
    }

    private void purgeValuesFromWindow(int fromIndex, int toIndex) {
        System.out.println("Purging values from [" + fromIndex + " to " + toIndex + ")");
        for(int i = fromIndex; i != toIndex; i = (i + 1) % MODULUS) {
            System.out.println("Purging item at index: " + i);
            _sendBuffer[i] = null;
        }
    }

    public void notifyAckArrived(packet ackPacket) {
        
        int ackNumber = ackPacket.getSeqNum();
        int ackNumberAfterLoop = (ackNumber + WINDOW_MAX_SIZE) % MODULUS;

        System.out.println("Ack for SeqNum '" + ackNumber + "' arrived; _indexOfNextPacketToSend = " + _indexOfNextPacketToSend + "; _indexOfFirstOutstandingPacket = " + _indexOfFirstOutstandingPacket);

        boolean withinRange = (ackNumber >= _indexOfFirstOutstandingPacket);
        boolean withinRangeAfterLoop = (ackNumberAfterLoop >= _indexOfFirstOutstandingPacket); 
        
        System.out.println("ackNumber >= _indexOfFirstOutstandingPacket ? " + withinRange + " ... after looping? " + withinRangeAfterLoop);
        
        if( (withinRange || withinRangeAfterLoop) && ackNumber <= _indexOfNextPacketToSend ) {
            
            purgeValuesFromWindow(_indexOfFirstOutstandingPacket, ackNumber);
            _indexOfFirstOutstandingPacket = ackNumber;

            if( ackNumber == _indexOfNextPacketToSend ) {
                _isBlocked = false;
                _stopTimer();
            } else {
                _restartTimer();
            }

        } else {
            // Discard ACK
        }

    }

    public void sendPacket() {
        if( isBlocking()) {
            return; // Do nothing, we're in a blocking state
        }

        if( !_dataChunker.hasMoreChunks() ) {
            return;
        }

        // Determine Sequence Number
        int sequenceNumber = _indexOfNextPacketToSend;
        String payload = _dataChunker.getNextChunk();

        packet p = new packet(PacketHelper.PacketType.DATA.getValue(), sequenceNumber, payload.length(), payload);
        _seqNumLogger.appendToFile(sequenceNumber + "\n");
        System.out.println("Saving Packet with SequenceNumber " + p.getSeqNum() + " in slot " + sequenceNumber);
        _sendBuffer[sequenceNumber] = p; // store a copy
        _dataSender.sendPacket(p); // send packet

        if( !_isTimerRunning ) {
            _startTimer();       
        }
     
        int newIndex = (_indexOfNextPacketToSend + 1) % MODULUS; // increment next expected packet to send
        if( ((_indexOfFirstOutstandingPacket + WINDOW_MAX_SIZE) % MODULUS ) == newIndex ) {
            _isBlocked = true;
            _indexOfNextPacketToSend = newIndex;
        } else {
            _indexOfNextPacketToSend = newIndex;
        }
    }

    public void sendEOTPacket() {
        _seqNumLogger.appendToFile(_indexOfNextPacketToSend + "\n");
        _dataSender.sendPacket(new packet(PacketHelper.PacketType.ClientToServerEOT.getValue(), _indexOfNextPacketToSend, 0, null));
        _stopTimer();
    }

    public void finish() {
        _stopTimer();
    }

    public boolean areAllSentPacketsAcked() {
        boolean areAllSentPacketsAcked = true;

        for(int i = 0; i < _sendBuffer.length; i++ ) {
            if( _sendBuffer[i] != null ) {
                areAllSentPacketsAcked = false;
                break;
            }
        }

        return areAllSentPacketsAcked;
    }
}