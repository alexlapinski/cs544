import java.net.*;
import java.io.*;

public final class PacketHelper {
	
	/**
	 * @brief PacketType enum, representing the types of packets
	 * @details Each packet type corresponds to a non-negative integer.
	 */
	public enum PacketType {
	
		ACK(0),
		DATA(1),
		ServerToClientEOT(2),
		ClientToServerEOT(3);

		private int _value;
		PacketType(int value) {
			_value = value;
		}

		public int getValue() {
			return _value;
		}
	}

	/**
	 * @brief Shared Const representing the max size of a packet
	 * @details Max number of characters that a packet may carry
	 */
	public static final int PACKET_MAX_CHARACTERS = 30;

	/**
	 * @brief Simple Helper method to serialize a packet object into bytes
	 * @details Accept a valid instance of a packet p and serialize it to a byte array
	 * 
	 * @param p Valid packet object
	 * @return packet p as a byte array or null if input was invalid or null
	 */
	public static byte[] serialize(packet p) throws IOException {
		if( p == null ) {
			return null;
		}

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

		objectOutputStream.writeObject(p);
		objectOutputStream.flush();

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * @brief deserialize a packet represented as a byte array
	 * @details Accept a byte array and deserialize it into a packet for use in the server
	 * 
	 * @param source valid non-null byte array
	 * @return valid packet instance, or null if source was invalid or null
	 */
	public static packet deserialize(byte[] source) throws IOException {
		if( source == null ) {
			return null;
		}

		if( source.length == 0 ) {
			return null;
		}

		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(source));

		packet objectValue = null;

		try {
			objectValue = (packet) objectInputStream.readObject();
			objectInputStream.close();
		} catch(ClassNotFoundException cnfe) {
			System.out.println(cnfe);
		}

		return objectValue;
	}
}