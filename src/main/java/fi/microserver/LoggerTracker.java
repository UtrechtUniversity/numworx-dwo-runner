package fi.microserver;

import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class LoggerTracker extends ServiceTracker<Object, Object> {

	private Logger logger;
	@SuppressWarnings("rawtypes")
	private ServiceReference current;

	public LoggerTracker(BundleContext context, Logger logger) {
		super(context, "org.osgi.service.log.LogService", null);
		this.logger = logger;
	}

	@Override
	public Object addingService(ServiceReference<Object> reference) {
		Object service = super.addingService(reference);
		if (current == null) {
			current = reference;
			logger.setLogger(service);
			logger.setLogLevel(1);
		} else {
			// implement ranking
		}		
		return service;
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		super.removedService(reference, service);
		logger.setLogger(getService());
		logger.setLogLevel(1);
	}


}
