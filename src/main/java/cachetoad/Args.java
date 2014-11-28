package cachetoad;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.kohsuke.args4j.Option;

public class Args {

	@Option(name = "-d", aliases = { "--dir" }, usage = "Local cache dir.", required = true) private String cacheDir;
	@Option(name = "-o", aliases = { "--origin" }, usage = "Origin server.", required = true) private String origin;
	@Option(name = "-p", aliases = { "--port" }, usage = "Bind port.", required = true) private int bindPort;

	public File getCacheDir () {
		return new File(this.cacheDir);
	}

	public URI getOrigin () throws URISyntaxException {
		return new URI(this.origin);
	}

	public int getBindPort () {
		if (this.bindPort < 1) throw new IllegalArgumentException("Invalid bind port.");
		return this.bindPort;
	}

	@Override
	public String toString () {
		return new StringBuilder("Args{")
				.append("dir=").append(this.cacheDir)
				.append(", origin=").append(this.origin)
				.append(", port=").append(this.bindPort)
				.append("}").toString();
	}

}
