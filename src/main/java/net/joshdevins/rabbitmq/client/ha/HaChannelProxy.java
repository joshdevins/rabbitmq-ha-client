package net.joshdevins.rabbitmq.client.ha;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;

public class HaChannelProxy implements InvocationHandler {

	private static final Logger LOG = Logger.getLogger(HaChannelProxy.class);

	/**
	 * Default value = 10000 = 10 seconds
	 */
	private static final long DEFAULT_OPERATION_RETRY_TIMEOUT_MILLIS = 10000;

	/**
	 * Default value = 2 (one retry)
	 */
	private static final int DEFAULT_MAX_OPERATION_INVOCATIONS = 2;

	private Channel target;

	private long operationRetryTimeoutMillis = DEFAULT_OPERATION_RETRY_TIMEOUT_MILLIS;

	private int maxOperationInvocations = DEFAULT_MAX_OPERATION_INVOCATIONS;

	public HaChannelProxy(final Channel target) {
		setTargetChannel(target);
	}

	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {

		// invoke a method max times
		AlreadyClosedException lastAce = null;
		for (int i = 1; i <= maxOperationInvocations; i++) {

			// delegate all other method invocations
			// sych on target object to make sure it's not being replaced
			synchronized (target) {
				try {
					return InvocationHandlerUtils.delegateMethodInvocation(
							method, args, target);

				} catch (AlreadyClosedException ace) {
					// deal with this outside the synchronized block so that if
					// a reconnection does occur, it can replace the target
				}
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("Operation invocation failed, sleeping: i=" + i
						+ ", maxOperationInvocations="
						+ maxOperationInvocations + ", timeout="
						+ operationRetryTimeoutMillis);
			}

			try {
				Thread.sleep(operationRetryTimeoutMillis);
			} catch (InterruptedException ie) {
				LOG
						.warn("Thread interrupted while timeout waiting for reconnection to occurr. Just going on to next invocation...");
			}
		}

		// can't happen
		if (lastAce == null) {
			throw new IllegalStateException(
					"No operation invocations were attempted! This is a bug!");
		}

		LOG.warn("Operation invocation reached max allowable: "
				+ maxOperationInvocations, lastAce);
		throw lastAce;
	}

	public void setMaxOperationInvocations(final int maxOperationInvocations) {

		Validate.isTrue(maxOperationInvocations > 0,
				"max operation invocations must be 1 or greater");
		this.maxOperationInvocations = maxOperationInvocations;
	}

	public void setNoOperationRetries() {
		maxOperationInvocations = 0;
	}

	public void setOperationRetryTimeoutMillis(final long timeout) {
		operationRetryTimeoutMillis = timeout;
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
