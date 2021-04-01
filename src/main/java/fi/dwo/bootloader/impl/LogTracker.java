package fi.dwo.bootloader.impl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

class LogTracker extends ServiceTracker<LogService, LogService>
		implements LogService {

	private static class DummyService implements LogService  {

		@Override
		public void log(int level, String message) {
		}

		@Override
		public void log(int level, String message, Throwable exception) {
		}

		@Override
		public void log(ServiceReference sr, int level, String message) {
		}

		@Override
		public void log(ServiceReference sr, int level, String message, Throwable exception) {
		}
		
	}

	private static final LogService DUMMYSERVICE = new DummyService();
 	
	
	public LogTracker(BundleContext context) {
		super(context, "org.osgi.service.log.LogService", null);
	}

	@Override
	public void log(int level, String message) {
		service().log(level, message);
	}

	public LogService service() {
		LogService service = getService();
		if (service == null) return DUMMYSERVICE;
		return service;
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		service().log(level, message, exception);
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		service().log(sr, level, message);
	}

	@Override
	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		service().log(sr, level, message, exception);
	}

}
