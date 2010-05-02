package net.joshdevins.rabbitmq.client;

import org.apache.commons.lang.Validate;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

public final class InboundMessage extends Message {

	private final long deliveryTag;

	private final boolean redelivered;

	public InboundMessage(final Envelope envelope,
			final AMQP.BasicProperties properties, final byte[] body) {

		super(envelope == null ? null : envelope.getExchange(),
				envelope == null ? null : envelope.getRoutingKey(), properties,
				body);

		Validate.notNull(envelope, "envelope is required");

		deliveryTag = envelope.getDeliveryTag();
		redelivered = envelope.isRedeliver();
	}

	public long getDeliveryTag() {
		return deliveryTag;
	}

	public boolean isRedeliver() {
		return redelivered;
	}
}
