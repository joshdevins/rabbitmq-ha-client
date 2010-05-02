package net.joshdevins.rabbitmq.client.ha;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class InvocationHandlerUtils {

	private InvocationHandlerUtils() {
		// do not instantiate
	}

	/**
	 * Simple wrapper around {@link Method#invoke(Object, Object...)} which
	 * rethrows any target exception.
	 */
	public static Object delegateMethodInvocation(final Method method,
			final Object[] args, final Object target) throws Throwable {

		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException ite) {
			throw ite.getTargetException();
		}
	}
}
