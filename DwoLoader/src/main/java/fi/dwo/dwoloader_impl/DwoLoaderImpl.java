package fi.dwo.dwoloader_impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.bundlerepository.Repository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import fi.dwo.bootloader.LoaderBuilder.Update;
import fi.dwo.dwoloader.DwoLoader;

public class DwoLoaderImpl implements DwoLoader, ManagedService {

	private final LogService log;
	private final BundleContext context;
	private final Repository repos;
	private long lastModified;
	private ServiceRegistration<DwoLoader> ref;
	public ServiceRegistration<ManagedService> managed;
	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cm;

	public DwoLoaderImpl(Repository repos, LogService logService, BundleContext context) {
		this.repos = repos;
		this.log = logService;
		this.context = context;
		lastModified = 0;
		cm = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
				context, ConfigurationAdmin.class, null);
		
	}
	
	void open() {
      cm.open();
	  Dictionary<String, String> dict = new Hashtable<String, String>();
	  dict.put(Constants.SERVICE_PID, context.getBundle().getSymbolicName());
	  managed = context.registerService(ManagedService.class, this, dict);
	}

	public Update getUpdate() {
		if(lastModified == repos.getLastModified())
			return Update.NEVER;
		return Update.ALWAYS;
	}

	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		Object object = properties != null ? properties.get("lastModified") : null;
		if (object instanceof Number)
			lastModified = ((Number) object).longValue();
		managed.unregister();managed = null;
		ConfigurationAdmin admin = cm.getService();
		if (admin != null) {
			String pid = context.getBundle().getSymbolicName();
			try {
				Configuration c = admin.getConfiguration(pid);
				Dictionary<String, Object> dict = c.getProperties();
				if (dict == null)
					dict = new Hashtable<String, Object>();
				dict.put("lastModified", Long.valueOf(repos.getLastModified()));
				c.update(dict);
			} catch (IOException e) {
				log.log(LogService.LOG_WARNING, "set lastModified", e);
			}
		}
		ref = context.registerService(DwoLoader.class, this, properties);
		
	}

	public void close() {
		if (managed != null)
			managed.unregister();
		if (ref != null)
		  ref.unregister();
		cm.close();
	}


}
