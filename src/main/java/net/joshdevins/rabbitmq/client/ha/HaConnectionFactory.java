package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * A simple {@link ConnectionFactory} proxy that further proxies any created
 * {@link Connection} and subsequent {@link Channel}s. Sadly a dynamic proxy
 * cannot be used since the RabbitMQ {@link ConnectionFactory} does not have an
 * interface. As such, this class extends {@link ConnectionFactory} and
 * overrides necessary methods.
 * 
 * <p>
 * TODO: Create utility to populate some connections in the
 * CachingConnectionFactory on startup. Should fail fast but will reconnect
 * using this underlying.
 * </p>
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public class HaConnectionFactory extends ConnectionFactory {

	/**
	 * Listener to {@link Connection} shutdowns. Hooks together the
	 * {@link HaConnectionProxy} to the shutdown event.
	 */
	private class HaShutdownListener implements ShutdownListener {

		private final HaConnectionProxy connectionProxy;

		public HaShutdownListener(final HaConnectionProxy connectionProxy) {

			assert connectionProxy != null;
			this.connectionProxy = connectionProxy;
		}

		public void shutdownCompleted(
				final ShutdownSignalException shutdownSignalException) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Shutdown signal caught: "
						+ shutdownSignalException.getMessage());
			}

			// only try to reconnect if it was a problem with the broker
			if (!shutdownSignalException.isInitiatedByApplication()) {
				asyncReconnect(this, connectionProxy);

			} else {
				if (LOG.isDebugEnabled()) {
					LOG
							.debug("Ignoring shutdown signal, application initiated");
				}
			}
		}
	}

	/**
	 * Kicks off to time the reconnection attempt, then makes the reconnection
	 * attempt.
	 */
	private class ReconnectionTask implements Runnable {

		private final ShutdownListener listener;

		private final HaConnectionProxy connectionProxy;

		ReconnectionTask(final ShutdownListener listener,
				final HaConnectionProxy connectionProxy) {

			assert listener != null;
			assert connectionProxy != null;

			this.listener = listener;
			this.connectionProxy = connectionProxy;
		}

		public void run() {

			// need to close the connection gate on the channels
			connectionProxy.closeConnectionLatch();

			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (int i = 0; i < connectionProxy.getKnownHosts().length; i++) {

				if (i > 0) {
					sb.append(',');
				}

				sb.append(connectionProxy.getKnownHosts()[i].toString());
			}
			sb.append(']');

			String addressesAsString = sb.toString();

			if (LOG.isDebugEnabled()) {
				LOG.info("Reconnection starting, sleeping: addresses="
						+ addressesAsString + ", wait="
						+ reconnectionWaitMillis);
			}

			// TODO: Add max reconnection attempts
			boolean connected = false;
			while (!connected) {

				Thread.currentThread();
				try {
					Thread.sleep(reconnectionWaitMillis);
				} catch (InterruptedException ie) {

					if (LOG.isDebugEnabled()) {
						LOG
								.debug("Reconnection timer thread was interrupted, ignoring and reconnecting now");
					}
				}

				Exception exception = null;
				try {
					Connection connection;
					if (connectionProxy.getMaxRedirects() == null) {
						connection = newTargetConnection(connectionProxy
								.getKnownHosts(), 0);
					} else {
						connection = newTargetConnection(connectionProxy
								.getKnownHosts(), connectionProxy
								.getMaxRedirects());
					}

					if (LOG.isDebugEnabled()) {
						LOG.info("Reconnection complete: addresses="
								+ addressesAsString);
					}

					connection.addShutdownListener(listener);

					// refresh any channels created by previous connection
					connectionProxy.setTargetConnection(connection);
					connectionProxy.replaceChannelsInProxies();

					connected = true;

				} catch (ConnectException ce) {
					// connection refused
					exception = ce;

				} catch (IOException ioe) {
					// some other connection problem
					exception = ioe;
				}

				if (exception != null) {
					LOG.warn("Failed to reconnect, retrying: addresses="
							+ addressesAsString + ", message="
							+ exception.getMessage());
				}
			}
		}
	}

	private static final Logger LOG = Logger
			.getLogger(HaConnectionFactory.class);

	/**
	 * Default value = 1000 = 1 second
	 */
	private static final long DEFAULT_RECONNECTION_WAIT_MILLIS = 1000;

	private long reconnectionWaitMillis = DEFAULT_RECONNECTION_WAIT_MILLIS;

	private final ExecutorService executorService;

	private RetryStrategy retryStrategy;

	public HaConnectionFactory() {
		super();
		executorService = Executors.newCachedThreadPool();
	}

	public HaConnectionFactory(final ConnectionParameters params) {
		super(params);
		executorService = Executors.newCachedThreadPool();
	}

	/**
	 * Wraps a raw {@link Connection} with an HA-aware proxy.
	 * 
	 * @see ConnectionFactory#newConnection(Address[], int)
	 */
	@Override
	public Connection newConnection(final Address[] addrs,
			final int maxRedirects) throws IOException {

		return createConnectionProxy(maxRedirects, super.newConnection(addrs,
				maxRedirects));
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

	public void setRetryStrategy(final RetryStrategy retryStrategy) {
		this.retryStrategy = retryStrategy;
	}

	/**
	 * Creates an {@link HaConnectionProxy} around a raw {@link Connection}.
	 */
	protected Connection createConnectionProxy(final Integer maxRedirects,
			final Connection targetConnection) {

		ClassLoader classLoader = Connection.class.getClassLoader();
		Class<?>[] interfaces = {Connection.class};

		HaConnectionProxy proxy = new HaConnectionProxy(maxRedirects,
				targetConnection, retryStrategy);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Creating connection proxy: "
					+ targetConnection.toString());
		}

		Connection target = (Connection) Proxy.newProxyInstance(classLoader,
				interfaces, proxy);
		target.addShutdownListener(new HaShutdownListener(proxy));

		return target;
	}

	private void asyncReconnect(final ShutdownListener listener,
			final HaConnectionProxy connectionProxy) {

		executorService.submit(new ReconnectionTask(listener, connectionProxy));
	}

	private Connection newTargetConnection(final Address[] addrs,
			final int maxRedirects) throws IOException {

		return super.newConnection(addrs, maxRedirects);
	}
}
