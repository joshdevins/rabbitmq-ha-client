package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import com.rabbitmq.client.ShutdownSignalException;

/**
 * For each {@link StatefulConnection} created, it needs to be maintained and
 * managed. This entity is responsible for just that. Notification of
 * {@link StatefulConnection} state changes are still handled by
 * {@link StatefulConnectionListener}s registered on a
 * {@link StatefulConnection}. This implementation is thread safe and one
 * instance can be used for all connections.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public class HaConnectionManager extends AbstractStatefulConnectionListener {

	/**
	 * Kicks off to time the reconnection attempt, then makes the reconnection
	 * attempt.
	 */
	private class ReconnectionTask implements Runnable {

		private final StatefulConnection connection;

		ReconnectionTask(final StatefulConnection connection) {

			assert connection != null;
			this.connection = connection;
		}

		public void run() {

			if (LOG.isDebugEnabled()) {
				LOG.info("Reconnection starting: address="
						+ connection.getAddress() + ", wait="
						+ reconnectionWaitMillis);
			}

			Thread.currentThread();
			try {
				Thread.sleep(reconnectionWaitMillis);
			} catch (InterruptedException ie) {

				if (LOG.isDebugEnabled()) {
					LOG
							.debug("Reconnection timer thread was interrupted, ignoring and reconnecting now");
				}
			}

			connection.connect();
		}
	}

	private static final Logger LOG = Logger
			.getLogger(HaConnectionManager.class);

	/**
	 * Default value = 10000 = 10 seconds
	 */
	private static final long DEFAULT_RECONNECTION_WAIT_MILLIS = 10000;

	private long reconnectionWaitMillis = DEFAULT_RECONNECTION_WAIT_MILLIS;

	private final ExecutorService executorService;

	public HaConnectionManager() {
		executorService = Executors.newCachedThreadPool();
	}

	@Override
	public void onConnectFailure(final StatefulConnection connection,
			final IOException ioException) {

		asyncReconnect(connection);
	}

	@Override
	public void onDisconnect(final StatefulConnection connection,
			final ShutdownSignalException shutdownSignalException) {

		asyncReconnect(connection);
	}

	@Override
	public void onDisconnectFailure(final StatefulConnection connection,
			final IOException ioException) {

		asyncReconnect(connection);
	}

	/**
	 * Set the reconnection wait time in milliseconds. The value must be greater
	 * than 0. This is the number of milliseconds between getting a dropped
	 * connection and a reconnection attempt.
	 */
	public void setReconnectionWaitMillis(final long reconnectionIntervalMillis) {

		Validate.isTrue(reconnectionIntervalMillis > 0,
				"reconnectionIntervalMillis must be greater than 0");
		reconnectionWaitMillis = reconnectionIntervalMillis;
	}

	private void asyncReconnect(final StatefulConnection connection) {
		executorService.submit(new ReconnectionTask(connection));
	}
}
