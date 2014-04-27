package edu.drexel.cs544.alexlapinski;

import java.util.Arrays;

/**
 * @brief A simple class to contain the transfer message
 * @details A simple class that has a payload of a string and a boolean flag to indicate if it is the last message
 */
public class TransferMessage {

    public static final int FLAG_SIZE = 1; // byte
    public static final int PAYLOAD_SIZE = 16; // bytes
    public static final int MESSAGE_SIZE = 17; // 1 byte for flag + 16 bytes for payload

    private boolean _isLastMessage;
    private byte[] _payload;

    public boolean getIsLastMessage() {
        return _isLastMessage;
    }

    public byte[] getPayload() {
        return _payload;
    }
    
    public String getPayloadAsString() {
        return new String(_payload).trim();
    }

    public TransferMessage(boolean isLastMessage, byte[] payload) throws Exception {
        _isLastMessage = isLastMessage;
        _payload = payload;

        int payloadSizeInBytes = payload.length;
        if( payloadSizeInBytes > PAYLOAD_SIZE ) {
            throw new Exception("The payload may only be a max size of " + PAYLOAD_SIZE + " bytes; it was " + payloadSizeInBytes);
        }
    }

    /**
     * @brief Transform the Transfer Message to a byte array
     * @details Packs up the Transfer Message as a byte array to send as a Datagram
     * @return byte array representation of TransferMessage
     */
    public byte[] getBytes() {
        
        byte[] message = new byte[TransferMessage.MESSAGE_SIZE];

        // The isLastMessage flag gets converted to the first byte
        message[0] = TransferMessage._convertBooleanToByte(_isLastMessage);

        // The payload is the last 16 bytes
        for(int i = 0;i < PAYLOAD_SIZE && i < _payload.length; i++ ) {
            message[i+1] = _payload[i];
        }

        return message;
    }

    /**
     * @brief Convience method to help building a transfer message from a byte
     * @details Parses a byte arrary representation of the message and builds a TransferMessage object
     * 
     * @param rawMessage byte array representation of a TransferMessage
     * @return a valid TransferMessage, or null if parsing failed
     */
    public static TransferMessage fromBytes(byte[] rawMessage) throws Exception {

        if( rawMessage == null || rawMessage.length == 0) {
            return null; // null input == null output
        }

        // First byte is the boolean flag
        byte firstByte = rawMessage[0];
        boolean isLastMessage = TransferMessage._convertByteToBoolean(firstByte);

        byte[] remainingBytes = Arrays.copyOfRange(rawMessage, 1, rawMessage.length);

        // TODO
        return new TransferMessage(isLastMessage, remainingBytes);
    }


    /**
     * @brief Internal Convience method for boolean to byte
     * @details a boolean 'true' is converted to 0b0001, otherwise it is converted to 0b0000
     * 
     * @param value boolean value
     * @return byte representation of boolean value
     */
    private static byte _convertBooleanToByte(boolean value) {
        if( value ) {
            return 0b0001;
        }

        return 0b0000;
    }

    /**
     * @brief Internal Convience method for byte to boolean
     * @details a non-zero value is considered 'true', otherwise it is 'false'
     * 
     * @param value 1-byte representation of a boolean value
     * @return boolean value
     */
    private static boolean _convertByteToBoolean(byte value) {
        return ( value != 0b0000 ); // anything non-zero is 'true'
    }
}