/*
 * Copyright 2010 Josh Devins
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.joshdevins.rabbitmq.client.ha;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP.Queue.BindOk;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/META-INF/spring/applicationContext.xml")
public class RabbitTemplateConsumerIntegrationTest {

    private static class TestChannelCallback implements ChannelCallback<BindOk> {

        /*
         * TODO: Document: If you use an auto-delete queue, you need to recreate it too, but this can fail depending
         * on the retry strategy being used since a consume method could be invoked in a separate thread from the queue
         * creation. Furthermore, using any blocking retry strategy on the same channel that was reconnected will cause
         * a race condition -- which will get called first, the consume message or the queueDecalre/Binding? Or just
         * don't use an auto-delete queue!
         */
        public BindOk doInRabbit(final Channel channel) throws Exception {

            // bind to the default topic and consume all messages
            channel.queueDeclare("testQueue");
            return channel.queueBind("testQueue", "amq.topic", "#");
        }
    }

    private static class TestHaConnectionListener extends AbstractHaConnectionListener {

        private final ConnectionFactory connectionFactory;

        private final String host;

        private TestHaConnectionListener(final ConnectionFactory connectionFactory, final String host) {

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

    private static final Logger LOG = Logger.getLogger(RabbitTemplateConsumerIntegrationTest.class);

    @Autowired
    private HaConnectionFactory haConnectionFactory;

    @Autowired
    private RabbitTemplate template;

    @Before
    public void before() {

        // add my connection listener to the HaConnectionFactory
        haConnectionFactory.addHaConnectionListener(new TestHaConnectionListener(haConnectionFactory,
                "devins-ubuntu-vm01"));
    }

    @Test
    public void testSyncConsume() throws UnsupportedEncodingException, InterruptedException {

        BindOk bindOk = template.execute(new TestChannelCallback());

        // test for not null or something?
        Assert.assertNotNull(bindOk);

        // empty out queue
        while (template.receive("testQueue") != null) {
            receiveMessage(1, template.receive("testQueue"));
        }

        receiveMessage(1, template.receive("testQueue"));

        // empty out queue
        while (true) {
            Thread.sleep(1000);
            receiveMessage(1, template.receive("testQueue"));
        }
    }

    private void receiveMessage(final int expected, final Message message) throws UnsupportedEncodingException {

        if (message == null) {
            LOG.info("no message");
            return;
        }

        String str = new String(message.getBody(), "UTF-8");
        LOG.info(str);
    }
}
