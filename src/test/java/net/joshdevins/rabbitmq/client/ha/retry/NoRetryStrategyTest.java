package net.joshdevins.rabbitmq.client.ha.retry;

import org.junit.Assert;
import org.junit.Test;

public class NoRetryStrategyTest {

	@Test
	public void basicTest() {
		NoRetryStrategy strategy = new NoRetryStrategy();

		Assert.assertFalse(strategy.shouldRetry(null, 0, null));
	}
}
