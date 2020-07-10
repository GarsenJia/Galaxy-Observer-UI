// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.integration.kryo;

import java.util.Arrays;
import java.util.Objects;

public record KryoGameInfo(int[] version, String gameName, boolean isPtr) {
	// overriding equals is necessary as the default record's one does not use Arrays.equals(.)
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final KryoGameInfo that = (KryoGameInfo) o;
		return isPtr == that.isPtr &&
				Arrays.equals(version, that.version) &&
				Objects.equals(gameName, that.gameName);
	}
	
}
