public final class PacketHelper {
	
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

	public static final int PACKET_MAX_SIZE = 30;
}