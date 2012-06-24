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
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.joshdevins.rabbitmq.client.ha.retry.BlockingRetryStrategy;
import net.joshdevins.rabbitmq.client.ha.retry.RetryStrategy;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * A simple {@link ConnectionFactory} proxy that further proxies any created {@link Connection} and subsequent
 * {@link Channel}s. Sadly a dynamic proxy
 * cannot be used since the RabbitMQ {@link ConnectionFactory} does not have an
 * interface. As such, this class extends {@link ConnectionFactory} and
 * overrides necessary methods.
 * 
 * <p>
 * TODO: Create utility to populate some connections in the CachingConnectionFactory on startup. Should fail fast but
 * will reconnect using this underlying.
 * </p>
 * 
 * @author Josh Devins
 */
public class HaConnectionFactory extends ConnectionFactory {

    private class ConnectionSet {

        private final Connection wrapped;

        private final HaConnectionProxy proxy;

        private final HaShutdownListener listener;

        private ConnectionSet(final Connection wrapped, final HaConnectionProxy proxy, final HaShutdownListener listener) {

            this.wrapped = wrapped;
            this.proxy = proxy;
            this.listener = listener;
        }
    }

    /**
     * Listener to {@link Connection} shutdowns. Hooks together the {@link HaConnectionProxy} to the shutdown event.
     */
    private class HaShutdownListener implements ShutdownListener {

        private final HaConnectionProxy connectionProxy;

        // needs also to be able to call asyncReconnect or to create own
        // ReconnectionTask
        public HaShutdownListener(final HaConnectionProxy connectionProxy) {

            assert connectionProxy != null;
            this.connectionProxy = connectionProxy;
        }

        public void shutdownCompleted(final ShutdownSignalException shutdownSignalException) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Shutdown signal caught: " + shutdownSignalException.getMessage());
            }

            for (HaConnectionListener listener : listeners) {
                listener.onDisconnect(connectionProxy, shutdownSignalException);
            }

