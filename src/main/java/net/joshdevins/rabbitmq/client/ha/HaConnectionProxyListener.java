package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;

import com.rabbitmq.client.ShutdownSignalException;

public interface HaConnectionProxyListener {

	void onConnect(final HaConnectionProxy connectionProxy);

	void onConnectFailure(final HaConnectionProxy connectionProxy,
			final IOException ioException);

	void onDisconnect(final HaConnectionProxy connectionProxy,
			final ShutdownSignalException shutdownSignalException);

	void onDisconnectFailure(final HaConnectionProxy connectionProxy,
			final IOException ioException);
}
