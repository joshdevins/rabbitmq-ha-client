package net.joshdevins.rabbitmq.client.ha;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import net.joshdevins.rabbitmq.client.ha.retry.RetryStrategy;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class HaConnectionProxy implements InvocationHandler {

	private static final Logger LOG = Logger.getLogger(HaConnectionProxy.class);

	private static Method CREATE_CHANNEL_METHOD;

	private static Method CREATE_CHANNEL_INT_METHOD;

	private final Integer maxRedirects;

	private Connection target;

	private final Set<HaChannelProxy> channelProxies;

	private final RetryStrategy retryStrategy;

	public HaConnectionProxy(final Integer maxRedirects,
			final Connection target, final RetryStrategy retryStrategy) {

		assert target != null;
		assert retryStrategy != null;

		this.target = target;
		this.maxRedirects = maxRedirects;
		this.retryStrategy = retryStrategy;

		channelProxies = new HashSet<HaChannelProxy>();
	}

	public void closeConnectionLatch() {
		for (HaChannelProxy proxy : channelProxies) {
			proxy.closeConnectionLatch();
		}
	}

	public Address[] getKnownHosts() {
		return target.getKnownHosts();
	}

	public Integer getMaxRedirects() {
		return maxRedirects;
	}

	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {

		// intercept calls to create a channel
		if (method.equals(CREATE_CHANNEL_METHOD)
				|| method.equals(CREATE_CHANNEL_INT_METHOD)) {

			return createChannelAndWrapWithProxy(method, args);
		}

		// delegate all other method invocations
		return InvocationHandlerUtils.delegateMethodInvocation(method, args,
				target);
	}

	public void reconnect() {
		target.abort(AMQP.CHANNEL_ERROR, "reconnect");
	}

	protected Channel createChannelAndWrapWithProxy(final Method method,
			final Object[] args) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {

		Channel targetChannel = (Channel) method.invoke(target, args);

		ClassLoader classLoader = Connection.class.getClassLoader();
		Class<?>[] interfaces = {Channel.class};

		// create the proxy and add to the set of channels we have created
		HaChannelProxy proxy = new HaChannelProxy(targetChannel, retryStrategy);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Creating channel proxy: " + targetChannel.toString());
		}

		// save the channel number-to-proxy relationship to be replaced later
		synchronized (channelProxies) {
			channelProxies.add(proxy);
		}

		return (Channel) Proxy.newProxyInstance(classLoader, interfaces, proxy);
	}

	protected void replaceChannelsInProxies() throws IOException {

		synchronized (channelProxies) {

			for (HaChannelProxy proxy : channelProxies) {

				// replace dead channel with a new one using the same ID
				int channelNumber = proxy.getTargetChannel().getChannelNumber();
				proxy.setTargetChannel(target.createChannel(channelNumber));
			}
		}
	}

	protected void setTargetConnection(final Connection target) {

		assert target != null;
		this.target = target;
	}

	static {

		// initialize static fields or fail fast
		try {
			CREATE_CHANNEL_METHOD = Connection.class.getMethod("createChannel");
			CREATE_CHANNEL_INT_METHOD = Connection.class.getMethod(
					"createChannel", int.class);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
