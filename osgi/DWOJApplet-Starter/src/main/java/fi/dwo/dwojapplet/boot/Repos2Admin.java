package fi.dwo.dwojapplet.boot;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

public class Repos2Admin extends ServiceTracker<XmlBackedRepositoryFactory,XmlBackedRepositoryFactory> implements RepAdmin {

	private String repository;
	private LogService LOG;

	public Repos2Admin(BundleContext context, LogService logservice) {
		super(context, "org.knopflerfish.service.repository.XmlBackedRepositoryFactory", null);
		LOG = logservice;
		
	}

	@Override
	public XmlBackedRepositoryFactory addingService(ServiceReference<XmlBackedRepositoryFactory> reference) {
		XmlBackedRepositoryFactory factory = super.addingService(reference);
		ServiceReference<Repository> rep = null;
		if (factory != null && repository != null)
		try {
			 rep = factory.create(repository, null, this);
		} catch (Exception e) {
		      LogService s = LOG;
              if (s != null) {
                s.log(reference, LogService.LOG_ERROR, repository, e);
              }
              
		    }
		
		return factory;

	}

	@Override
	public void modifiedService(ServiceReference<XmlBackedRepositoryFactory> reference, XmlBackedRepositoryFactory service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedService(ServiceReference<XmlBackedRepositoryFactory> reference, XmlBackedRepositoryFactory service) {
		try {
			service.destroy(this);
		} catch (Exception e) {
			LOG.log(LogService.LOG_WARNING, "removedService", e);
		}
		super.removedService(reference, service);
	}
	public String getRepository() {
		return repository;
	}

	@Override
	public void setRepository(String repository) {
		if(repository != null && repository.equals(getRepository())) return;
		XmlBackedRepositoryFactory admin = getService();
		if(admin != null && getRepository() != null)
		{
		  LOG.log(LogService.LOG_INFO, "remove Repository " + getRepository());
		  try {
			admin.destroy(this);
		} catch (Exception e) {
			LOG.log(LogService.LOG_WARNING, "setRepository removal " + getRepository(), e);
		}
		}
		this.repository = repository;
		if(admin != null && getRepository() != null)
			try {
	            LOG.log(LogService.LOG_INFO, "addRepository " + getRepository());
				admin.create(getRepository(), null, this);
			} catch (Exception e) {
				LOG.log(LogService.LOG_ERROR, "addRepository failed", e);
			}
		
	}

}
