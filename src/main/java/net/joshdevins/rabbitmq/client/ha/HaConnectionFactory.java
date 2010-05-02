package net.joshdevins.rabbitmq.client.ha;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.joshdevins.rabbitmq.client.ConnectionFactory;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import com.rabbitmq.client.Address;

public class HaConnectionFactory implements ConnectionFactory, InitializingBean {

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
		Validate.notEmpty(addresses, "One or more addresses are required");

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
	 * Create a set of {@link StatefulConnection}s for each address and add
	 * either a default {@link HaConnectionManager} as a listener or use the one
	 * provided with the {@link #setHaConnectionManager(HaConnectionManager)}
	 * property. Once the listener has been added, we can start the connection
	 * attempt.
	 */
	public void afterPropertiesSet() throws Exception {

		// create a default HaConnectionManager if not set
		if (haConnectionManager == null) {
			haConnectionManager = new HaConnectionManager();
		}

		Set<StatefulConnection> newConnections = new HashSet<StatefulConnection>(
				addresses.size());

		for (Address address : addresses) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating stateful connection for: "
						+ address.toString());
			}

			StatefulConnection connection = new StatefulConnection(
					connectionFactory, address);

			connection.addListener(haConnectionManager);
			connection.connect();

			newConnections.add(connection);
		}

		connections = Collections.unmodifiableSet(newConnections);
	}

	/**
	 * Creates and returns all stateful connections for the provided addresses.
	 * The state of the underlying connections is "new" only if
	 * {@link HaConnectionFactory#afterPropertiesSet()} has not been invoked. If
	 * it as been, then they are already attempting to connect to their
	 * destinations.
	 */
	public Set<StatefulConnection> getConnections() {
		return connections;
	}

	public void setHaConnectionManager(
			final HaConnectionManager haConnectionManager) {

		this.haConnectionManager = haConnectionManager;
	}
}
