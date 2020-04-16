package fi.dwo.dwoloader_impl;

import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fi.dwo.bootloader.LoaderBuilder.Update;
import fi.dwo.dwoloader.DwoLoader;

public class Activator implements BundleActivator {
	
	class DwoLoaderDummy implements AutoCloseable, DwoLoader {
	  
    private ServiceRegistration<DwoLoader> reg;
    private RuntimeException re;
    public DwoLoaderDummy(Exception e, BundleContext context) {
      reg = context.registerService(DwoLoader.class, this, new Hashtable<String,Object>());
//      if (e instanceof RuntimeException) {
//        re = (RuntimeException) e;
//      } else if (e != null) {
//        re = new RuntimeException(e);
//      }
    }

    @Override
    public Update getUpdate() {
      if (re != null) {
        re.fillInStackTrace();
        throw re;
      }
      return Update.MAYBE;
    }

    @Override
    public void close() {
      reg.unregister();
    }

  }

  private class ReposAdmin implements ServiceTrackerCustomizer<RepositoryAdmin,AutoCloseable> {
		
		public AutoCloseable addingService(ServiceReference<RepositoryAdmin> reference) {
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
	              return new DwoLoaderDummy(e, context);
	              
			    }
			
			return impl;
		}

		public void modifiedService(ServiceReference<RepositoryAdmin> reference, AutoCloseable service) {
		}

		public void removedService(ServiceReference<RepositoryAdmin> reference, AutoCloseable service) {
			try {
        service.close();
      } catch (Exception e) {
        LogService s = logTracker.getService();
        if (s != null) {
          s.log(reference, LogService.LOG_WARNING, index, e);
        }
      }
		}
		
	}
	
	private ServiceTracker<LogService,LogService> logTracker;
	private ServiceTracker<RepositoryAdmin,AutoCloseable> reposTracker;
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
		
		reposTracker  = new ServiceTracker<RepositoryAdmin, AutoCloseable>(context,RepositoryAdmin.class, new ReposAdmin());
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
