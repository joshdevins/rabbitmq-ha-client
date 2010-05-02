package net.joshdevins.spring.amqp.rabbitmq;

import java.io.IOException;

import com.rabbitmq.client.ShutdownSignalException;

public interface HaStatefulConnectionListener {

	void onConnect(final HaStatefulConnection connection);

	void onConnectFailure(final HaStatefulConnection connection,
			final IOException ioException);

	void onDisconnect(final HaStatefulConnection connection,
			final ShutdownSignalException shutdownSignalException);

	void onDisconnectFailure(final HaStatefulConnection connection,
			final IOException ioException);
}
