package cachetoad;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

class HttpFetchingLoader extends CacheLoader<CacheKey, CachedMetadata> {

	private static final Logger LOG = LoggerFactory.getLogger(HttpFetchingLoader.class);

	private final HttpClient httpClient;
	private final URI origin;

	public HttpFetchingLoader (final HttpClient httpClient, final URI origin) {
		this.httpClient = httpClient;
		this.origin = origin;
	}

	@Override
	public CachedMetadata load (final CacheKey key) throws IOException {
		if (!HttpMethod.GET.is(key.getMethod())) throw new IllegalStateException();

		String reason = "cold";
		if (key.isCached()) {
			final CachedMetadata meta = CachedMetadata.fromFile(key.cacheFileHeaders());
			if (meta.isValid()) return meta;
			reason = "expired";
		}

		final long startTime = System.nanoTime();
		final Integer maxAgeSeconds = this.httpClient.execute(
				new HttpGet(UriHelper.appendPath(this.origin, key.getUri())),
				new CacheResponseHandler(key));
		final long fetchTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		LOG.info("{} ({}, fetch={}ms, max-age={}s)", key, reason, fetchTime, maxAgeSeconds);

		return CachedMetadata.fromFile(key.cacheFileHeaders());
	}
}
