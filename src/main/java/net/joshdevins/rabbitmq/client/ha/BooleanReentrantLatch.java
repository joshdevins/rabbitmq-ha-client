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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A reentrant latch with simple open/closed semantics.
 * 
 * @author Josh Devins
 */
public class BooleanReentrantLatch {

    /**
     * Synchronization control for {@link BooleanReentrantLatch}. Uses {@link AbstractQueuedSynchronizer} state to
     * represent gate state.
     * 
     * <p>
     * states: open == 0, closed == 1
     * </p>
     */
    private static final class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = -7271227048279204885L;

        protected Sync(final boolean open) {
            setState(open ? 0 : 1);
        }

        @Override
        public boolean tryReleaseShared(final int releases) {

            // only open gate if it's currently closed (atomically)
            return compareAndSetState(1, 0);
        }

        protected boolean isOpen() {
            return getState() == 0;
        }

        @Override
        protected int tryAcquireShared(final int acquires) {

            // if acquires is 0, this is a test only not an acquisition attempt
            if(acquires == 0) {

                // if open, thread can proceed right away
                // if closed, thread needs to wait
                // this is a fake out since lock is not actually obtained
                return isOpen() ? 1 : -1;
            }

            // if acquires is 1, this is an acquisition attempt
            // close gate even if it's already closed
            setState(1);
            return 1;
        }
    }

    private final Sync sync;

    public BooleanReentrantLatch() {
        this(true);
    }

    public BooleanReentrantLatch(final boolean open) {
        sync = new Sync(open);
    }

    public void close() {
        sync.acquireShared(1);
    }

    /**
     * Equality is based on the current state of both latches. That is, they
     * must both be open or both closed.
     */
    @Override
    public boolean equals(final Object obj) {

        if(!(obj instanceof BooleanReentrantLatch)) {
            return false;
        }

        BooleanReentrantLatch rhs = (BooleanReentrantLatch) obj;
        return isOpen() == rhs.isOpen();
    }

    /**
     * For information purposes only. It is safest to call {@link #waitUntilOpen(long)}.
     */
    public boolean isClosed() {
        return !isOpen();
    }

    /**
     * For information purposes only. It is safest to call {@link #waitUntilOpen(long)}.
     */
    public boolean isOpen() {
        return sync.isOpen();
    }

    public void open() {
        sync.releaseShared(1);
    }

    /**
     * Returns a string identifying this latch, as well as its state: open or
     * closed.
     * 
     * @return a string identifying this latch, as well as its state
     */
    @Override
    public String toString() {
        return super.toString() + "[" + (isOpen() ? "open" : "closed") + "]";
    }

    /**
     * Waits for the gate to open. If this returns without an exception, the
     * gate is open.
     */
    public void waitUntilOpen() throws InterruptedException {
        sync.acquireSharedInterruptibly(0);
    }

    /**
     * Waits for the gate to open.
     * 
     * @return true if the gate is now open, false if the timeout was reached
     */
    public boolean waitUntilOpen(final long timeout, final TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(0, unit.toNanos(timeout));
    }
}
