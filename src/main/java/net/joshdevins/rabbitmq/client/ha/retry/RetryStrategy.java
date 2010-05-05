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

package net.joshdevins.rabbitmq.client.ha.retry;

import java.io.IOException;

import net.joshdevins.rabbitmq.client.ha.BooleanReentrantLatch;
import net.joshdevins.rabbitmq.client.ha.HaConnectionFactory;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;

/**
 * Retry strategy for failed {@link Channel} operations.
 * 
 * @author Josh Devins
 */
public interface RetryStrategy {

    /**
     * A simple retry handler/strategy callback method. This method can do
     * whatever it likes including sleeping for some time, throw some {@link RuntimeException} or doing something else.
     * 
     * <p>
     * A standard implementation will ship that retries N times and sleeps for T milliseconds between retries.
     * </p>
     * 
     * <p>
     * TODO: Add a hook to let the {@link HaConnectionFactory} notify locks when connections have been reestablished.
     * </p>
     * 
     * @param e
     *        The {@link Exception} thrown by the underlying {@link Channel}. This will be either an {@link IOException}
     *        or an {@link AlreadyClosedException}.
     * @param numOperationInvocations
     *        The number of operation invocations that have been made. This will always be 1 or more.
     * @param connectionGate
     *        A gate that can be waited on to be opened. The gate is opened when a connection is reestablished and the
     *        channel is ready for use again.
     * 
     * @return true if the method should be invoked again on the same Channel,
     *         false if we should fail and rethrow the {@link IOException}
     */
    public boolean shouldRetry(Exception e, int numOperationInvocations, BooleanReentrantLatch connectionGate);
}
