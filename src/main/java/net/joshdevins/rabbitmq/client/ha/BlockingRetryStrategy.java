package net.joshdevins.rabbitmq.client.ha;

import org.apache.log4j.Logger;

/**
 * A simple retry strategy that waits on the connection gate to be opened before
 * retrying. There is no retry limit and no timeout on the connection gate.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public class BlockingRetryStrategy implements RetryStrategy {

	private static final Logger LOG = Logger
			.getLogger(BlockingRetryStrategy.class);

	public boolean shouldRetry(final Exception e,
			final int numOperationInvocations,
			final BooleanReentrantLatch connectionGate) {

		try {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Waiting for connection gate to open: no timeout");
			}

			connectionGate.waitUntilOpen();

			if (LOG.isDebugEnabled()) {
				LOG.debug("Waited for connection gate to open: connected=true");
			}
		} catch (InterruptedException e1) {

			LOG
					.warn("Interrupted during timeout waiting for next operation invocation to occurr. Retrying invocation now.");
		}

		// always retry
		// if connected finally, then of course, retry
		// if not connected, just retry again since there is no limit here
		return true;
	}
}
