package net.joshdevins.rabbitmq.client.ha;

/**
 * A {@link RetryStrategy} that will never retry a failed operation.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public class NoRetryStrategy implements RetryStrategy {

	public boolean shouldRetry(final Exception e,
			final int numOperationInvocations, final BooleanReentrantLatch connectionGate) {
		return false;
	}
}
