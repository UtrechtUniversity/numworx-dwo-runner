package fi.dwo.dwojapplet.boot;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.Logger;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

public class Repos2Admin extends ServiceTracker<XmlBackedRepositoryFactory,XmlBackedRepositoryFactory> implements RepAdmin {

	private String repository;
	private Logger LOG;

	public Repos2Admin(BundleContext context, Logger log2) {
		super(context, "org.knopflerfish.service.repository.XmlBackedRepositoryFactory", null);
		LOG = log2;
		
	}

	@Override
	public XmlBackedRepositoryFactory addingService(ServiceReference<XmlBackedRepositoryFactory> reference) {
		XmlBackedRepositoryFactory factory = super.addingService(reference);
		ServiceReference<Repository> rep = null;
		if (factory != null && repository != null)
		try {
			 rep = factory.create(repository, null, this);
		} catch (Exception e) {
		      Logger s = LOG;
              if (s != null) {
                s.error( repository, reference, e);
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
			LOG.warn( "removedService", e);
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
		  LOG.info("remove Repository " + getRepository());
		  try {
			admin.destroy(this);
		} catch (Exception e) {
			LOG.warn("setRepository removal " + getRepository(), e);
		}
		}
		this.repository = repository;
		if(admin != null && getRepository() != null)
			try {
	            LOG.info("addRepository " + getRepository());
				admin.create(getRepository(), null, this);
			} catch (Exception e) {
				LOG.error("addRepository failed", e);
			}
		
	}

}
