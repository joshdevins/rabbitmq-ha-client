package net.joshdevins.rabbitmq.client.ha.retry;

import net.joshdevins.rabbitmq.client.ha.BooleanReentrantLatch;

/**
 * A {@link RetryStrategy} that will never retry a failed operation.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public class NeverRetryStrategy implements RetryStrategy {

	public boolean shouldRetry(final Exception e,
			final int numOperationInvocations,
			final BooleanReentrantLatch connectionGate) {
		return false;
	}
}
