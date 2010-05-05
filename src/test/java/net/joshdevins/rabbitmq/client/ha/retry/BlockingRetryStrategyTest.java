package net.joshdevins.rabbitmq.client.ha.retry;

import net.joshdevins.rabbitmq.client.ha.BooleanReentrantLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BlockingRetryStrategyTest {

	private class TestRunnable implements Runnable {

		private boolean shouldRetry = false;

		public void run() {

			// this will block until released by the other thread
			shouldRetry = strategy.shouldRetry(null, 0, latch);
		}
	}

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
}
