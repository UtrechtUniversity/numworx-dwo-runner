package fi.dwo.bootloader.impl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

class LogTracker extends ServiceTracker<LogService, LogService>
		implements LogService {

	public LogTracker(BundleContext context) {
		super(context, "org.osgi.service.log.LogService", null);
	}

	@Override
	public void log(int level, String message) {
		service().log(level, message);
	}

	public LogService service() {
		return getService();
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
