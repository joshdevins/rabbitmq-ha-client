package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;

public class HaChannelProxy implements InvocationHandler {

	private static final Logger LOG = Logger.getLogger(HaChannelProxy.class);

	private Channel target;

	private final RetryStrategy retryStrategy;

	public HaChannelProxy(final Channel target,
			final RetryStrategy retryStrategy) {
		setTargetChannel(target);

		assert retryStrategy != null;
		this.retryStrategy = retryStrategy;
	}

	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {

		// invoke a method max times
		Exception lastException = null;
		boolean keepOnInvoking = true;
		for (int numOperationInvocations = 1; keepOnInvoking; numOperationInvocations++) {

			// delegate all other method invocations
			// sych on target object to make sure it's not being replaced
			synchronized (target) {
				try {
					return InvocationHandlerUtils.delegateMethodInvocation(
							method, args, target);

					// deal with exceptions outside the synchronized block so
					// that if a reconnection does occur, it can replace the
					// target
				} catch (IOException ioe) {
					lastException = ioe;

				} catch (AlreadyClosedException ace) {
					lastException = ace;
				}
			}

			keepOnInvoking = retryStrategy.shouldRetry(lastException,
					numOperationInvocations);
		}

		LOG.warn("Operation invocation failed after retry strategy gave up: ",
				lastException);
		throw lastException;
	}

	protected Channel getTargetChannel() {
		return target;
	}

	protected synchronized void setTargetChannel(final Channel target) {

		assert target != null;

		if (this.target != null) {

			if (LOG.isDebugEnabled() && this.target != null) {
				LOG.debug("Replacing channel: channel="
						+ this.target.toString());
			}

			synchronized (this.target) {

				this.target = target;

				if (LOG.isDebugEnabled() && this.target != null) {
					LOG.debug("Replaced channel: channel="
							+ this.target.toString());
				}
			}
		} else {
			this.target = target;
		}
	}
}
