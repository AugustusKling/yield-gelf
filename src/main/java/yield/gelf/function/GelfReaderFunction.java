package yield.gelf.function;

import java.util.Map;

import javax.annotation.Nonnull;

import yield.config.ConfigReader;
import yield.config.FunctionConfig;
import yield.config.TypedYielder;
import yield.core.Yielder;
import yield.gelf.GelfReader;
import yield.json.JsonEvent;

/**
 * Converts byte[] events to {@link JsonEvent}s.
 * 
 * @see GelfReceiverFunction GelfReceiverFunction if you want to read from the
 *      network.
 */
public class GelfReaderFunction extends FunctionConfig {

	@Override
	@Nonnull
	public TypedYielder getSource(String args, Map<String, TypedYielder> context) {
		Yielder<byte[]> input = getYielderTypesafe(byte[].class,
				ConfigReader.LAST_SOURCE, context);
		GelfReader reader = new GelfReader();
		input.bind(reader);
		return wrapResultingYielder(reader);
	}

}
