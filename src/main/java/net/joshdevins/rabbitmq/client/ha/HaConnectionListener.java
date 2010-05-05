package net.joshdevins.rabbitmq.client.ha;

import com.rabbitmq.client.ShutdownSignalException;

public interface HaConnectionListener {

	void onConnectFailure(final HaConnectionProxy connectionProxy,
			final Exception exception);

	void onConnection(final HaConnectionProxy connectionProxy);

	void onDisconnect(final HaConnectionProxy connectionProxy,
			final ShutdownSignalException shutdownSignalException);

	void onReconnectFailure(final HaConnectionProxy connectionProxy,
			final Exception exception);

	void onReconnection(final HaConnectionProxy connectionProxy);
}
