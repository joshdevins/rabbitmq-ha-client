package net.joshdevins.spring.amqp.rabbitmq;

import java.io.IOException;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionParameters;

public class HaCachingConnectionFactory extends CachingConnectionFactory {

	private HaStatefulConnection haConnection;

	/**
	 * @see CachingConnectionFactory#CachingConnectionFactory(com.rabbitmq.client.ConnectionFactory,
	 *      String)
	 */
	public HaCachingConnectionFactory(
			final com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory,
			final String hostName) {

		super(rabbitConnectionFactory, hostName);
	}

	/**
	 * @see CachingConnectionFactory#CachingConnectionFactory(ConnectionParameters,
	 *      String)
	 */
	public HaCachingConnectionFactory(
			final ConnectionParameters connectionParameters,
			final String hostName) {

		super(connectionParameters, hostName);
	}

	/**
	 * @see CachingConnectionFactory#CachingConnectionFactory(String)
	 */
	public HaCachingConnectionFactory(final String hostName) {

		super(hostName);
	}

	/**
	 * Create an {@link HaStatefulConnection} based on the raw RabbitMQ
	 * {@link Connection} created by the {@link CachingConnectionFactory}.
	 */
	@Override
	protected Connection doCreateConnection() throws IOException {

		// new connection
		if (haConnection == null) {
			haConnection = new HaStatefulConnection(this);
			haConnection.connect();
		}

		// if this is an "existing" HA connection, then this is a reconnect

		return haConnection.getUnderlyingConnection();
	}
}
