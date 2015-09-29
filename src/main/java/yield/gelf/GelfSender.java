package yield.gelf;

import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;

import yield.core.BaseControlQueueProvider;
import yield.core.EventListener;
import yield.core.EventType;
import yield.input.ListenerExceutionFailed;
import yield.input.ListenerExecutionAborted;
import yield.json.JsonEvent;

/**
 * Forwards events over the network as GELF messages.
 */
public class GelfSender extends BaseControlQueueProvider implements
		EventListener<JsonEvent> {

	private GelfTransport transport;
	private boolean blocking = true;

	public GelfSender(GelfTransports transport, InetSocketAddress target) {
		final GelfConfiguration config = new GelfConfiguration(target)
				.transport(transport).queueSize(512).connectTimeout(5000)
				.reconnectDelay(1000).tcpNoDelay(true).sendBufferSize(32768);

		this.transport = GelfTransports.create(config);
	}

	@Override
	public void feed(JsonEvent e) {
		String shortMessage = e.get("message");
		if (shortMessage == null) {
			shortMessage = "";
		}
		String host = e.get("host");
		if (host == null) {
			host = "?";
		}
		final GelfMessageBuilder builder = new GelfMessageBuilder(shortMessage,
				host);
		for (Entry<String, String> entry : e) {
			switch (entry.getKey()) {
			case "full_message":
				builder.fullMessage(e.get("full_message"));
				break;
			case "timestamp":
				String timestamp = e.get("timestamp");
				if (timestamp == null) {
					builder.timestamp(Calendar.getInstance().getTimeInMillis() / 1000.0);
				} else {
					// Convert millisecond timestamp that Yield used to second
					// based timestamp of GELF.
					builder.timestamp(Double.parseDouble(timestamp) / 1000.0);
				}
			case "level":
				try {
					GelfMessageLevel level = GelfMessageLevel.valueOf(e
							.get("level"));
					builder.level(level);
				} catch (IllegalArgumentException ex) {
					// Happens if valueOf-call fails.

					try {
						int numericLevel = Integer.parseInt(e.get("level"));
						GelfMessageLevel level = GelfMessageLevel
								.fromNumericLevel(numericLevel);
						builder.level(level);
					} catch (NumberFormatException ex2) {
						// Leave off level. It cannot be mapped to GELF's syslog
						// based levels and the level is optional, too.
					}
				}
				break;
			default:
				// GELF expects all key to be prefixed with an underscore unless
				// there are part of the GELF specification.
				String gelfKey;
				if (entry.getKey().startsWith("_")) {
					gelfKey = entry.getKey();
				} else {
					gelfKey = "_" + entry.getKey();
				}
				// GELF restricts field to those where the name matches
				// ^[\w\.\-]*$ and does not allow for fields to be named _id.
				if (gelfKey.matches("^[\\w\\.\\-]*$") && !"_id".equals(gelfKey)) {
					builder.additionalField(gelfKey, entry.getValue());
				}
			}
		}
		final GelfMessage message = builder.build();
		if (blocking) {
			// Blocks until there is capacity in the queue
			try {
				transport.send(message);
			} catch (InterruptedException e1) {
				getControlQueue().feed(new ListenerExecutionAborted());
			}
		} else {
			// Returns false if there isn't enough room in the queue
			boolean enqueued = transport.trySend(message);
			if (!enqueued) {
				getControlQueue().feed(
						new ListenerExceutionFailed<JsonEvent>(e,
								"Buffer queue exceeded maximum size."));
			}
		}
	}

	@Override
	@Nonnull
	public EventType getInputType() {
		return new EventType(JsonEvent.class);
	}
}
