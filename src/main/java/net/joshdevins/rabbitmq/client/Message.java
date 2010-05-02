package net.joshdevins.rabbitmq.client;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rabbitmq.client.AMQP;

/**
 * A domain object representing the most basic parts of an AMQP message.
 * Producer/outbound and consumer/inbound messages will provide additional
 * attributes.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public abstract class Message {

	private final String exchangeName;

	private final String routingKey;

	private final AMQP.BasicProperties properties;

	private final byte[] body;

	public Message(final String exchangeName, final String routingKey,
			final AMQP.BasicProperties properties, final byte[] body) {

		// TODO: is only exchnage name required in a message?
		// only exchange name is required
		Validate.notEmpty(exchangeName, "exchangeName is required");

		this.exchangeName = exchangeName;
		this.routingKey = routingKey;
		this.properties = properties;
		this.body = body;
	}

	/**
	 * Based on all properties.
	 */
	@Override
	public boolean equals(final Object object) {
		return EqualsBuilder.reflectionEquals(this, object);
	}

	public byte[] getBody() {
		return body;
	}
	public String getExchangeName() {
		return exchangeName;
	}

	public AMQP.BasicProperties getProperties() {
		return properties;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	/**
	 * Based on all properties.
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder
				.reflectionHashCode(-334466181, -1967723775, this);
	}

	/**
	 * Based on all properties.
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
