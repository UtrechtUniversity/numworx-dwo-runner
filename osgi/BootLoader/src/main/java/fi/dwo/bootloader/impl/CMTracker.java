package fi.dwo.bootloader.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

public class CMTracker extends
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> {

	private static final String FI_DWO_DWOJAPPLET = "fi.dwo.dwojapplet";
	private static final String LOGGER_CONTEXT_PID = "org.osgi.service.log.admin";
	LogTracker log;
	Config config;
	Deferred<Config> deferred;

	public CMTracker(BundleContext context, LogTracker lOGt) {
		super(context, ConfigurationAdmin.class, null);
		this.log = lOGt;
	}

	public Promise<Config> open(Config config) {
		this.config = config;
		deferred = new Deferred<Config>();
		open();
		return deferred.getPromise();
	}

	@Override
	public ConfigurationAdmin addingService(
			ServiceReference<ConfigurationAdmin> reference) {
		ConfigurationAdmin cm = super.addingService(reference);
		configureLogger(cm, reference);
		configure(cm, reference);
		return cm;
	}

	private void configureLogger(ConfigurationAdmin cm, ServiceReference<ConfigurationAdmin> reference) {
		try {
			configureLogger(cm, LOGGER_CONTEXT_PID);
		} catch(IOException e) {
			log.warning(reference,"configure " + LOGGER_CONTEXT_PID, e);
		}		
	}

	private void configureLogger(ConfigurationAdmin cm, String pid) throws IOException {
		Configuration config = cm.getConfiguration(pid, null);
		Properties p = new Properties();
		InputStream in = getClass().getResourceAsStream("resources/" + pid);
		p.load(in);
		in.close();
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Dictionary<String, Object> dict = (Dictionary) p;
		Dictionary<String, Object> old = config.getProperties();
		if (old != null) {
			Enumeration<String> e = dict.keys();
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				if (null == old.get(key))
					old.put(key, dict.get(key));
			}
		} else {
			old = dict;
		}
		if (!old.equals(config.getProperties()))
			config.update(old);
	}

	void configure(ConfigurationAdmin cm, ServiceReference<?> reference) {
		try {
			configure(cm, FI_DWO_DWOJAPPLET);
		} catch (IOException e) {
			log.error(reference, "configure "
					+ FI_DWO_DWOJAPPLET, e);
			deferred.fail(e);
		}
	}

	private void configure(ConfigurationAdmin cm, String pid) throws IOException {
		Configuration config = cm.getConfiguration(pid, null);
		Properties p = new Properties();
		InputStream in = getClass().getResourceAsStream("resources/" + pid);
		p.load(in);
		in.close();
		// defaults from microserver
		p.put("language", this.config.getProperty("fi.dwo.language"));
		p.put("profile", this.config.getProperty("fi.dwo.profile"));
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Dictionary<String, Object> dict = (Dictionary) p;
		Dictionary<String, Object> old = config.getProperties();
		if (old != null) {
			Enumeration<String> e = dict.keys();
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				if (null == old.get(key))
					old.put(key, dict.get(key));
			}
		} else {
			old = dict;
		}
		if (!old.equals(config.getProperties()))
			config.update(old);

		deferred.resolve(this.config);
	}

}
