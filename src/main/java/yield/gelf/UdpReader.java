package yield.gelf;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import yield.core.EventQueue;
import yield.input.network.ConnectionFailed;

/**
 * Listens for UDP traffic and emits message payload.
 */
public class UdpReader extends EventQueue<byte[]> {
	/**
	 * Limit of IP payload is 65535, IP uses 20 bytes and UDP 8 bytes, which
	 * leaves 65507 bytes as payload of a UDP datagram. However the usable limit
	 * is highly dependent on the routing path which 8 KB being commonly usable.
	 */
	private static final int MAX_UDP_PAYLOAD = 65507;

	public UdpReader(final int port) {
		new Thread() {
			@Override
			public void run() {
				try (DatagramSocket socket = new DatagramSocket(port)) {
					byte[] buf = new byte[MAX_UDP_PAYLOAD];
					while (true) {
						DatagramPacket packet = new DatagramPacket(buf,
								buf.length);
						socket.receive(packet);
						feed(Arrays.copyOfRange(packet.getData(), 0,
								packet.getLength()));
					}
				} catch (IOException e) {
					getControlQueue().feed(
							new ConnectionFailed(
									"Could not listen/read on port " + port
											+ ".", e));
				}
			};
		}.start();
	}
}
