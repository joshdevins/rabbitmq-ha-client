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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BlockingRetryStrategyTest {

    private BooleanReentrantLatch latch;

    private BlockingRetryStrategy strategy;

    @Test
    public void basicTest() throws InterruptedException {
        TestRunnable runnable = new TestRunnable();
        Thread thread = new Thread(runnable);
        thread.start();

        latch.open();

        // FIXME: peril!
        Thread.sleep(100);

        Assert.assertFalse(thread.isAlive());
        Assert.assertTrue(runnable.shouldRetry);
    }

    @Before
    public void before() {
        strategy = new BlockingRetryStrategy();
        latch = new BooleanReentrantLatch(false);
    }

    private class TestRunnable implements Runnable {

        private boolean shouldRetry = false;

        public void run() {

            // this will block until released by the other thread
            shouldRetry = strategy.shouldRetry(null, 0, latch);
        }
    }
}
