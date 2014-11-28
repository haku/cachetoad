package cachetoad;

import java.io.File;
import java.io.IOException;

import com.google.common.hash.Hashing;

public class CacheKey {

	private final String method;
	private final String uri;
	private final File baseDir;

	private String toString;
	private String id;
	private File cacheFileHeaders;
	private File cacheFileBody;

	public CacheKey (final String method, final String uri, final File baseDir) {
		this.method = method;
		this.uri = uri;
		this.baseDir = baseDir;
	}

	public String getMethod () {
		return this.method;
	}

	public String getUri () {
		return this.uri;
	}

	@Override
	public String toString () {
		if (this.toString == null) this.toString = String.format("%s %s", this.method, this.uri);
		return this.toString;
	}

	public String id () {
		if (this.id == null) this.id = Hashing.sha1().hashUnencodedChars(this.uri).toString(); // TODO include method?
		return this.id;
	}

	public boolean isCached () {
		return cacheFileHeaders().exists() && cacheFileBody().exists();
	}

	public File tmpRecieveFileHeaders () throws IOException {
		return File.createTempFile(id() + "h", ".part", this.baseDir);
	}

	public File tmpRecieveFileBody () throws IOException {
		return File.createTempFile(id() + "b", ".part", this.baseDir);
	}

	public File cacheFileHeaders () {
		if (this.cacheFileHeaders == null) this.cacheFileHeaders = new File(this.baseDir, id() + "h");
		return this.cacheFileHeaders;
	}

	public File cacheFileBody () {
		if (this.cacheFileBody == null) this.cacheFileBody = new File(this.baseDir, id() + "b");
		return this.cacheFileBody;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.uri == null) ? 0 : this.uri.hashCode());
		result = prime * result + ((this.method == null) ? 0 : this.method.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final CacheKey other = (CacheKey) obj;
		if (this.uri == null) {
			if (other.uri != null) return false;
		}
		else if (!this.uri.equals(other.uri)) return false;
		if (this.method == null) {
			if (other.method != null) return false;
		}
		else if (!this.method.equals(other.method)) return false;
		return true;
	}

}
