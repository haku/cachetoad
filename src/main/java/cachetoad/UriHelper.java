package cachetoad;

import java.net.URI;
import java.net.URISyntaxException;

public class UriHelper {

	private UriHelper () {
		throw new AssertionError();
	}

	public static URI appendPath (final URI u, final String path) {
		try {
			return new URI(u.getScheme(), u.getUserInfo(), u.getHost(), u.getPort(), u.getPath() + path, u.getQuery(), u.getFragment());
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Failed to append path '" + path + "' to URI '" + u + "'.", e);
		}
	}

}
