package yield.gelf.types;

import java.util.Arrays;

/**
 * A single chunk of chunked message.
 */
public class ChunkedMessage extends GelfMessage {
	private byte[] message;

	public ChunkedMessage(byte[] message) {
		super(message);
		this.message = message;
	}

	@Override
	public MessageType getType() {
		return MessageType.CHUNKED;
	}

	/**
	 * @return Message id. All chunks of the same message share this id.
	 */
	public MessageId getMessageId() {
		return new MessageId(Arrays.copyOfRange(message, 2, 10));
	}

	/**
	 * @return Index of chunk. First chunk has index 0.
	 */
	public byte getSequenceNumber() {
		return message[10];
	}

	/**
	 * @return Number of chunks of the message.
	 */
	public byte getSequenceCount() {
		return message[11];
	}

	@Override
	public String getBody() {
		byte[] body = Arrays.copyOfRange(message, 12, message.length);
		if (MessageType.getType(body) == MessageType.UNCOMPRESSED) {
			return GelfMessage.forMessage(body).getBody();
		} else {
			throw new IllegalArgumentException(
					"Fragments of a chunked message cannot be decompressed separately.");
		}
	}

	/**
	 * @return Body of chunk.
	 */
	public byte[] getRawBody() {
		return Arrays.copyOfRange(message, 12, message.length);
	}
}
