package fi.dwo.dwoloader_impl;

import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<ConfigurationAdmin, AutoCloseable> {
	
	final static String REPO_PID = "org.knopflerfish.repository.xml.MSF";
	private ServiceTracker<LogService,LogService> logTracker;
	private ServiceTracker<Object, AutoCloseable> reposTracker, repos2Tracker;
	//private ServiceTracker<ConfigurationAdmin, AutoCloseable> cmTracker;
	private String index;
	private BundleContext context;
	
	public void start(BundleContext context) throws Exception {
		this.context = context;
		ServiceReference<?> ref = context.getServiceReference("org.osgi.service.provisioning.ProvisioningService");
		if (ref != null) {
		  ProvisioningService service = (ProvisioningService) context.getService(ref);
		  Dictionary<?, ?> dict = service.getInformation();
		  index = (String) dict.get("fi.dwo.dwojapplet.domain.DWO");
		  context.ungetService(ref);
		}
		if (index == null)
		  index = context.getProperty("fi.dwo.dwojapplet.domain.DWO");
		if(index == null) 
			index = "https://app.dwo.nl/dwo/index.xml";
		else
			index = URI.create(index).resolve("index.xml").toString();
		
		logTracker = new ServiceTracker<LogService,LogService>(context, LogService.class, null);
		reposTracker  = new ServiceTracker<Object, AutoCloseable>(context,"org.apache.felix.bundlerepository.RepositoryAdmin", new ReposAdmin(context, index, logTracker));
		repos2Tracker = new ServiceTracker<Object, AutoCloseable>(context, "org.knopflerfish.service.repository.XmlBackedRepositoryFactory", new Repos2Admin(context, index, logTracker, reposTracker));
		//cmTracker = new ServiceTracker<ConfigurationAdmin, AutoCloseable>(context, ConfigurationAdmin.class, this);
		logTracker.open();
		reposTracker.open();
		repos2Tracker.open();
		//cmTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		reposTracker.close();
		logTracker.close();
		//cmTracker.close();
		repos2Tracker.close();
		index = null;
	}

	@Override
	public AutoCloseable addingService(ServiceReference<ConfigurationAdmin> reference) {
		ConfigurationAdmin cm = context.getService(reference);
		try {
			Configuration[] cfgs = cm.listConfigurations("(service.factoryPid=\"" + REPO_PID + "\")");
			if (cfgs == null || cfgs.length == 0) {
				Configuration cfg = cm.createFactoryConfiguration(REPO_PID);
				Dictionary<String, String> properties = new Hashtable<>();
				properties.put("url", index);
				cfg.update(properties);
			}
			return new DwoLoaderDummy(null, context);
		} catch (IOException e) {
			return new DwoLoaderDummy(e, context);
		} catch (InvalidSyntaxException e) {
			return new DwoLoaderDummy(e, context);
		}
	}

	@Override
	public void modifiedService(ServiceReference<ConfigurationAdmin> reference, AutoCloseable service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedService(ServiceReference<ConfigurationAdmin> reference, AutoCloseable service) {
		try {
			service.close();
		} catch (Exception e) {
		}
		
	}

}
