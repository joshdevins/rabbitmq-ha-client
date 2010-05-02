package net.joshdevins.rabbitmq.client;

import java.util.Set;

import net.joshdevins.rabbitmq.client.ha.StatefulConnection;

public interface ConnectionFactory {

	Set<StatefulConnection> getConnections();
}
