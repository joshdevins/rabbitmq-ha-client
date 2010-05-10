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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rabbitmq.client.AMQP.Queue.BindOk;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/META-INF/spring/applicationContext.xml")
public class RabbitTemplateConsumerIntegrationTest {

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
