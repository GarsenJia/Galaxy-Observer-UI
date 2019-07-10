package interfacebuilder.integration.kryo;

import com.ahli.util.StringInterner;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ImmutableSerializer;

public class InternStringSerializer extends ImmutableSerializer<String> {
	
	public InternStringSerializer() {
		setAcceptsNull(true);
	}
	
	@Override
	public void write(final Kryo kryo, final Output output, final String object) {
		output.writeString(object);
	}
	
	@Override
	public String read(final Kryo kryo, final Input input, final Class<? extends String> type) {
		return StringInterner.intern(input.readString());
	}
	
}