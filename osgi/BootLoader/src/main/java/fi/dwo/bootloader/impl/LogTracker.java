package fi.dwo.bootloader.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

public class LogTracker extends ServiceTracker<LoggerFactory, LoggerFactory> implements InvocationHandler {

	/**
	 * @param instance the instance to set
	 */
	public void setInstance(Object instance) {
		this.instance = instance;
	}

	private Logger DUMMYSERVICE = null; // Sink (Proxy or Mock)
	private Object instance = this;
 	
	
	public LogTracker(BundleContext context) {
		super(context, LoggerFactory.class, null);
		DUMMYSERVICE = (Logger) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ Logger.class }, this);
	}

	public LogTracker(BundleContext context, Object instance) {
		this(context);
		setInstance(instance);
	}

	public Logger service(Class<?> clz) {
		LoggerFactory service = getService();
		if (service == null) return DUMMYSERVICE;
		return service.getLogger(clz);
	}
	
	public Logger service() {
		return service(instance.getClass());
	}

	public void warning(String string, Throwable e) {
		service().warn(string, e);
	}

	public void warning(ServiceReference<?> reference, String string, Exception e) {
		service().warn(string, reference, e);
	}

	public void error(String string, Throwable failure) {
		service().error(string, failure);
	}

	public void warning(String string) {
		service().warn(string);
	}

	public void error(ServiceReference<?> reference, String string, Exception e) {
		service().error(string, reference, e);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return null;
	}


}
