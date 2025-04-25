package fi.dwo.dwoloader_impl;

import java.net.URI;
import java.util.Dictionary;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	
	private ServiceTracker<LoggerFactory,LoggerFactory> logTracker;
	private ServiceTracker<Object, AutoCloseable> reposTracker;
	private String index;
	
	public void start(BundleContext context) throws Exception {
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
		
		logTracker = new ServiceTracker<LoggerFactory,LoggerFactory>(context, LoggerFactory.class, null);
		reposTracker  = createRepAdmin(context);
		logTracker.open();
		reposTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		reposTracker.close();
		logTracker.close();
		index = null;
	}

	private  ServiceTracker<Object, AutoCloseable> createRepAdmin(BundleContext context) {
		return new ServiceTracker<Object, AutoCloseable>(context, "org.knopflerfish.service.repository.XmlBackedRepositoryFactory", new Repos2Admin(context, index, logTracker));
	}

}
