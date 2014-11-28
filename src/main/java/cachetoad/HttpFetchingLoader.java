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

	private CacheExpirer<CacheKey> cacheExpirer;

	public HttpFetchingLoader (final HttpClient httpClient, final URI origin) {
		this.httpClient = httpClient;
		this.origin = origin;
	}

	public void setCacheExpirer (final CacheExpirer<CacheKey> cacheExpirer) {
		this.cacheExpirer = cacheExpirer;
	}

	@Override
	public CachedMetadata load (final CacheKey key) throws IOException {
		if (!HttpMethod.GET.is(key.getMethod())) throw new IllegalStateException();

		String reason = "cold";
		if (key.isCached()) {
			reason = "expired";

			final CachedMetadata meta = CachedMetadata.fromFile(key.cacheFileHeaders());
			if (meta.isValid()) {
				final long secondsRemaining = meta.secondsRemaining();
				if (secondsRemaining < Main.CACHE_RECHECK_SECONDS) {
					this.cacheExpirer.scheduleExpire(key, secondsRemaining);
				}
				return meta;
			}
		}

		final long startTime = System.nanoTime();
		final Integer maxAgeSeconds = this.httpClient.execute(
				new HttpGet(UriHelper.appendPath(this.origin, key.getUri())),
				new CacheResponseHandler(key));
		final long fetchTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		LOG.info("{} ({}, fetch={}ms, max-age={}s)", key, reason, fetchTime, maxAgeSeconds);

		if (maxAgeSeconds < Main.CACHE_RECHECK_SECONDS) {
			this.cacheExpirer.scheduleExpire(key, Math.max(1, maxAgeSeconds));
		}

		return CachedMetadata.fromFile(key.cacheFileHeaders());
	}

}
