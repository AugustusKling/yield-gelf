package yield.gelf.function;

import java.net.InetSocketAddress;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import yield.config.FunctionConfig;
import yield.config.ParameterMap;
import yield.config.ParameterMap.Param;
import yield.config.ShortDocumentation;
import yield.config.TypedYielder;
import yield.core.EventQueue;
import yield.gelf.GelfReader;
import yield.gelf.TcpReader;
import yield.gelf.UdpReader;
import yield.json.JsonEvent;

/**
 * Starts listener to receive GELF events via TCP or UDP. Yield locally as
 * {@link JsonEvent}.
 */
public class GelfReceiverFunction extends FunctionConfig {
	private static enum Parameters implements Param {
		@ShortDocumentation(text = "Transport protocol. Either 'UDP' or 'TCP'.")
		protocol {
			@Override
			public Object getDefault() {
				return "TCP";
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
		ParameterMap<Parameters> parameters = parseArguments(args,
				Parameters.class);

		EventQueue<byte[]> receiver;
		GelfReader reader = new GelfReader();
		int port = parameters.getInteger(Parameters.port);
		switch (parameters.getString(Parameters.protocol)) {
		case "TCP":
			receiver = new TcpReader(new InetSocketAddress(port));
			break;
		case "UDP":
			receiver = new UdpReader(port);
			break;
		default:
			throw new IllegalArgumentException(
					"Only 'tcp' and 'udp' are permitted as 'protocol'. The argument 'protocol' is required.");
		}
		receiver.bind(reader);
		return wrapResultingYielder(reader);
	}

	@Override
	@Nullable
	public <Parameter extends Enum<Parameter> & Param> Class<? extends Param> getParameters() {
		return Parameters.class;
	}
}
