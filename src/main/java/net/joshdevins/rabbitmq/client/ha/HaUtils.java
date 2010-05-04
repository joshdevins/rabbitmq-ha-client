package net.joshdevins.rabbitmq.client.ha;

import java.io.EOFException;
import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.AMQImpl;

public final class HaUtils {

	private HaUtils() {
		// do not instantiate
	}

	/**
	 * Pulls out the cause of the {@link IOException} and if it is of type
	 * {@link ShutdownSignalException}, passes on to
	 * {@link #isShutdownRecoverable(ShutdownSignalException)}.
	 */
	public static boolean isShutdownRecoverable(final IOException ioe) {

		if (ioe.getCause() instanceof ShutdownSignalException) {
			return !isShutdownRecoverable((ShutdownSignalException) ioe
					.getCause());
		}

		return true;
	}

	/**
	 * Determines if the {@link ShutdownSignalException} can be recovered from.
	 * 
	 * Straight code copy from RabbitMQ messagepatterns library v0.1.3 {@code
	 * ConnectorImpl}.
	 */
	public static boolean isShutdownRecoverable(final ShutdownSignalException s) {

		if (s != null) {
			int replyCode = 0;

			if (s.getReason() instanceof AMQImpl.Connection.Close) {
				replyCode = ((AMQImpl.Connection.Close) s.getReason())
						.getReplyCode();
			}

			return s.isInitiatedByApplication()
					&& (replyCode == AMQP.CONNECTION_FORCED
							|| replyCode == AMQP.INTERNAL_ERROR || s.getCause() instanceof EOFException);
		}

		return false;
	}
}
