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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * A proxy around the standard {@link Consumer}.
 * 
 * @author Josh Devins
 */
public class HaConsumerProxy implements Consumer {

    private class ConsumeRunner implements Callable<Object> {

        public Object call() throws Exception {

            try {
                return channelProxy.invoke(channelProxy, basicConsumeMethod, basicConsumeArgs);

            } catch (Throwable e) {

                // bad news?
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error reinvoking basicConsume", e);
                }

                return e;
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(HaConsumerProxy.class);

    private final Consumer target;

    private final HaChannelProxy channelProxy;

    private final Method basicConsumeMethod;

    private final Object[] basicConsumeArgs;

    private final ExecutorService executor;

    public HaConsumerProxy(final Consumer target, final HaChannelProxy channelProxy, final Method basicConsumeMethod,
            final Object[] basicConsumeArgs) {

        assert target != null;
        assert channelProxy != null;
        assert basicConsumeMethod != null;
        assert basicConsumeArgs != null;

        this.target = target;
        this.channelProxy = channelProxy;
        this.basicConsumeMethod = basicConsumeMethod;
        this.basicConsumeArgs = basicConsumeArgs;

        executor = Executors.newCachedThreadPool();
    }

	public void handleCancel(final String consumerTag) throws IOException {
		target.handleCancel(consumerTag);
	}

    public void handleCancelOk(final String consumerTag) {
        target.handleCancelOk(consumerTag);
    }

    public void handleConsumeOk(final String consumerTag) {
        target.handleConsumeOk(consumerTag);
    }

    public void handleDelivery(final String consumerTag, final Envelope envelope, final BasicProperties properties,
            final byte[] body) throws IOException {
        target.handleDelivery(consumerTag, envelope, properties, body);
    }

	public void handleRecoverOk(final String consumerTag) {
		target.handleRecoverOk(consumerTag);
	}

    public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig) {

        // this is why we wrapped this
        if (LOG.isDebugEnabled()) {
            LOG.debug("Consumer asked to handle shutdown signal, reregistering consume. " + sig.getMessage());
        }

        // just poke back in and call basicConsume all over again with target
        // make sure to close the connected gate
        channelProxy.closeConnectionLatch();

        // this is it?
        executor.submit(new ConsumeRunner());
    }
}
