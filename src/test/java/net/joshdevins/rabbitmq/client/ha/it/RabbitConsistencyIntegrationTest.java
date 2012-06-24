package net.joshdevins.rabbitmq.client.ha.it;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rabbitmq.client.AMQP.Queue.BindOk;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/META-INF/spring/applicationContext.xml")
public class RabbitConsistencyIntegrationTest {

    public class PojoHandler {

        public void handleMessage(final String message) {

            if (LOG.isInfoEnabled()) {
                LOG.info("Received message: " + message);
            }

            // check for message
            Integer integer = Integer.valueOf(message);

            if (!messages.contains(integer)) {
                messagesReceivedNotDelivered.add(integer);
                return;
            }

            messages.remove(integer);
        }
    }

    private static final Logger LOG = Logger.getLogger(RabbitConsistencyIntegrationTest.class);

    private static final int NUM_MESSAGES = 1000;

    private static final int NUM_CONSUMERS = 1;

    private Set<Integer> messages;

    private List<Integer> messagesReceivedNotDelivered;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitTemplate template;

    @Before
    public void before() {
        messages = Collections.synchronizedSet(new HashSet<Integer>());
        messagesReceivedNotDelivered = Collections.synchronizedList(new LinkedList<Integer>());
    }

    @Test
    public void test() throws InterruptedException {

        // ensure queue already created
        BindOk bindOk = template.execute(new TestChannelCallback());
        Assert.assertNotNull(bindOk);

        // setup async container
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("testQueue");
        container.setConcurrentConsumers(NUM_CONSUMERS);
        // container.setChannelTransacted(true);

        MessageListenerAdapter adapter = new MessageListenerAdapter();
        adapter.setDelegate(new PojoHandler());
        container.setMessageListener(adapter);
        container.afterPropertiesSet();

        // empty out queue
        while (template.receive("testQueue") != null) {
            // do
        }

        container.start();

        // template.setChannelTransacted(true);

        // send all the messages
        long start = new Date().getTime();

        for (int i = 0; i < NUM_MESSAGES; i++) {

            messages.add(i);
            template.convertAndSend(Integer.toString(i));
            Thread.sleep(10);
        }

        long end = new Date().getTime();
        float time = (end - start) / (float) 1000;

        Thread.sleep(1000);

        LOG.debug("Time (secs.): " + time);
        LOG.debug("Avg. rate (msgs/sec): " + NUM_MESSAGES / time);

        // everything should be empty
        Assert.assertEquals(0, messagesReceivedNotDelivered.size());
        Assert.assertEquals(0, messages.size());
    }
}
