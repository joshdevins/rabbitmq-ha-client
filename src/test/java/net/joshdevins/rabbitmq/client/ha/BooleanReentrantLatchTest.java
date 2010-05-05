package net.joshdevins.rabbitmq.client.ha;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BooleanReentrantLatchTest {

	private class TestCallable implements Callable<Long> {

		public Long call() throws Exception {

			try {

				long startTime = new Date().getTime();
				latch.waitUntilOpen();
				return new Date().getTime() - startTime;

			} catch (InterruptedException ie) {
				return new Long(-1);
			}
		}
	}

	private static final int NUM_REPETITIONS = 3;

	private BooleanReentrantLatch latch;

	private long startTime;

	@Test
	public void basicInitialStateTest_Closed() {

		latch = new BooleanReentrantLatch(false);
		BooleanReentrantLatch localLatch = new BooleanReentrantLatch(false);

		Assert.assertEquals(latch, localLatch);

		Assert.assertFalse(localLatch.isOpen());
		Assert.assertTrue(localLatch.isClosed());
	}

	@Test
	public void basicInitialStateTest_Open() {

		BooleanReentrantLatch localLatch = new BooleanReentrantLatch(true);

		Assert.assertEquals(latch, localLatch);

		Assert.assertTrue(localLatch.isOpen());
		Assert.assertFalse(localLatch.isClosed());
	}

	@Test
	public void basicRepeatedCloseTest() {

		latch = new BooleanReentrantLatch(false);
		Assert.assertTrue(latch.isClosed());

		for (int i = 0; i < NUM_REPETITIONS; i++) {
			latch.close();
			Assert.assertTrue(latch.isClosed());
		}
	}

	@Test
	public void basicRepeatedCloseThenOpenTest() {

		for (int i = 0; i < NUM_REPETITIONS; i++) {

			Assert.assertTrue(latch.isOpen());

			latch.close();
			Assert.assertTrue(latch.isClosed());

			latch.open();
		}
	}

	@Test
	public void basicRepeatedOpenTest() {

		Assert.assertTrue(latch.isOpen());

		for (int i = 0; i < NUM_REPETITIONS; i++) {
			latch.open();
			Assert.assertTrue(latch.isOpen());
		}
	}

	@Test
	public void basicWaitOnOpenLatchTest() throws InterruptedException {

		setStartTime();
		latch.waitUntilOpen();

		// should return right away
		assertTimeLapsed();
	}

	@Test
	public void basicWaitOnOpenLatchTest_WithTimeout()
			throws InterruptedException {

		setStartTime();
		latch.waitUntilOpen(1000, TimeUnit.MILLISECONDS);

		// should return right away
		assertTimeLapsed();
	}

	@Before
	public void before() {
		latch = new BooleanReentrantLatch();
	}

	@Test
	public void concurrentRepeatedWaitOnClosedLatchTest()
			throws InterruptedException, ExecutionException {

		int numTestCallables = 10;

		TestCallable[] testCallables = new TestCallable[numTestCallables];
		for (int i = 0; i < numTestCallables; i++) {
			testCallables[i] = new TestCallable();
		}

		ExecutorService service = Executors
				.newFixedThreadPool(numTestCallables);

		for (int i = 0; i < NUM_REPETITIONS; i++) {

			latch.close();

			List<Future<Long>> futures = new LinkedList<Future<Long>>();
			for (int j = 0; j < numTestCallables; j++) {
				futures.add(service.submit(testCallables[j]));
			}

			for (Future<Long> future : futures) {

				// threads are not finished
				Assert.assertFalse(future.isDone());
				Assert.assertFalse(future.isCancelled());
			}

			latch.open();

			// FIXME: This is fraught with peril, I know
			// wait a sec before testing the state
			Thread.sleep(100);

			for (Future<Long> future : futures) {

				// threads are finished
				Assert.assertTrue(future.isDone());
				Assert.assertFalse(future.isCancelled());

				// not interrupted
				Assert.assertNotSame(new Long(-1), future.get());

				// within time frame
				Assert.assertTrue(future.get() < 100);
			}
		}
	}

	private void assertTimeLapsed() {
		assertTimeLapsed(100);
	}

	private void assertTimeLapsed(final long acceptedInterval) {

		// FIXME: This seems suspect, but not sure how else to judge time
		long endTime = new Date().getTime();
		Assert.assertTrue(startTime - endTime <= acceptedInterval);
	}

	private void setStartTime() {
		startTime = new Date().getTime();
	}
}
