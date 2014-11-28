package cachetoad;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final Args args;

    private Main(final Args args) {
        this.args = args;
    }

    public static void main(final String[] rawArgs) {
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

    public void run() throws Exception {
        final File baseDir = new File("/tmp/cachetoad");
        if (!baseDir.mkdirs() && !baseDir.exists()) throw new IOException("Can not make: " + baseDir.getAbsolutePath());
        LOG.info("baseDir: {}", baseDir);
        final LoadingCache<String, File> cache = CacheBuilder.newBuilder().build(new HttpFetcher(baseDir));

        final QueuedThreadPool jettyThreadPool = new QueuedThreadPool();
        jettyThreadPool.setDaemon(true);
        jettyThreadPool.setName("jt");
        final Server server = new Server(jettyThreadPool);
        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(13355);
        server.addConnector(connector);
        server.setHandler(new MyHandler(cache));
        server.start();
        LOG.info("listening: {}", connector.getPort());

        new CountDownLatch(1).await();
    }

    private static class MyHandler extends AbstractHandler {

        private static final Logger LOG = LoggerFactory.getLogger(Main.MyHandler.class);

        private final LoadingCache<String, File> cache;

        public MyHandler(final LoadingCache<String, File> cache) {
            this.cache = cache;
        }

        @Override
        public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
            if (response.isCommitted() || baseRequest.isHandled()) return;
            baseRequest.setHandled(true);

            final String method = request.getMethod();
            final String requestUri = request.getRequestURI();

            final File file;
            try {
                file = this.cache.get(requestUri); // TODO add method into key, etc.
            }
            catch (final ExecutionException e) {
                throw new IOException(e); // FIXME
            }

            LOG.info("{} {}", method, requestUri);

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("not found desu~");
        }
    }

    private static class HttpFetcher extends CacheLoader<String, File> {

        private final File baseDir;

        public HttpFetcher(final File baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public File load(final String uri) throws Exception {
            final File f = new File(this.baseDir, Hashing.sha1().hashUnencodedChars(uri).toString());
            LOG.warn("TODO {} --> {}", uri, f);
            return f;
        }

    }

}
