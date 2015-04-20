package yield.gelf.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;

/**
 * Deflated message.
 */
public class ZlibMessage extends GelfMessage {

	private byte[] message;

	public ZlibMessage(byte[] message) {
		super(message);
		this.message = message;
	}

	@Override
	public MessageType getType() {
		return MessageType.ZLIB;
	}

	@Override
	public String getBody() {
		byte[] buffer = new byte[message.length];
		ByteArrayOutputStream out = new ByteArrayOutputStream(message.length);
		InflaterInputStream in = new InflaterInputStream(
				new ByteArrayInputStream(message));
		int bytesRead;
		try {
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			throw new RuntimeException("Decompressing ZLib message failed.", e);
		}
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}
}
