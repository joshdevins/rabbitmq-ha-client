package net.joshdevins.rabbitmq.client.ha.it;

import org.springframework.amqp.rabbit.core.ChannelCallback;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP.Queue.BindOk;

public class TestChannelCallback implements ChannelCallback<BindOk> {

    /*
     * TODO: Document: If you use an auto-delete queue, you need to recreate it too, but this can fail depending
     * on the retry strategy being used since a consume method could be invoked in a separate thread from the queue
     * creation. Furthermore, using any blocking retry strategy on the same channel that was reconnected will cause
     * a race condition -- which will get called first, the consume message or the queueDecalre/Binding? Or just
     * don't use an auto-delete queue!
     */
    public BindOk doInRabbit(final Channel channel) throws Exception {

        // bind to the default topic and consume all messages
        channel.queueDeclare("testQueue", true);
        return channel.queueBind("testQueue", "amq.topic", "#");
    }
}
