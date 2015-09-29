# yield-gelf
Extension for Yield to send and receive GELF log events.

Build JAR:
```
mvn assembly:single
```

Example usage:
```
# Load extension functions.
function gelf-send yield.gelf.function.GelfSenderFunction "file:/.../yield-gelf-1.0-SNAPSHOT-jar-with-dependencies.jar"
function gelf-receive yield.gelf.function.GelfReceiverFunction "file:/.../yield-gelf-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Get some events, for example monitor /tmp.
watch "/tmp"
# Send events to another host.
gelf-send protocol="UDP" targetHost="test.sample" port=12201

# Open port and listen for GELF messages.
gelf-receive protocol="UDP" port=12201
# Simply print to show test data was transferred.
print
```
