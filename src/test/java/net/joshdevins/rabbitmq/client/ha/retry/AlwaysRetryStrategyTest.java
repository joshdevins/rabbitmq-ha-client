package net.joshdevins.rabbitmq.client.ha.retry;

import org.junit.Assert;
import org.junit.Test;

public class AlwaysRetryStrategyTest {

	@Test
	public void basicTest() {
		AlwaysRetryStrategy strategy = new AlwaysRetryStrategy();

		Assert.assertTrue(strategy.shouldRetry(null, 0, null));
	}
}
