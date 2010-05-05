package net.joshdevins.rabbitmq.client.ha.retry;

import org.junit.Assert;
import org.junit.Test;

public class NeverRetryStrategyTest {

	@Test
	public void basicTest() {
		NeverRetryStrategy strategy = new NeverRetryStrategy();

		Assert.assertFalse(strategy.shouldRetry(null, 0, null));
	}
}
