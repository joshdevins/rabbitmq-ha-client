package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
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
 * @author devins
 */
public class HaConnectionFactory extends ConnectionFactory
		implements
			InitializingBean {

	protected class HaConnectionInvocationHandler implements InvocationHandler {

		private final Connection target;

		public HaConnectionInvocationHandler(final Connection target) {

			assert target != null;
			this.target = target;
		}

		public Object invoke(final Object proxy, final Method method,
				final Object[] args) throws Throwable {

			// TODO Auto-generated method stub
			return null;
		}
	}

	protected class HaConnectionShutdownListener implements ShutdownListener {

		public void shutdownCompleted(final ShutdownSignalException cause) {
			// TODO Auto-generated method stub

		}
	}

	private static final Logger LOG = Logger
			.getLogger(HaConnectionFactory.class);

	private final com.rabbitmq.client.ConnectionFactory connectionFactory;

	private final Set<Address> addresses;

	private HaConnectionManager haConnectionManager;

	private Set<StatefulConnection> connections;

	public HaConnectionFactory(
			final com.rabbitmq.client.ConnectionFactory connectionFactory,
			final Address... addresses) {

		Validate.notNull(connectionFactory, "connectionFactory is required");
		Validate.notEmpty(addresses, "one or more addresses are required");

		this.connectionFactory = connectionFactory;

		// ensure no duplicate addresses
		Set<Address> dedupedAddresses = new HashSet<Address>(addresses.length);
		dedupedAddresses.addAll(Arrays.asList(addresses));

		this.addresses = dedupedAddresses;
	}

	public HaConnectionFactory(
			final com.rabbitmq.client.ConnectionFactory connectionFactory,
			final String addresses) {

		this(connectionFactory, Address.parseAddresses(addresses));
	}

	/**
	 * Creates a default {@link HaConnectionManager} if none was set as a
	 * property.
	 */
	public void afterPropertiesSet() throws Exception {

		// create a default HaConnectionManager if not set
		if (haConnectionManager == null) {
			haConnectionManager = new HaConnectionManager();
		}

		// Set<StatefulConnection> newConnections = new
		// HashSet<StatefulConnection>(
		// addresses.size());
		//
		// for (Address address : addresses) {
		//
		// if (LOG.isDebugEnabled()) {
		// LOG.debug("Creating stateful connection for: "
		// + address.toString());
		// }
		//
		// StatefulConnection connection = new StatefulConnection(
		// connectionFactory, address);
		//
		// connection.addListener(haConnectionManager);
		// connection.connect();
		//
		// newConnections.add(connection);
		// }
		//
		// connections = Collections.unmodifiableSet(newConnections);
	}

	/**
	 * @see ConnectionFactory#newConnection(Address[])
	 */
	@Override
	public Connection newConnection(final Address[] addrs) throws IOException {

		return createConnectionProxy(super.newConnection(addrs));
	}

	/**
	 * @see ConnectionFactory#newConnection(Address[], int)
	 */
	@Override
	public Connection newConnection(final Address[] addrs,
			final int maxRedirects) throws IOException {

		return createConnectionProxy(super.newConnection(addrs, maxRedirects));
	}

	/**
	 * @see ConnectionFactory#newConnection(String)
	 */
	@Override
	public Connection newConnection(final String hostName) throws IOException {

		return createConnectionProxy(super.newConnection(hostName));
	}

	/**
	 * @see ConnectionFactory#newConnection(String, int)
	 */
	@Override
	public Connection newConnection(final String hostName, final int portNumber)
			throws IOException {

		return createConnectionProxy(super.newConnection(hostName, portNumber));
	}

	public void setHaConnectionManager(
			final HaConnectionManager haConnectionManager) {

		this.haConnectionManager = haConnectionManager;
	}

	@SuppressWarnings("unchecked")
	protected Connection createConnectionProxy(final Connection targetConnection) {

		ClassLoader classLoader = Connection.class.getClassLoader();
		Class[] interfaces = {Connection.class};
		InvocationHandler invocationHandler = new HaConnectionInvocationHandler(
				targetConnection);

		return (Connection) Proxy.newProxyInstance(classLoader, interfaces,
				invocationHandler);
	}
}
