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

import org.apache.log4j.Logger;

/**
 * A simple retry strategy that waits on the connection gate to be opened before
 * retrying. There is no retry limit and no timeout on the connection gate.
 * 
 * @author Josh Devins
 */
public class BlockingRetryStrategy implements RetryStrategy {

    private static final Logger LOG = Logger.getLogger(BlockingRetryStrategy.class);

    public boolean shouldRetry(final Exception e, final int numOperationInvocations,
            final BooleanReentrantLatch connectionGate) {

        try {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Waiting for connection gate to open: no timeout - " + e.getMessage());
            }

            connectionGate.waitUntilOpen();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Waited for connection gate to open: connected=" + connectionGate.isOpen());
            }
        } catch (InterruptedException e1) {

            LOG
                    .warn("Interrupted during timeout waiting for next operation invocation to occurr. Retrying invocation now.");
        }

        // always retry
        // if connected finally, then of course, retry
        // if not connected, just retry again since there is no limit here
        return true;
    }
}
