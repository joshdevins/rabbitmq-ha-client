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
import java.lang.reflect.Method;

import net.joshdevins.rabbitmq.client.ha.retry.RetryStrategy;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;

/**
 * A proxy around the standard {@link Channel}.
 * 
 * @author Josh Devins
 */
public class HaChannelProxy implements InvocationHandler {

    private static final Logger LOG = Logger.getLogger(HaChannelProxy.class);

    private final HaConnectionProxy connectionProxy;

    private Channel target;

    private final RetryStrategy retryStrategy;

    private final BooleanReentrantLatch connectionLatch;

    public HaChannelProxy(final HaConnectionProxy connectionProxy, final Channel target,
            final RetryStrategy retryStrategy) {

        assert connectionProxy != null;
        assert target != null;
        assert retryStrategy != null;

        this.connectionProxy = connectionProxy;
        this.target = target;
        this.retryStrategy = retryStrategy;

        connectionLatch = new BooleanReentrantLatch();
    }

    public void closeConnectionLatch() {
        connectionLatch.close();
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

        if(LOG.isDebugEnabled()) {
            LOG.debug("Invoke: " + method.getName());
        }

        // TODO: Rethink this assumption!
        // close is special since we can ignore failures safely
        if(method.getName().equals("close")) {
            try {
                target.close();
            } catch(Exception e) {

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Failed to close underlying channel, not a problem: " + e.getMessage());
                }
            }

            connectionProxy.removeClosedChannel(this);

            // FIXME: Is this the right return value for a void method?
            return null;
        }

        // invoke a method max times
        Exception lastException = null;
        boolean shutdownRecoverable = true;
        boolean keepOnInvoking = true;

        // don't check for open state, just let it fail
        // this will ensure that after a connection has been made, setup can
        // proceed before letting operations retry
        for(int numOperationInvocations = 1; keepOnInvoking && shutdownRecoverable; numOperationInvocations++) {

            // sych on target Channel to make sure it's not being replaced
            synchronized(target) {

                // delegate all other method invocations
                try {
                    return InvocationHandlerUtils.delegateMethodInvocation(method, args, target);

                    // deal with exceptions outside the synchronized block so
                    // that if a reconnection does occur, it can replace the
                    // target
                } catch(IOException ioe) {
                    lastException = ioe;
                    shutdownRecoverable = HaUtils.isShutdownRecoverable(ioe);

                } catch(AlreadyClosedException ace) {
                    lastException = ace;
                    shutdownRecoverable = HaUtils.isShutdownRecoverable(ace);
                }
            }

            // only keep on invoking if error is recoverable
            if(shutdownRecoverable) {

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Invocation failed, calling retry strategy: " + lastException.getMessage());
                }

                keepOnInvoking = retryStrategy.shouldRetry(lastException, numOperationInvocations, connectionLatch);
            }
        }

        if(shutdownRecoverable) {
            LOG.warn("Operation invocation failed after retry strategy gave up", lastException);
        } else {
            LOG.warn("Operation invocation failed with unrecoverable shutdown signal", lastException);
        }

        throw lastException;
    }

    protected Channel getTargetChannel() {
        return target;
    }

    protected void markAsClosed() {
        connectionLatch.close();
    }

    protected void markAsOpen() {
        connectionLatch.open();
    }

    protected void setTargetChannel(final Channel target) {

        assert target != null;

        if(LOG.isDebugEnabled() && this.target != null) {
            LOG.debug("Replacing channel: channel=" + this.target.toString());
        }

        synchronized(this.target) {

            this.target = target;

            if(LOG.isDebugEnabled() && this.target != null) {
                LOG.debug("Replaced channel: channel=" + this.target.toString());
            }
        }
    }
}
