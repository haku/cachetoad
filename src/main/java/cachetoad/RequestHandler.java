package cachetoad;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.LoadingCache;

class RequestHandler extends AbstractHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

	private final LoadingCache<CacheKey, CachedMetadata> cache;
	private final File baseDir;

	public RequestHandler (final LoadingCache<CacheKey, CachedMetadata> cache, final File baseDir) {
		this.cache = cache;
		this.baseDir = baseDir;
	}

	@Override
	public void handle (final String target, final Request baseRequest, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
		if (resp.isCommitted() || baseRequest.isHandled()) return;
		baseRequest.setHandled(true);

		final String method = req.getMethod();
		final String requestUri = req.getRequestURI();

		if (!HttpMethod.GET.is(method)) {
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

		try {
			final CacheKey key = new CacheKey(method, requestUri, this.baseDir);
			final CachedMetadata meta = this.cache.get(key);
			meta.writeTo(resp);

			int bodyLength = -1;
			try (final InputStream is = new FileInputStream(key.cacheFileBody())) {
				bodyLength = IOUtils.copy(is, resp.getOutputStream());
			}

			LOG.info("{} {} ({} bytes)", method, requestUri, bodyLength);
		}
		catch (final ExecutionException e) {
			throw new IOException(e); // FIXME
		}
	}
}
