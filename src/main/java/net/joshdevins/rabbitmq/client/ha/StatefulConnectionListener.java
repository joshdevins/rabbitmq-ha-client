package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;

import com.rabbitmq.client.ShutdownSignalException;

public interface StatefulConnectionListener {

	void onConnect(final StatefulConnection connection);

	void onConnectFailure(final StatefulConnection connection,
			final IOException ioException);

	void onDisconnect(final StatefulConnection connection,
			final ShutdownSignalException shutdownSignalException);

	void onDisconnectFailure(final StatefulConnection connection,
			final IOException ioException);
}
