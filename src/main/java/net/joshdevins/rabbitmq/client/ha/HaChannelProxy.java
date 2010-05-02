package net.joshdevins.rabbitmq.client.ha;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.rabbitmq.client.Channel;

public class HaChannelProxy implements InvocationHandler {

	public Channel target;

	public HaChannelProxy(final Channel target) {
		setTargetChannel(target);
	}

	public Channel getTargetChannel() {
		return target;
	}

	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {

		return InvocationHandlerUtils.delegateMethodInvocation(method, args,
				target);
	}

	protected void setTargetChannel(final Channel target) {

		assert target != null;
		this.target = target;
	}
}
