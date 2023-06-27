package fi.dwo.dwojapplet.boot;

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
		getService().log(level, message);
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		getService().log(level, message, exception);
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		getService().log(sr, level, message);
	}

	@Override
	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		getService().log(sr, level, message, exception);
	}

}
