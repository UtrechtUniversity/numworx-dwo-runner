package fi.dwo.bootloader.impl;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class ConfiguratorSingleton extends
		ServiceTracker<ManagedService, ManagedService> {

	private static final String PFX = "resources/";
	private BundleContext context;
	private LogService LOG;

	public ConfiguratorSingleton(BundleContext context, LogService lOG) {
		super(context, ManagedService.class, null);
		this.context = context;
		LOG = lOG;
	}

	public ManagedService addingService(ServiceReference<ManagedService> ref) {
		ManagedService singleton = context.getService(ref);
		String pid = (String) ref.getProperty(Constants.SERVICE_PID);
		LOG.log(LogService.LOG_INFO, "got " + singleton + ", pid=" + pid);
		if (installSingleton(singleton, pid))
			return singleton;
		context.ungetService(ref);
		return null;
	}

	private boolean installSingleton(ManagedService factory, String pid) {
		Properties p;
		try {
			p = new Properties();
			Dictionary<String, String> dict = (Dictionary) p;
			InputStream in = getClass().getResourceAsStream(PFX + pid);
			if (in == null)
				return false;
			p.load(in);
			in.close();
			factory.updated(dict);
			return true;
		} catch (Exception e) {
			LOG.log(LogService.LOG_ERROR, "installSingleton " + pid, e);
			return false;
		}
	}

}
