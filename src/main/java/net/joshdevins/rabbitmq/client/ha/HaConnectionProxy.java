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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import net.joshdevins.rabbitmq.client.ha.retry.RetryStrategy;

import org.apache.log4j.Logger;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

/**
 * A proxy around the standard {@link Connection}.
 * 
 * TODO: Catch close method on Connection and Channel to do cleanup.
 * 
 * @author Josh Devins
 */
public class HaConnectionProxy implements InvocationHandler {

    private static final Logger LOG = Logger.getLogger(HaConnectionProxy.class);

    private static Method CREATE_CHANNEL_METHOD;

    private static Method CREATE_CHANNEL_INT_METHOD;

    private final Address[] addrs;

    private Connection target;

    private final Set<HaChannelProxy> channelProxies;

    private final RetryStrategy retryStrategy;

    public HaConnectionProxy(final Address[] addrs, final Connection target,
            final RetryStrategy retryStrategy) {

        assert addrs != null;
        assert addrs.length > 0;
        assert retryStrategy != null;

        this.target = target;
        this.addrs = addrs;
        this.retryStrategy = retryStrategy;

        channelProxies = new HashSet<HaChannelProxy>();
    }

    public void closeConnectionLatch() {
        for (HaChannelProxy proxy : channelProxies) {
            proxy.closeConnectionLatch();
        }
    }

    public Address[] getAddresses() {
        return addrs;
    }

    public Connection getTargetConnection() {
        return target;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

        // intercept calls to create a channel
        if (method.equals(CREATE_CHANNEL_METHOD) || method.equals(CREATE_CHANNEL_INT_METHOD)) {

            return createChannelAndWrapWithProxy(method, args);
        }

        // delegate all other method invocations
        return InvocationHandlerUtils.delegateMethodInvocation(method, args, target);
    }

    public void markAsOpen() {

        synchronized (channelProxies) {

            for (HaChannelProxy proxy : channelProxies) {
                proxy.markAsOpen();
            }
        }
    }

    protected Channel createChannelAndWrapWithProxy(final Method method, final Object[] args)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        Channel targetChannel = (Channel) method.invoke(target, args);

        ClassLoader classLoader = Connection.class.getClassLoader();
        Class<?>[] interfaces = { Channel.class };

        // create the proxy and add to the set of channels we have created
        HaChannelProxy proxy = new HaChannelProxy(this, targetChannel, retryStrategy);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating channel proxy: " + targetChannel.toString());
        }

        // save the channel number-to-proxy relationship to be replaced later
        synchronized (channelProxies) {
            channelProxies.add(proxy);
        }

        return (Channel) Proxy.newProxyInstance(classLoader, interfaces, proxy);
    }

    protected void removeClosedChannel(final HaChannelProxy channelProxy) {
        synchronized (channelProxies) {
            channelProxies.remove(channelProxy);
        }
    }

    protected void replaceChannelsInProxies() throws IOException {

        synchronized (channelProxies) {

            for (HaChannelProxy proxy : channelProxies) {

                // replace dead channel with a new one using the same ID
                int channelNumber = proxy.getTargetChannel().getChannelNumber();
                proxy.setTargetChannel(target.createChannel(channelNumber));
            }
        }
    }

    protected void setTargetConnection(final Connection target) {

        assert target != null;
        this.target = target;
    }

    static {

        // initialize static fields or fail fast
        try {
            CREATE_CHANNEL_METHOD = Connection.class.getMethod("createChannel");
            CREATE_CHANNEL_INT_METHOD = Connection.class.getMethod("createChannel", int.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
