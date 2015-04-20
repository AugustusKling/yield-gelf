package yield.gelf.types;

import java.util.Arrays;

/**
 * Types of GELF messages.
 */
public enum MessageType {
	/**
	 * GZIP compressed, everything in one packet.
	 */
	GZIP((byte) 0x1F, (byte) 0x8B),
	/**
	 * ZLib compressed, everything in one packet.
	 */
	ZLIB((byte) 0x78, (byte) 0x00) {
		@Override
		protected boolean secondByteMatches(byte[] message) {
			// Second byte is not constant.
			// TODO Check if value is plausible nevertheless.
			return true;
		}
	},
	/**
	 * Message spans across various packets. Strip 12 byte header of first
	 * packet to determine type.
	 */
	CHUNKED((byte) 0x1e, (byte) 0x0f),
	/**
	 * Simple message. Uncompressed, everything in one packet.
	 */
	UNCOMPRESSED((byte) 0x7B, (byte) 0x00) {
		@Override
		protected boolean secondByteMatches(byte[] message) {
			// Second byte does not matter because it could be any whitespace or
			// a quote.
			return true;
		}
	};

	private byte headerFirst;
	private byte headerSecond;

	private MessageType(byte headerFirst, byte headerSecond) {
		this.headerFirst = headerFirst;
		this.headerSecond = headerSecond;
	}

	/**
	 * Deduces message type from first bytes of message.
	 * 
	 * @param message
	 *            Payload of network packet.
	 * @return Type of message.
	 */
	public static MessageType getType(byte[] message) {
		if (message.length < 2) {
			throw new IllegalArgumentException("Message is too short: "
					+ bytesToHex(message));
		}
		for (MessageType type : MessageType.values()) {
			if (type.firstByteMatches(message)
					&& type.secondByteMatches(message)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Message type unknown: "
				+ bytesToHex(Arrays.copyOfRange(message, 0, 2)));
	}

	protected boolean firstByteMatches(byte[] message) {
		return headerFirst == message[0];
	}

	protected boolean secondByteMatches(byte[] message) {
		return headerSecond == message[1];
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}