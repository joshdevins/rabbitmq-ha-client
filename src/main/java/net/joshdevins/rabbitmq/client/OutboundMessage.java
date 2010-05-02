package net.joshdevins.rabbitmq.client;

import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * Simple wrapper object since an outbound message is the same as the root
 * abstract type.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public final class OutboundMessage extends Message {

	public OutboundMessage(final String exchangeName, final String routingKey,
			final BasicProperties properties, final byte[] body) {

		super(exchangeName, routingKey, properties, body);
	}
}
