package cachetoad;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;

public class CacheExpirer<K> implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(CacheExpirer.class);

	private final Cache<K, ?> cache;
	private final DelayQueue<Expiry<K>> delayQueue = new DelayQueue<>();

	public CacheExpirer (final Cache<K, ?> cache) {
		this.cache = cache;
	}

	public void scheduleExpire (final K key, final long delaySeconds) {
		this.delayQueue.add(new Expiry<K>(TimeUnit.SECONDS.toNanos(delaySeconds) + now(), key));
	}

	@Override
	public void run () {
		try {
			while (true) {
				final Expiry<K> expired = this.delayQueue.take();
				this.cache.invalidate(expired.getKey());
			}
		}
		catch (final InterruptedException e) {
			LOG.error("Interupted.", e);
		}
	}

	private static class Expiry<K> implements Delayed {

		private final long time;
		private final K key;

		public Expiry (final long time, final K key) {
			this.time = time;
			this.key = key;
		}

		public K getKey () {
			return this.key;
		}

		@Override
		public int compareTo (final Delayed o) {
			if (o == this) return 0;
			final long d = (getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
			return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
		}

		@Override
		public long getDelay (final TimeUnit unit) {
			return unit.convert(this.time - now(), TimeUnit.NANOSECONDS);
		}

	}

	private static final long NANO_ORIGIN = System.nanoTime();

	protected static long now () {
		return System.nanoTime() - NANO_ORIGIN;
	}

}
