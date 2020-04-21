package fi.microserver;

import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class LoggerTracker extends ServiceTracker {

	private Logger logger;

	public LoggerTracker(BundleContext context, Logger logger) {
		super(context, "org.osgi.service.log.LogService", null);
		this.logger = logger;
	}

	@Override
	public Object addingService(ServiceReference reference) {
		Object service = super.addingService(reference);
		logger.setLogger(service);
		logger.setLogLevel(4);
		return service;
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		logger.setLogger(null);
		logger.setLogLevel(1);
		super.removedService(reference, service);
	}


}
