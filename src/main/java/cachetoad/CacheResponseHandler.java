package cachetoad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import com.google.common.io.Files;

class CacheResponseHandler implements ResponseHandler<Integer> {

	private final CacheKey key;

	public CacheResponseHandler (final CacheKey key) {
		this.key = key;
	}

	@Override
	public Integer handleResponse (final HttpResponse resp) throws IOException {
		final File tmpHeaders = this.key.tmpRecieveFileHeaders();
		final File tmpBody = this.key.tmpRecieveFileBody();
		boolean tmpHeadersDeleted = false;
		boolean tmpBodyDeleted = false;
		try {
			final int maxAgeSeconds = CachedMetadata.findMaxAgeSeconds(resp);
			CachedMetadata.toFile(maxAgeSeconds, resp, tmpHeaders);

			try (final OutputStream os = new FileOutputStream(tmpBody)) {
				IOUtils.copy(resp.getEntity().getContent(), os);
			}

			Files.move(tmpBody, this.key.cacheFileBody());
			tmpBodyDeleted = true;

			Files.move(tmpHeaders, this.key.cacheFileHeaders());
			tmpHeadersDeleted = true;

			return maxAgeSeconds;
		}
		finally {
			if (!tmpHeadersDeleted) tmpHeaders.delete();
			if (!tmpBodyDeleted) tmpBody.delete();
		}
	}

}