package fi.microserver;

import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class LoggerTracker extends ServiceTracker {

	private Logger logger;
	@SuppressWarnings("rawtypes")
	private ServiceReference current;

	public LoggerTracker(BundleContext context, Logger logger) {
		super(context, "org.osgi.service.log.LogService", null);
		this.logger = logger;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object addingService(ServiceReference reference) {
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
	public void removedService(ServiceReference reference, Object service) {
		super.removedService(reference, service);
		logger.setLogger(getService());
		logger.setLogLevel(1);
	}


}
