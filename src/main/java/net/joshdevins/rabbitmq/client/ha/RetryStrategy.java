package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;

public interface RetryStrategy {

	/**
	 * A simple retry handler/strategy callback method. This method can do
	 * whatever it likes including sleeping for some time, throw some
	 * {@link RuntimeException} or doing something else.
	 * 
	 * <p>
	 * A standard implementation will ship that retries N times and sleeps for T
	 * milliseconds between retries.
	 * </p>
	 * 
	 * <p>
	 * TODO: Add a hook to let the {@link HaConnectionFactory} notify locks when
	 * connections have been reestablished.
	 * </p>
	 * 
	 * @param e
	 *            The {@link Exception} thrown by the underlying {@link Channel}
	 *            . This will be either an {@link IOException} or an
	 *            {@link AlreadyClosedException}.
	 * @param numOperationInvocations
	 *            The number of operation invocations that have been made. This
	 *            will always be 1 or more.
	 * @param connectionGate
	 *            A gate that can be waited on to be opened. The gate is opened
	 *            when a connection is reestablished and the channel is ready
	 *            for use again.
	 * 
	 * @return true if the method should be invoked again on the same Channel,
	 *         false if we should fail and rethrow the {@link IOException}
	 */
	public boolean shouldRetry(Exception e, int numOperationInvocations,
			BooleanReentrantLatch connectionGate);
}