            // only try to reconnect if it was a problem with the broker
            if (!shutdownSignalException.isInitiatedByApplication()) {

                // start an async reconnection
                executorService.submit(new ReconnectionTask(true, this, connectionProxy));

            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring shutdown signal, application initiated");
                }
            }
        }
    }

    private class ReconnectionTask implements Runnable {

        private final boolean reconnection;

        private final ShutdownListener shutdownListener;

        private final HaConnectionProxy connectionProxy;

        public ReconnectionTask(final boolean reconnection, final ShutdownListener shutdownListener,
                final HaConnectionProxy connectionProxy) {

            Validate.notNull(shutdownListener, "shutdownListener is required");
            Validate.notNull(connectionProxy, "connectionProxy is required");

            this.reconnection = reconnection;
            this.shutdownListener = shutdownListener;
            this.connectionProxy = connectionProxy;
        }

        public void run() {

            // need to close the connection gate on the channels
            connectionProxy.closeConnectionLatch();

            String addressesAsString = getAddressesAsString();

            if (LOG.isDebugEnabled()) {
                LOG.info("Reconnection starting, sleeping: addresses=" + addressesAsString + ", wait="
                        + reconnectionWaitMillis);
            }

            // TODO: Add max reconnection attempts
            boolean connected = false;
            while (!connected) {

                try {
                    Thread.sleep(reconnectionWaitMillis);
                } catch (InterruptedException ie) {

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Reconnection timer thread was interrupted, ignoring and reconnecting now");
                    }
                }

                Exception exception = null;
                try {
                    Connection connection = newTargetConnection(connectionProxy.getAddresses());

                    if (LOG.isDebugEnabled()) {
                        LOG.info("Reconnection complete: addresses=" + addressesAsString);
                    }

                    connection.addShutdownListener(shutdownListener);

                    // refresh any channels created by previous connection
                    connectionProxy.setTargetConnection(connection);
                    connectionProxy.replaceChannelsInProxies();

                    connected = true;

                    if (reconnection) {
                        for (HaConnectionListener listener : listeners) {
                            listener.onReconnection(connectionProxy);
                        }

                    } else {
                        for (HaConnectionListener listener : listeners) {
                            listener.onConnection(connectionProxy);
                        }
                    }

                    connectionProxy.markAsOpen();

                } catch (ConnectException ce) {
                    // connection refused
                    exception = ce;

                } catch (IOException ioe) {
                    // some other connection problem
                    exception = ioe;
                }

                if (exception != null) {
                    LOG.warn("Failed to reconnect, retrying: addresses=" + addressesAsString + ", message="
                            + exception.getMessage());

                    if (reconnection) {
                        for (HaConnectionListener listener : listeners) {
                            listener.onReconnectFailure(connectionProxy, exception);
                        }

                    } else {
                        for (HaConnectionListener listener : listeners) {
                            listener.onConnectFailure(connectionProxy, exception);
                        }
                    }
                }
            }
        }

        private String getAddressesAsString() {

            StringBuilder sb = new StringBuilder();
            sb.append('[');

            for (int i = 0; i < connectionProxy.getAddresses().length; i++) {

                if (i > 0) {
                    sb.append(',');
                }

                sb.append(connectionProxy.getAddresses()[i].toString());
            }

            sb.append(']');
            return sb.toString();
        }
    }

    private static final Logger LOG = Logger.getLogger(HaConnectionFactory.class);

    /**
     * Default value = 1000 = 1 second
     */
    private static final long DEFAULT_RECONNECTION_WAIT_MILLIS = 1000;

    private long reconnectionWaitMillis = DEFAULT_RECONNECTION_WAIT_MILLIS;

    private final ExecutorService executorService;

    private RetryStrategy retryStrategy;

    private Set<HaConnectionListener> listeners;

    public HaConnectionFactory() {
        super();

        executorService = Executors.newCachedThreadPool();
        setDefaultRetryStrategy();

        // TODO: Should we use a concurrent instance or sync access to this Set?
        listeners = new HashSet<HaConnectionListener>();
    }

    public void addHaConnectionListener(final HaConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Wraps a raw {@link Connection} with an HA-aware proxy.
     * 
     * @see ConnectionFactory#newConnection(Address[], int)
     */
    @Override
    public Connection newConnection(final Address[] addrs) throws IOException {

        Connection target = null;
        try {
            target = super.newConnection(addrs);

        } catch (IOException ioe) {
            LOG.warn("Initial connection failed, wrapping anyways and letting reconnector go to work: "
                    + ioe.getMessage());
        }

        ConnectionSet connectionPair = createConnectionProxy(addrs, target);

        // connection success
        if (target != null) {
            return connectionPair.wrapped;

        }

        // connection failed, reconnect in the same thread
        ReconnectionTask task = new ReconnectionTask(false, connectionPair.listener, connectionPair.proxy);
        task.run();

        return connectionPair.wrapped;
    }

    /**
     * Allows setting a {@link Set} of {@link HaConnectionListener}s. This is
     * ammenable for Spring style property setting. Note that this will override
     * any existing listeners!
     */
    public void setHaConnectionListener(final Set<HaConnectionListener> listeners) {

        Validate.notEmpty(listeners, "listeners are required and none can be null");
        this.listeners = new ConcurrentSkipListSet<HaConnectionListener>(listeners);
    }

    /**
     * Set the reconnection wait time in milliseconds. The value must be greater
     * than 0. This is the number of milliseconds between getting a dropped
     * connection and a reconnection attempt.
     */
    public void setReconnectionWaitMillis(final long reconnectionIntervalMillis) {

        Validate.isTrue(reconnectionIntervalMillis > 0, "reconnectionIntervalMillis must be greater than 0");
        reconnectionWaitMillis = reconnectionIntervalMillis;
    }

    public void setRetryStrategy(final RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    /**
     * Creates an {@link HaConnectionProxy} around a raw {@link Connection}.
     */
    protected ConnectionSet createConnectionProxy(final Address[] addrs,
            final Connection targetConnection) {

        ClassLoader classLoader = Connection.class.getClassLoader();
        Class<?>[] interfaces = { Connection.class };

        HaConnectionProxy proxy = new HaConnectionProxy(addrs, targetConnection, retryStrategy);

        if (LOG.isDebugEnabled()) {
            LOG
                    .debug("Creating connection proxy: "
                            + (targetConnection == null ? "none" : targetConnection.toString()));
        }

        Connection target = (Connection) Proxy.newProxyInstance(classLoader, interfaces, proxy);
        HaShutdownListener listener = new HaShutdownListener(proxy);

        // failed initial connections will have this set later upon successful connection
        if (targetConnection != null) {
            target.addShutdownListener(listener);
        }

        return new ConnectionSet(target, proxy, listener);
    }

    private Connection newTargetConnection(final Address[] addrs) throws IOException {
        return super.newConnection(addrs);
    }

    private void setDefaultRetryStrategy() {
        retryStrategy = new BlockingRetryStrategy();
    }
}
