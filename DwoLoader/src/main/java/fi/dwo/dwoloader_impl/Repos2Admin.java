package fi.dwo.dwoloader_impl;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Repos2Admin implements ServiceTrackerCustomizer<Object,AutoCloseable> {

	private BundleContext context;
	private String index;
	private ServiceTracker<LogService, LogService> logTracker;

	public Repos2Admin(BundleContext context, String index, ServiceTracker<LogService, LogService> logTracker) {
		this.context = context;
		this.index = index;
		this.logTracker = logTracker;
	}

	@Override
	public AutoCloseable addingService(ServiceReference<Object> reference) {
		XmlBackedRepositoryFactory factory = (XmlBackedRepositoryFactory) context.getService(reference);
		ServiceReference<Repository> rep = null;
		DwoLoaderDummy impl = null;
		if (factory != null)
		try {
			 rep = factory.create(index, null, null);
			 impl = new DwoLoaderDummy(null, context);
		} catch (Exception e) {
		      LogService s = logTracker.getService();
              if (s != null) {
                s.log(reference, LogService.LOG_ERROR, index, e);
              }
              return new DwoLoaderDummy(e, context);
              
		    }
		
		return impl;

	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, AutoCloseable service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedService(ServiceReference<Object> reference, AutoCloseable service) {
		// TODO Auto-generated method stub
		
	}

}
