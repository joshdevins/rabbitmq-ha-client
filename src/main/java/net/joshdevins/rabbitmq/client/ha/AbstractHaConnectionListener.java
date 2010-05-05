package net.joshdevins.rabbitmq.client.ha;

import com.rabbitmq.client.ShutdownSignalException;

public abstract class AbstractHaConnectionListener
		implements
			HaConnectionListener {

	public void onConnectFailure(final HaConnectionProxy connectionProxy,
			final Exception exception) {
	}

	public void onConnection(final HaConnectionProxy connectionProxy) {
	}

	public void onDisconnect(final HaConnectionProxy connectionProxy,
			final ShutdownSignalException shutdownSignalException) {
	}

	public void onReconnectFailure(final HaConnectionProxy connectionProxy,
			final Exception exception) {
	}

	public void onReconnection(final HaConnectionProxy connectionProxy) {
	}
}
