package net.joshdevins.rabbitmq.client.ha;

import org.apache.log4j.Logger;

/**
 * A simple retry strategy that waits on the connection gate to be opened before
 * retrying. There is no retry limit.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public class BlockingRetryStrategy implements RetryStrategy {

	private static final Logger LOG = Logger
			.getLogger(BlockingRetryStrategy.class);

	/**
	 * Default value = 5000 = 5 secs.
	 */
	private static final long DEFAULT_TIMEOUT_MILLIS = 5000;

	private long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;

	public void setTimeoutMillis(final long timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
	}

	public boolean shouldRetry(final Exception e,
			final int numOperationInvocations, final BooleanGate connectionGate) {

		// just return if the gate is open
		// there is currently no retry limit
		try {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Waiting for connection gate to open: "
						+ timeoutMillis);
			}

			boolean connected = connectionGate.waitUntilOpen(timeoutMillis);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Waited for connection gate to open: connected="
						+ connected);
			}

			return !connected;
		} catch (InterruptedException e1) {

			LOG
					.warn("Interrupted during timeout waiting for next operation invocation to occurr. Retrying invocation now.");
			return true;
		}
	}
}
