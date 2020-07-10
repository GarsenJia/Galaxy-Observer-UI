package interfacebuilder.integration.kryo.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import interfacebuilder.integration.kryo.KryoGameInfo;

public class KryoGameInfoSerializer extends Serializer<KryoGameInfo> {
	
	@Override
	public void write(final Kryo kryo, final Output output, final KryoGameInfo object) {
		output.setVariableLengthEncoding(false);
		output.writeInts(object.version(), 0, 4, true);
		output.writeString(object.gameName());
		output.writeBoolean(object.isPtr());
	}
	
	@Override
	public KryoGameInfo read(final Kryo kryo, final Input input, final Class<? extends KryoGameInfo> type) {
		input.setVariableLengthEncoding(false);
		final int[] version = input.readInts(4, true);
		final String gameName = input.readString();
		final boolean isPtr = input.readBoolean();
		return new KryoGameInfo(version, gameName, isPtr);
	}
	
}
