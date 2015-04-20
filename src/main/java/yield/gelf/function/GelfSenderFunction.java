package yield.gelf.function;

import java.net.InetSocketAddress;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.graylog2.gelfclient.GelfTransports;

import yield.config.ConfigReader;
import yield.config.FunctionConfig;
import yield.config.ParameterMap;
import yield.config.ParameterMap.Param;
import yield.config.ShortDocumentation;
import yield.config.TypedYielder;
import yield.core.Yielder;
import yield.gelf.GelfSender;
import yield.json.JsonEvent;

/**
 * Sends {@link JsonEvent}s via GELF to another host.
 */
public class GelfSenderFunction extends FunctionConfig {
	private static enum Parameters implements Param {
		@ShortDocumentation(text = "Transport protocol. Either 'UDP' or 'TCP'.")
		protocol {
			@Override
			public Object getDefault() {
				return "tcp";
			}
		},
		@ShortDocumentation(text = "Hostname of target. Specifies the host that shall receive the events.")
		targetHost {
			@Override
			public Object getDefault() {
				return "localhost";
			}
		},
		@ShortDocumentation(text = "Local port to listen on.")
		port {
			@Override
			public Object getDefault() {
				return 12201;
			}
		}
	}

	@Override
	@Nonnull
	public TypedYielder getSource(String args, Map<String, TypedYielder> context) {
		Yielder<JsonEvent> input = getYielderTypesafe(JsonEvent.class,
				ConfigReader.LAST_SOURCE, context);
		ParameterMap<Parameters> parameters = parseArguments(args,
				Parameters.class);
		GelfTransports transport = GelfTransports.valueOf(parameters
				.getString(Parameters.protocol));
		InetSocketAddress target = new InetSocketAddress(
				parameters.getString(Parameters.targetHost),
				parameters.getInteger(Parameters.port));
		input.bind(new GelfSender(transport, target));
		return wrapResultingYielder(input);
	}

	@Override
	@Nullable
	public <Parameter extends Enum<Parameter> & Param> Class<? extends Param> getParameters() {
		return Parameters.class;
	}
}
