package net.joshdevins.rabbitmq.client.ha.it;

import net.joshdevins.rabbitmq.client.ha.AbstractHaConnectionListener;
import net.joshdevins.rabbitmq.client.ha.HaConnectionProxy;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class TestHaConnectionListener extends AbstractHaConnectionListener {

    private final ConnectionFactory connectionFactory;

    private final String host;

    TestHaConnectionListener(final ConnectionFactory connectionFactory, final String host) {

        this.connectionFactory = connectionFactory;
        this.host = host;
    }

    @Override
    public void onReconnection(final HaConnectionProxy connectionProxy) {

        // use a separate connection and channel to avoid race conditions with any operations that are blocked
        // waiting for the reconnection to finish...and don't cache anything otherwise you get the same problem!
        try {
            Connection connection = connectionFactory.newConnection(host);
            Channel channel = connection.createChannel();

            channel.queueDeclare("testQueue");
            channel.queueBind("testQueue", "amq.topic", "#");

            channel.close();
            connection.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
