package fi.dwo.dwojapplet.boot;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class Configurator extends
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> implements Configuration, PropertyChangeListener {

	String pid;
	Dictionary<String,Object> dict;
	private boolean update;
	
	public Configurator(
			BundleContext context) {
		super(context, ConfigurationAdmin.class, null);
		pid = context.getBundle().getSymbolicName();
	}

	public Configuration getConfiguration() {
		ConfigurationAdmin adm = getService();
		if (adm != null) {
			try {
				return adm.getConfiguration(pid);
			} catch (IOException e) {
			}
		}
		return this;
	}

	@Override
	public ConfigurationAdmin addingService(
			ServiceReference<ConfigurationAdmin> reference) {
		ConfigurationAdmin cm = super.addingService(reference);
		if (dict != null && update) {
			try {
				cm.getConfiguration(pid).update(dict);
			} catch (IOException e) {
			}
			dict = null;
			update = false;
		}
		return cm;
	}

	@Override
	public void removedService(ServiceReference<ConfigurationAdmin> reference,
			ConfigurationAdmin service) {
//		try {
//			dict = copy(service.getConfiguration(pid).getProperties());
//		} catch (IOException e) {
//		}
		super.removedService(reference, service);
	}

	@Override
	public String getPid() {
		return pid;
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		if(dict == null)
			dict = new Hashtable<String,Object>();
		return (dict);
	}

	@Override
	public void update(Dictionary<String, ?> properties) throws IOException {
		Dictionary<String, Object> dict = copy(properties);	
		this.dict = dict;
		update = true;
	}

	private Dictionary<String, Object> copy(Dictionary<String, ?> properties) {
		Dictionary<String, Object>
		dict = new Hashtable<String,Object>();
		Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			dict.put(key, properties.get(key));
		}
		dict.put(Constants.SERVICE_PID, pid);
		return dict;
	}

	@Override
	public void delete() throws IOException {
	}

	@Override
	public String getFactoryPid() {
		return null;
	}

	@Override
	public void update() throws IOException {
		update = true;
	}

	@Override
	public void setBundleLocation(String location) {
	}

	@Override
	public String getBundleLocation() {
		return null;
	}

	@Override
	public long getChangeCount() {
		return 0;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String key = evt.getPropertyName();
		Object value = evt.getNewValue();
		Configuration c = getConfiguration();
		Dictionary<String, Object> dict = c.getProperties();
		if (value != null) dict.put(key, value);
		else dict.remove(key);
		try {
			c.update(dict);
		} catch (IOException e) {
		}
		
	}

	public BundleContext getBundleContext() {
		return context;
	}
}
