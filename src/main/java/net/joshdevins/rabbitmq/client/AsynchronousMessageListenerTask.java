package net.joshdevins.rabbitmq.client;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.QueueingConsumer.Delivery;

/**
 * A binding of an asynchronous processing task to a {@link MessageListener}
 * instance. The basic decoupling and abstraction of a {@link MessageListener}
 * to an asynchronous listener.
 * 
 * TODO: Transactions!
 */
public class AsynchronousMessageListenerTask implements Runnable {

	public static final boolean DEFAULT_AUTO_ACK = false;

	private final MessageListener messageListener;

	private final String queueName;

	private final QueueingConsumer consumer;

	private boolean autoAck = DEFAULT_AUTO_ACK;

	private boolean shutdown = false;

	public AsynchronousMessageListenerTask(final Channel channel,
			final MessageListener messageListener, final String queueName) {

		assert channel != null;
		assert messageListener != null;
		assert queueName != null && queueName.length() > 0;

		this.messageListener = messageListener;
		this.queueName = queueName;

		consumer = new QueueingConsumer(channel);
	}

	public void run() {

		try {
			consumer.getChannel().basicConsume(queueName, !autoAck, consumer);
		} catch (IOException ioException) {
			// TODO: What now?
		}

		while (!shutdown) {

			Delivery delivery = null;
			try {
				delivery = consumer.nextDelivery();

			} catch (ShutdownSignalException shutdownSignalException) {
				// TODO: Log this
				// TODO: Is this a valid assumption?
				// get out of here, the channel or connection is shutting down
				break;

			} catch (InterruptedException interruptedException) {
				// TODO: Log this
				// just continue and try again, or maybe we are shutting down
				continue;
			}

			InboundMessage message = new InboundMessage(delivery.getEnvelope(),
					delivery.getProperties(), delivery.getBody());

			try {
				messageListener.onMessage(message);

				if (!autoAck) {

					try {
						consumer.getChannel().basicAck(
								delivery.getEnvelope().getDeliveryTag(), false);
					} catch (IOException ioException) {
						// TODO What to do in this case? We weren't able to ack.
					}
				}

			} catch (RuntimeException re) {
				// no ACK will be made, TX should be rolled back
				// TODO: Any nACK?
			}
		}
	}

	public void setAutoAck(final boolean autoAck) {
		this.autoAck = autoAck;
	}

	/**
	 * Signals this instance to shutdown when it has the chance. This will not
	 * immediately kill anything.
	 */
	public void shutdown() {
		shutdown = true;
	}
}
