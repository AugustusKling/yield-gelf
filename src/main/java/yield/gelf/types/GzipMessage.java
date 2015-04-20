package yield.gelf.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * GZipp'ed message.
 */
public class GzipMessage extends GelfMessage {

	private byte[] message;

	public GzipMessage(byte[] message) {
		super(message);
		this.message = message;
	}

	@Override
	public MessageType getType() {
		return MessageType.GZIP;
	}

	@Override
	public String getBody() {
		byte[] buffer = new byte[message.length];
		ByteArrayOutputStream out = new ByteArrayOutputStream(message.length);
		try {
			GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(
					message));
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			throw new RuntimeException("Decompressing GZip message failed.", e);
		}
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

}
