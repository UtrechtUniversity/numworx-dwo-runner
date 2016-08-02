package fi.microserver;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class Configurator extends ServiceTracker<ManagedServiceFactory, ManagedServiceFactory> {

	private BundleContext context;
	private LogService LOG;
	
	public Configurator(BundleContext context, LogService lOG) {
		super(context, ManagedServiceFactory.class, null);
		this.context = context;
		LOG = lOG;
	}

	public ManagedServiceFactory addingService(
			ServiceReference<ManagedServiceFactory> ref) {
		ManagedServiceFactory factory = context.getService(ref);
		String pid = (String) ref.getProperty(Constants.SERVICE_PID);
		LOG.log(LogService.LOG_INFO, "got " + factory.getName() + ", pid=" + pid);
		return factory;
	}

	public void modifiedService(ServiceReference<ManagedServiceFactory> ref,
			ManagedServiceFactory service) {
	}

	public void removedService(ServiceReference<ManagedServiceFactory> ref,
			ManagedServiceFactory object) {
		context.ungetService(ref);
	}

}
