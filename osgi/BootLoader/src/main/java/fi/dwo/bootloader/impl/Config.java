package fi.dwo.bootloader.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

public class Config implements ManagedService {

	private BundleContext context;
	private ServiceRegistration<ManagedService> ref;
	private Dictionary<String, ?> config;
	private Dictionary<?,?> provision;
	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cm;
	private LogService log;
	private Deferred<Config> deferred = new Deferred<Config>();

	public Config(BundleContext context, LogService log) {
		this.context = context;
		this.setLog(log);
		cm = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
				context, ConfigurationAdmin.class, null);
        installProvision();
	}

	void open() {
		Dictionary<String, String> dict = new Hashtable<String, String>();
		dict.put(Constants.SERVICE_PID, context.getBundle().getSymbolicName());
		ref = context.registerService(ManagedService.class, this, dict);
		cm.open();
	}

	private void installProvision() {
      try {
        ServiceReference<?> r = context.getServiceReference("org.osgi.service.provisioning.ProvisioningService");
        if (r != null) {
          ProvisioningService service = (ProvisioningService) context.getService(r);
          provision = service.getInformation(); context.ungetService(r);
        }
      } catch (Exception e) {
        log.log(ref.getReference(), LogService.LOG_WARNING, "install provision service", e);
      }
    
  }

  void close() {
		if (ref != null) {
			ref.unregister();
			ref = null;
		}
		cm.close();
	}

	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		config = properties;
		if (!deferred.getPromise().isDone())
			deferred.resolve(this);
	}

	public Object getProperty(String key) {
		if (config != null) {
			Object value = config.get(key);
			if (value != null)
				return value;
		}  
		if (provision != null) {
		  Object value = provision.get(key);
		  if (value != null) {
		    return value;
		  }
		}
		return context.getProperty(key);
	}

	public void setProperty(String key, Object value) {
		ConfigurationAdmin admin = cm.getService();
		if (admin != null) {
			String pid = context.getBundle().getSymbolicName();
			try {
				Configuration c = admin.getConfiguration(pid);
				Dictionary<String, Object> dict = c.getProperties();
				if (dict == null)
					dict = new Hashtable<String, Object>();
				dict.put(key, value);
				c.update(dict);
			} catch (IOException e) {
				getLog().log(LogService.LOG_WARNING, "setProperty " + key, e);
			}
		}
	}

	public LogService getLog() {
		return log;
	}

	public void setLog(LogService log) {
		this.log = log;
	}

	Promise<Config> getPromise() {
		return deferred.getPromise();
	}
}
