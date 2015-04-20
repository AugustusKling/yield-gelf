package yield.gelf.types;

import java.util.Arrays;

/**
 * Message id of a chunked message. All chunks of the same message share this
 * id.
 */
public class MessageId {

	private byte[] idBytes;

	public MessageId(byte[] idBytes) {
		this.idBytes = idBytes;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MessageId) {
			return Arrays.equals(idBytes, ((MessageId) obj).idBytes);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (byte b : idBytes) {
			hash = hash + b;
		}
		return hash;
	}
}
