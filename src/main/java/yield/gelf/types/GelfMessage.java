package yield.gelf.types;

/**
 * Message or part thereof which was transmitted in a single network packet.
 */
public abstract class GelfMessage {
	/**
	 * @param message
	 *            Payload of network packet.
	 */
	public GelfMessage(byte[] message) {
		if (MessageType.getType(message) != getType()) {
			throw new IllegalArgumentException("Message type mismatch.");
		}
	}

	/**
	 * @return Type of message.
	 */
	public abstract MessageType getType();

	/**
	 * @return Message as JSON. Unavailable for chunked messages.
	 */
	public abstract String getBody();

	/**
	 * @param message
	 *            Payload of network packet.
	 * @return Suitable representation of message.
	 */
	public static GelfMessage forMessage(byte[] message) {
		MessageType bodyType = MessageType.getType(message);
		switch (bodyType) {
		case CHUNKED:
			return new ChunkedMessage(message);
		case GZIP:
			return new GzipMessage(message);
		case UNCOMPRESSED:
			return new UncompressedMessage(message);
		case ZLIB:
			return new ZlibMessage(message);
		default:
			// Should never happen as the above cases should match everything.
			throw new RuntimeException("Message type unknown.");
		}
	}
}
