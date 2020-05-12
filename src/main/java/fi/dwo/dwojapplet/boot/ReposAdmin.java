package fi.dwo.dwojapplet.boot;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

class ReposAdmin extends
		ServiceTracker<RepositoryAdmin, RepositoryAdmin> {
	private String repository;
	private LogService LOG;

	ReposAdmin(BundleContext context, LogService log) {
		super(context, "org.apache.felix.bundlerepository.RepositoryAdmin", null);
		LOG = log;
	}

	@Override
	public RepositoryAdmin addingService(
			ServiceReference<RepositoryAdmin> reference) {
		RepositoryAdmin admin = super.addingService(reference);
		try {
			if(getRepository() != null)
			{
			  LOG.log(reference, LogService.LOG_INFO, "addRepository " + getRepository());
			  admin.addRepository(getRepository());
			}
		} catch (Exception e) {
          LOG.log(reference, LogService.LOG_INFO, "addRepository failed", e);

		}
		return admin;
	}

	@Override
	public void removedService(
			ServiceReference<RepositoryAdmin> reference,
			RepositoryAdmin service) {
        LOG.log(reference, LogService.LOG_INFO, "remove Repository " + getRepository());
        service.removeRepository(getRepository());
		super.removedService(reference, service);
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		if(repository != null && repository.equals(getRepository())) return;
		RepositoryAdmin admin = getService();
		if(admin != null && getRepository() != null)
		{
		  LOG.log(LogService.LOG_INFO, "remove Repository " + getRepository());
		  admin.removeRepository(getRepository());
		}
		this.repository = repository;
		if(admin != null && getRepository() != null)
			try {
	            LOG.log(LogService.LOG_INFO, "addRepository " + getRepository());
				admin.addRepository(getRepository());
			} catch (Exception e) {
				LOG.log(LogService.LOG_ERROR, "addRepository failed", e);
			}
		
	}
}