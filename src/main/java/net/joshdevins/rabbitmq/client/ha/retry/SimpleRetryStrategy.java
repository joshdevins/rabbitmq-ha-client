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

import net.joshdevins.rabbitmq.client.ha.BooleanReentrantLatch;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

/**
 * A {@link RetryStrategy} that uses a timeout between retry attempts. This
 * allows the reconnection that is happening elsewhere to kick in. The strategy
 * will retry operations up to {@link #setMaxOperationInvocations(int)} times.
 * If the timeout is set to 0, then the strategy will not actually sleep at all,
 * and the next operation invocation will happen immediately.
 * 
 * @author Josh Devins
 */
public class SimpleRetryStrategy implements RetryStrategy {

    private static final Logger LOG = Logger.getLogger(SimpleRetryStrategy.class);

    /**
     * Default value = 10000 = 10 seconds
     */
    public static final long DEFAULT_OPERATION_RETRY_TIMEOUT_MILLIS = 10000;

    /**
     * Default value = 2 (one retry)
     */
    public static final int DEFAULT_MAX_OPERATION_INVOCATIONS = 2;

    private long operationRetryTimeoutMillis = DEFAULT_OPERATION_RETRY_TIMEOUT_MILLIS;

    private int maxOperationInvocations = DEFAULT_MAX_OPERATION_INVOCATIONS;

    public void setMaxOperationInvocations(final int maxOperationInvocations) {

        Validate.isTrue(maxOperationInvocations >= 2,
                "max operation invocations must be 2 or greater, otherwise use a simpler strategy");
        this.maxOperationInvocations = maxOperationInvocations;
    }

    public void setOperationRetryTimeoutMillis(final long timeout) {

        Validate.isTrue(timeout >= 0, "timeout must be a positive number");
        operationRetryTimeoutMillis = timeout;
    }

    public boolean shouldRetry(final Exception e, final int numOperationInvocations,
            final BooleanReentrantLatch connectionGate) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation invocation failed on IOException: numOperationInvocations=" + numOperationInvocations
                    + ", maxOperationInvocations=" + maxOperationInvocations + ", message=" + e.getMessage());
        }

        if (numOperationInvocations == maxOperationInvocations) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Max number of operation invocations reached, not retrying: " + maxOperationInvocations);
            }

            return false;
        }

        if (operationRetryTimeoutMillis > 0) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Sleeping before next operation invocation (millis): " + operationRetryTimeoutMillis);
            }

            try {
                Thread.sleep(operationRetryTimeoutMillis);
            } catch (InterruptedException ie) {
                LOG.warn("Interrupted during timeout waiting for next operation invocation to occurr. "
                        + "Retrying invocation now.");
            }
        } else {

            if (LOG.isDebugEnabled()) {
                LOG.debug("No timeout set, retrying immediately");
            }
        }

        return true;
    }

}
