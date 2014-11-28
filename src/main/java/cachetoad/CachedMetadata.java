package cachetoad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;

public class CachedMetadata {

	private static final String CACHE_CONTROL = "Cache-Control";
	private static final Pattern MAX_AGE_P = Pattern.compile("^.*max-age=([0-9]+).*$");

	public static int findMaxAgeSeconds (final HttpResponse resp) {
		final Header[] cacheCtls = resp.getHeaders(CACHE_CONTROL);
		for (int i = cacheCtls.length - 1; i <= 0; i--) {
			final Matcher m = MAX_AGE_P.matcher(cacheCtls[i].getValue());
			if (m.matches()) return Integer.parseInt(m.group(1));
		}
		return 0;
	}

	public static void toFile (final long maxAgeSeconds, final HttpResponse resp, final File file) throws IOException {
		try (final FileWriter w = new FileWriter(file)) {
			w.write(String.valueOf(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxAgeSeconds)));
			w.write("\n");
			w.write(String.valueOf(resp.getStatusLine().getStatusCode()));
			w.write("\n");
			for (final Header header : resp.getAllHeaders()) {
				w.write(header.getName());
				w.write(" ");
				w.write(header.getValue());
				w.write("\n");
			}
		}
	}

	public static CachedMetadata fromFile (final File file) throws IOException {
		try (final BufferedReader r = new BufferedReader(new FileReader(file))) {
			final long expiresMillis = Long.parseLong(r.readLine());
			final int status = Integer.parseInt(r.readLine());

			final List<Header> headers = new ArrayList<Header>();
			String line;
			while ((line = r.readLine()) != null) {
				if (line.length() < 1) continue;
				final int x = line.indexOf(" ");
				if (x < 1) continue;
				final String name = line.substring(0, x);
				final String value = line.substring(x + 1);
				headers.add(new BasicHeader(name, value));
			}

			return new CachedMetadata(expiresMillis, status, headers);
		}
	}

	private final long expiresMillis;
	private final int status;
	private final List<Header> headers;

	private CachedMetadata (final long expiresMillis, final int status, final List<Header> headers) {
		this.expiresMillis = expiresMillis;
		this.status = status;
		this.headers = headers;
	}

	public boolean isValid () {
		return this.expiresMillis > System.currentTimeMillis();
	}

	public void writeTo (final HttpServletResponse resp) {
		resp.setStatus(this.status);
		for (final Header header : this.headers) {
			resp.addHeader(header.getName(), header.getValue());
		}
	}

	public Header lastHeader (final String headerName) {
		for (int i = this.headers.size() - 1; i >= 0; i--) {
			final Header h = this.headers.get(i);
			if (headerName.equalsIgnoreCase(h.getName())) return h;
		}
		return null;
	}

}
