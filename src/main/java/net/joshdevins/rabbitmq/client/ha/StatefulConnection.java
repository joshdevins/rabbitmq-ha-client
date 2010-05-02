package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Domain object encapsulting the concept of a connection which has state. Event
 * listeners trigger state changes.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public final class StatefulConnection implements ShutdownListener {

	private final ConnectionFactory connectionFactory;

	private final Address address;

	private final Set<StatefulConnectionListener> listeners;

	private Connection connection;

	public StatefulConnection(final ConnectionFactory connectionFactory,
			final Address address) {

		assert connectionFactory != null;
		assert address != null;

		this.connectionFactory = connectionFactory;
		this.address = address;
		listeners = new HashSet<StatefulConnectionListener>();
	}

	public void addListener(final StatefulConnectionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Attempts to establish a connection. This could be a new connection or a
	 * reconnection.
	 */
	public synchronized void connect() {

		if (connection != null) {
			throw new IllegalStateException(
					"Connection instance already exists.");
		}

		try {
			connection = connectionFactory.newConnection(address.getHost(),
					address.getPort());

			// redirect all underlying shutdown notifications to this instance
			connection.addShutdownListener(this);

		} catch (IOException ioException) {

			for (StatefulConnectionListener listener : listeners) {
				listener.onConnectFailure(this, ioException);
			}

			return;
		}

		for (StatefulConnectionListener listener : listeners) {
			listener.onConnect(this);
		}
	}

	/**
	 * @see Connection#createChannel()
	 */
	public synchronized Channel createChannel() throws IOException {
		return connection.createChannel();
	}

	/**
	 * @see Connection#createChannel(int)
	 */
	public synchronized Channel createChannel(final int channelNumber)
			throws IOException {
		return connection.createChannel(channelNumber);
	}

	/**
	 * @see Connection#close()
	 */
	public synchronized void disconnect() {
		disconnect(-1);
	}

	/**
	 * @see Connection#close(int)
	 */
	public synchronized void disconnect(final int timeout) {

		if (connection == null) {
			throw new IllegalStateException(
					"Connection instance does not exist.");
		}

		try {
			connection.close(timeout);
		} catch (IOException ioException) {

			for (StatefulConnectionListener listener : listeners) {
				listener.onDisconnectFailure(this, ioException);
			}
		}

		connection = null;
	}

	/**
	 * Based on the underlying {@link Address} only.
	 */
	@Override
	public boolean equals(final Object object) {

		if (!(object instanceof StatefulConnection)) {
			return false;
		}

		StatefulConnection rhs = (StatefulConnection) object;
		EqualsBuilder eb = new EqualsBuilder();
		eb.append(address, rhs.address);
		return eb.isEquals();
	}

	public Address getAddress() {
		return address;
	}

	/**
	 * Based on the underlying {@link Address} only.
	 */
	@Override
	public int hashCode() {

		HashCodeBuilder hcb = new HashCodeBuilder(572855331, 598333593);
		hcb.append(address);

		return hcb.toHashCode();
	}

	/**
	 * Differs from {@link #isOpen()} as it only checks if a connection exists,
	 * not that it is open and useable.
	 */
	public boolean isConnected() {
		return connection != null;
	}

	/**
	 * @see Connection#isOpen()
	 */
	public boolean isOpen() {
		return isConnected() && connection.isOpen();
	}

	public synchronized void shutdownCompleted(
			final ShutdownSignalException shutdownSignalException) {

		connection = null;

		for (StatefulConnectionListener listener : listeners) {
			listener.onDisconnect(this, shutdownSignalException);
		}
	}

	@Override
	public String toString() {

		ToStringBuilder tsb = new ToStringBuilder(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
		tsb.append("address", address);
		tsb.append("connected?", isConnected());
		tsb.append("open?", isOpen());

		return tsb.toString();
	}
}
