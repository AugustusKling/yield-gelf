package yield.gelf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import yield.core.EventQueue;
import yield.input.FeedingPrevented;
import yield.input.ListenerExecutionAborted;
import yield.input.network.ConnectionFailed;

/**
 * Listens for TCP traffic and emits message payload.
 */
public class TcpReader extends EventQueue<byte[]> {
	public TcpReader(final InetSocketAddress listeningAddress) {
		super(byte[].class);

		new Thread() {
			@Override
			public void run() {
				// Start listening for connections.
				final AsynchronousServerSocketChannel listener;
				try {
					listener = AsynchronousServerSocketChannel.open().bind(
							listeningAddress);
				} catch (IOException e2) {
					getControlQueue().feed(
							new ConnectionFailed(listeningAddress.toString(),
									e2));
					return;
				}

				// Allow clients to connect.
				listener.accept(
						null,
						new CompletionHandler<AsynchronousSocketChannel, Void>() {

							@Override
							public void completed(AsynchronousSocketChannel ch,
									Void attachment) {
								// Accept the next connection.
								listener.accept(null, this);

								// Allocate a byte buffer (4K) to read from the
								// client
								ByteBuffer byteBuffer = ByteBuffer
										.allocate(4096);
								try {
									// Give client 5s to send data once
									// connection was established.
									int bytesRead = ch.read(byteBuffer).get(20,
											TimeUnit.SECONDS);

									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									while (bytesRead != -1) {
										// Make the buffer ready to read
										byteBuffer.flip();

										// Convert the buffer into a line
										byte[] receivedBytes = new byte[bytesRead];
										byteBuffer.get(receivedBytes, 0,
												bytesRead);

										// Bytes need to be copied separately
										// because GELF uses a NULL byte to
										// separate JSON messages from each
										// other. There is no header available
										// that would specify the message
										// length.
										// This does also mean that only GZip'ed
										// and deflated data cannot be received
										// via TCP because those algorithms
										// might create NULL bytes in their
										// headers.
										//
										// https://github.com/Graylog2/graylog2-server/issues/127
										// https://github.com/t0xa/gelfj/pull/61
										for (byte b : receivedBytes) {
											if (b == 0) {
												// Yield event because
												// end-of-message marker
												// encountered.
												feed(bos.toByteArray());
												bos.reset();
											} else {
												// Append to current message
												// fragment.
												bos.write(b);
											}
										}

										// Make the buffer ready to write
										byteBuffer.clear();

										// Read the next line
										bytesRead = ch.read(byteBuffer).get(20,
												TimeUnit.SECONDS);
									}

								} catch (InterruptedException e) {
									getControlQueue().feed(
											new ListenerExecutionAborted());
								} catch (ExecutionException e) {
									getControlQueue().feed(
											new FeedingPrevented(e));
								} catch (TimeoutException e) {
									// Client did not send event within 5s
									// limit.
									getControlQueue()
											.feed(new ConnectionFailed(
													listeningAddress.toString(),
													e));
								}

								try {
									// Close the connection if we need to
									if (ch.isOpen()) {
										ch.close();
									}
								} catch (IOException e) {
									getControlQueue()
											.feed(new ConnectionFailed(
													listeningAddress.toString(),
													e));
								}
							}

							@Override
							public void failed(Throwable e, Void attachment) {
								getControlQueue().feed(
										new ConnectionFailed(listeningAddress
												.toString(), new Exception(e)));
							}
						});
			};
		}.start();
	}

}
