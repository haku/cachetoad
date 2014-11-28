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

class HttpFetcher extends CacheLoader<CacheKey, Boolean> {

	private static final Logger LOG = LoggerFactory.getLogger(HttpFetcher.class);

	private final HttpClient httpClient;
	private final URI origin;

	public HttpFetcher (final HttpClient httpClient, final URI origin) {
		this.httpClient = httpClient;
		this.origin = origin;
	}

	@Override
	public Boolean load (final CacheKey key) throws IOException {
		if (key.isCached()) return Boolean.TRUE;
		if (!HttpMethod.GET.is(key.getMethod())) throw new IllegalStateException();

		long startTime = System.nanoTime();
		this.httpClient.execute(new HttpGet(UriHelper.appendPath(this.origin, key.getUri())), new CacheResponseHandler(key));
		LOG.info("{} ({}ms)", key, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
		return Boolean.TRUE;
	}
}