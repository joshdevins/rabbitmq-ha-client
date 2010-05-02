package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;

import com.rabbitmq.client.ShutdownSignalException;

public abstract class AbstractStatefulConnectionListener
		implements
			StatefulConnectionListener {

	public void onConnect(final StatefulConnection connection) {
	}

	public void onConnectFailure(final StatefulConnection connection,
			final IOException ioException) {
	}

	public void onDisconnect(final StatefulConnection connection,
			final ShutdownSignalException shutdownSignalException) {
	}

	public void onDisconnectFailure(final StatefulConnection connection,
			final IOException ioException) {
	}

}
