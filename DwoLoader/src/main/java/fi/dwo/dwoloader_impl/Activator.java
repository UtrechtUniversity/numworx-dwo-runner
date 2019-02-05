package fi.dwo.dwoloader_impl;

import java.net.URI;
import java.util.Dictionary;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {
	
	private class ReposAdmin implements ServiceTrackerCustomizer<RepositoryAdmin,DwoLoaderImpl> {
		
		public DwoLoaderImpl addingService(ServiceReference<RepositoryAdmin> reference) {
			RepositoryAdmin admin = context.getService(reference);
			Repository rep = null;
			DwoLoaderImpl impl = null;
			if (admin != null)
			try {
				 rep = admin.addRepository(index);
				 impl = new DwoLoaderImpl(rep, logTracker.getService(), context);
				 impl.open();
			} catch (Exception e) {
			    
			      LogService s = logTracker.getService();
	              if (s != null) {
	                s.log(reference, LogService.LOG_ERROR, index, e);
	              }
			    }
			
			return impl;
		}

		public void modifiedService(ServiceReference<RepositoryAdmin> reference, DwoLoaderImpl service) {
		}

		public void removedService(ServiceReference<RepositoryAdmin> reference, DwoLoaderImpl service) {
			service.close();
		}
		
	}
	
	private ServiceTracker<LogService,LogService> logTracker;
	private ServiceTracker<RepositoryAdmin,DwoLoaderImpl> reposTracker;
	private BundleContext context;
	private String index;
	
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
		
		reposTracker  = new ServiceTracker<RepositoryAdmin, DwoLoaderImpl>(context,RepositoryAdmin.class, new ReposAdmin());
		logTracker = new ServiceTracker<LogService,LogService>(context, LogService.class, null);

		logTracker.open();
		reposTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		reposTracker.close();
		logTracker.close();
		index = null;
	}

}
