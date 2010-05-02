package net.joshdevins.rabbitmq.client.ha;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class HaConnectionProxy implements InvocationHandler {

	private Connection target;

	public HaConnectionProxy(final Connection target) {
		setTargetConnection(target);
	}

	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {

		return InvocationHandlerUtils.delegateMethodInvocation(method, args,
				target);
	}

	public void setTargetConnection(final Connection target) {

		assert target != null;
		this.target = target;
	}

	protected Channel createChannelProxy(final Channel targetChannel) {

		ClassLoader classLoader = Connection.class.getClassLoader();
		Class<?>[] interfaces = {Channel.class};
		InvocationHandler invocationHandler = new HaChannelProxy(targetChannel);

		return (Channel) Proxy.newProxyInstance(classLoader, interfaces,
				invocationHandler);
	}
}
