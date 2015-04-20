package yield.gelf.types;

import java.nio.charset.StandardCharsets;

/**
 * Message that is transferred as simple JSON.
 */
public class UncompressedMessage extends GelfMessage {

	private byte[] message;

	public UncompressedMessage(byte[] message) {
		super(message);
		this.message = message;
	}

	@Override
	public MessageType getType() {
		return MessageType.UNCOMPRESSED;
	}

	@Override
	public String getBody() {
		return new String(message, StandardCharsets.UTF_8);
	}

}
