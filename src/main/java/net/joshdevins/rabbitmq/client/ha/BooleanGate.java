package net.joshdevins.rabbitmq.client.ha;

import java.util.concurrent.atomic.AtomicBoolean;

public class BooleanGate {

	private final AtomicBoolean open;

	public BooleanGate() {
		this(true);
	}

	public BooleanGate(final boolean initialState) {
		open = new AtomicBoolean(initialState);
	}

	public void close() {
		open.set(false);
	}

	/**
	 * For information purposes only. It is safest to call
	 * {@link #waitUntilOpen(long)}.
	 */
	public boolean isClosed() {
		return !isOpen();
	}

	/**
	 * For information purposes only. It is safest to call
	 * {@link #waitUntilOpen(long)}.
	 */
	public boolean isOpen() {
		return open.get();
	}

	public void open() {
		open.set(true);
		open.notifyAll();
	}

	/**
	 * Waits for the gate to open.
	 * 
	 * <p>
	 * This is a naïve implementation in that there is a race condition.
	 * Internally this checks to see if the gate is open and if not, will put
	 * the thread into a wait. If the gate happens to open after initially
	 * checking the state but before going into the wait, the thread could wait
	 * forever (since nothing will notify it). To avoid this, use short/decent
	 * timeouts such that if the race condition occurs, the timeout is short
	 * enough that there is little consequence to hitting the race condition.
	 * </p>
	 * 
	 * @return true if the gate is now open, false if the timeout was reached
	 */
	public boolean waitUntilOpen(final long timeout)
			throws InterruptedException {

		// gate is open, no need to wait
		if (isOpen()) {
			return true;
		}

		// race condition if the gate opens before getting here
		open.wait(timeout);

		return isOpen();
	}
}
