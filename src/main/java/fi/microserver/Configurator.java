package fi.microserver;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class Configurator extends ServiceTracker<ManagedServiceFactory, ManagedServiceFactory> {

	private static final String PFX = "resources/";
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
		installFactory(factory, pid);
		return factory;
	}

	private void installFactory(ManagedServiceFactory factory, String pid) {
		Properties p;
		int i = -1;
		try {
			p = new Properties();
			Dictionary<String, String> dict = (Dictionary) p;
			String instance = pid + i;
			InputStream in = getClass().getResourceAsStream(PFX + instance);
			p.load(in);
			 
			factory.updated(instance, dict);
			
		} catch(Exception e) {
			LOG.log(LogService.LOG_ERROR, "installFactory " + pid, e);
		}
		
	}

	public void removedService(ServiceReference<ManagedServiceFactory> ref,
			ManagedServiceFactory object) {
		String pid = (String) ref.getProperty(Constants.SERVICE_PID);
		object.deleted(pid + "-1"); // TODO
		context.ungetService(ref);
	}

}
