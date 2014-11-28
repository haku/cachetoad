package cachetoad;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private final Args args;

	private Main (final Args args) {
		this.args = args;
	}

	public static void main (final String[] rawArgs) {
		LOG.error("started.");

		final Args args = new Args();
		final CmdLineParser parser = new CmdLineParser(args);
		try {
			parser.parseArgument(rawArgs);
			LOG.info("args: {}", args);
			new Main(args).run();
		}
		catch (final Exception e) { // NOSONAR so we can cleanly print errors.
			System.err.println("An unhandled error occurred.");
			e.printStackTrace(System.err);

			System.err.print("Usage: ");
			parser.setUsageWidth(80);
			parser.printSingleLineUsage(System.err);
			System.err.println();
			parser.printUsage(System.err);
			System.err.println();
			System.exit(1);
		}
	}

	public void run () throws Exception {
		final HttpClient httpClient = HttpClientBuilder.create().build();

		final File baseDir = this.args.getCacheDir();
		if (!baseDir.mkdirs() && !baseDir.exists()) throw new IOException("Can not make: " + baseDir.getAbsolutePath());
		LOG.info("baseDir: {}", baseDir);
		final LoadingCache<CacheKey, Boolean> cache = CacheBuilder.newBuilder()
				// TODO expire rules!
				.expireAfterWrite(1, TimeUnit.HOURS)
				.build(new HttpFetcher(httpClient, this.args.getOrigin()));

		final QueuedThreadPool jettyThreadPool = new QueuedThreadPool();
		jettyThreadPool.setDaemon(true);
		jettyThreadPool.setName("jt");

		final Server server = new Server(jettyThreadPool);
		final ServerConnector connector = new ServerConnector(server);
		connector.setPort(this.args.getBindPort());
		server.addConnector(connector);
		server.setHandler(new RequestHandler(cache, baseDir));
		server.start();
		LOG.info("listening: {}", connector.getPort());

		new CountDownLatch(1).await();
	}

}
