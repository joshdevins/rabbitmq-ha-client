package net.joshdevins.rabbitmq.client;

/**
 * Interface for ansynchronour message listeners. This is modelled after the JMS
 * class of the same name.
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public interface MessageListener {

	void onMessage(InboundMessage message);
}
