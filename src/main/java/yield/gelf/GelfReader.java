package yield.gelf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import yield.core.EventListener;
import yield.core.EventSource;
import yield.core.EventType;
import yield.gelf.types.ChunkedMessage;
import yield.gelf.types.GelfMessage;
import yield.gelf.types.MessageId;
import yield.gelf.types.MessageType;
import yield.json.JsonEvent;

/**
 * Converts raw GELF messages to events.
 */
public class GelfReader extends EventSource<JsonEvent> implements
		EventListener<byte[]> {
	/**
	 * Holds incomplete messages. Chucks will be held up to 5 seconds. Messages
	 * will get dropped unless all chucks arrive within 5 seconds.
	 */
	Map<MessageId, List<ChunkedMessage>> incomplete = new ConcurrentHashMap<>();
	/**
	 * Arrival time of the first seen chunk of a message. The times are relative
	 * to whatever {@link System#nanoTime()} uses.
	 */
	Map<MessageId, Long> incompleteAges = new ConcurrentHashMap<>();

	public GelfReader() {
		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
				new Runnable() {

					/**
					 * Removes incomplete messages that did not get all chunks
					 * in time.
					 */
					@Override
					public void run() {
						long clearAge = System.nanoTime()
								- TimeUnit.SECONDS.toNanos(5);
						List<MessageId> toDrop = new ArrayList<>();
						for (Entry<MessageId, Long> incompleteMessage : incompleteAges
								.entrySet()) {
							if (incompleteMessage.getValue() < clearAge) {
								toDrop.add(incompleteMessage.getKey());
							}
						}

						for (MessageId timedOut : toDrop) {
							incomplete.remove(timedOut);
							incompleteAges.remove(timedOut);
						}
					}
				}, 5, 5, TimeUnit.SECONDS);
	}

	@Override
	public void feed(byte[] e) {
		GelfMessage gelfMessage = GelfMessage.forMessage(e);

		if (gelfMessage.getType() == MessageType.CHUNKED) {
			ChunkedMessage chunk = (ChunkedMessage) gelfMessage;
			MessageId messageId = chunk.getMessageId();
			List<ChunkedMessage> allChunks = incomplete.get(messageId);
			if (allChunks == null) {
				allChunks = new ArrayList<ChunkedMessage>();
				incomplete.put(messageId, allChunks);
				incompleteAges.put(messageId, System.nanoTime());
			}
			// Record current chunk.
			allChunks.add(chunk);

			if (chunk.getSequenceCount() == allChunks.size()) {
				// Assemble all chunks and yield event.

				incomplete.remove(messageId);
				incompleteAges.remove(messageId);

				Collections.sort(allChunks, new Comparator<ChunkedMessage>() {

					@Override
					public int compare(ChunkedMessage o1, ChunkedMessage o2) {
						return Integer.compare(o1.getSequenceNumber(),
								o2.getSequenceNumber());
					}
				});
				ByteArrayOutputStream out = new ByteArrayOutputStream(
						chunk.getSequenceCount() * 8192);
				for (ChunkedMessage singleMessage : allChunks) {
					try {
						out.write(singleMessage.getRawBody());
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
				String jsonSource = GelfMessage.forMessage(out.toByteArray())
						.getBody();
				// Yield assembles message.
				feedBoundQueues(new JsonEvent(jsonSource));
			}
		} else {
			// Yield message of received packet.
			feedBoundQueues(new JsonEvent(gelfMessage.getBody()));
		}
	}

	@Override
	@Nonnull
	public EventType getInputType() {
		return new EventType(byte[].class);
	}
}
