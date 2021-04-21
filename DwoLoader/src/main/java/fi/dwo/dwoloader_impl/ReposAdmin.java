package fi.dwo.dwoloader_impl;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class ReposAdmin implements ServiceTrackerCustomizer<Object,AutoCloseable> {
	
  	final BundleContext context;
  	final String index;
  	final ServiceTracker<LogService, LogService> logTracker;


	ReposAdmin(BundleContext context, String index, ServiceTracker<LogService, LogService> logTracker) {
		super();
		this.context = context;
		this.index = index;
		this.logTracker = logTracker;
	}

	public AutoCloseable addingService(ServiceReference<Object> reference) {
		RepositoryAdmin admin = (RepositoryAdmin) context.getService(reference);
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
              return new DwoLoaderDummy(e, context);
              
		    }
		
		return impl;
	}

	public void modifiedService(ServiceReference<Object> reference, AutoCloseable service) {
	}

	public void removedService(ServiceReference<Object> reference, AutoCloseable service) {
		try {
	    service.close();
		RepositoryAdmin admin = (RepositoryAdmin) context.getService(reference);
		admin.removeRepository(index);
		context.ungetService(reference);
  } catch (Exception e) {
    LogService s = logTracker.getService();
    if (s != null) {
      s.log(reference, LogService.LOG_WARNING, index, e);
    }
  }
	}
	
}