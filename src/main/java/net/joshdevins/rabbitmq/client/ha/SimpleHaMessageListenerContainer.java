package net.joshdevins.rabbitmq.client.ha;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.joshdevins.rabbitmq.client.MessageListener;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.InitializingBean;

/**
 * A simple HA-aware {@link MessageListener} container hiding the underlying
 * connection state from the {@link MessageListener} implementation. This
 * supports any number of task executors for concurrent consumption from the
 * given destination. By default, you will get a single thread thread pool.
 * 
 * <p>
 * This is a naive implementation in that a fixed number of threads are spun up
 * and consumers attached to them. There is no scheduling, throttling,
 * monitoring, etc. as happens in the Spring implementation <a href="http://static.springsource.org/spring/docs/3.0.x/spring-framework-reference/html/jms.html"
 * >DefaultMessageListenerContainer</a>. This may come in a future version but
 * in the meantime, make sure you use a reasonable number of concurrent
 * consumers.
 * </p>
 * 
 * <p>
 * Note that using more than one concurrent consumer will mean that any ordering
 * assumptions need to be disregarded. I repeat, there are <strong>no ordering
 * guarantees</strong> with multiple concurrent consumers.
 * </p>
 * 
 * @author Josh Devins <info@joshdevins.net>
 */
public class SimpleHaMessageListenerContainer implements InitializingBean {

	private static final int DEFAULT_NUM_CONCURRENT_CONSUMERS = 1;

	private final HaConnectionFactory haConnectionFactory;

	private final MessageListener messageListener;

	private final String queueName;

	private ExecutorService executorService;

	private ThreadFactory threadFactory;

	private int numConcurrentConsumers = DEFAULT_NUM_CONCURRENT_CONSUMERS;

	public SimpleHaMessageListenerContainer(
			final HaConnectionFactory haConnectionFactory,
			final MessageListener messageListener, final String queueName) {

		Validate
				.notNull(haConnectionFactory, "haConnectionFactory is required");
		Validate.notNull(messageListener, "messageListener is required");
		Validate.notEmpty(queueName, "queueName is required");

		this.haConnectionFactory = haConnectionFactory;
		this.messageListener = messageListener;
		this.queueName = queueName;
	}

	public void afterPropertiesSet() throws Exception {

		if (threadFactory == null) {
			threadFactory = Executors.defaultThreadFactory();
		}

		executorService = Executors.newFixedThreadPool(numConcurrentConsumers,
				threadFactory);

		// create a consumer task per thread
		for (int i = 0; i < numConcurrentConsumers; i++) {
			// executorService.submit();
		}
	}

	public void setNumberOfConcurrentConsumers(final int numConcurrentConsumers) {
		this.numConcurrentConsumers = numConcurrentConsumers;
	}

	/**
	 * Sets a {@link ThreadFactory} to be used when internally creating an
	 * {@link ExecutorService}.
	 */
	public void setThreadFactory(final ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}
}
