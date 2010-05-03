package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;

import com.rabbitmq.client.ShutdownSignalException;

public abstract class AbstractHaConnectionProxyListener
		implements
			HaConnectionProxyListener {

	public void onConnect(final HaConnectionProxy connectionProxy) {
	}

	public void onConnectFailure(final HaConnectionProxy connectionProxy,
			final IOException ioException) {
	}

	public void onDisconnect(final HaConnectionProxy connectionProxy,
			final ShutdownSignalException shutdownSignalException) {
	}

	public void onDisconnectFailure(final HaConnectionProxy connectionProxy,
			final IOException ioException) {
	}

}
