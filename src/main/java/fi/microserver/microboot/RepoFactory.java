package fi.microserver.microboot;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.repository.Repository;
import org.xml.sax.InputSource;

class RepoFactory implements ManagedServiceFactory {

	private static final String REPOSITORY_NAME = "repository.name";
	private final String FACTORY_PID = getClass().getName();
	private final ServiceRegistration<ManagedServiceFactory> registration;
	private final Map<String, ServiceRegistration<Repository>> repos = new Hashtable<>();
	private final BundleContext context;
	
	RepoFactory(BundleContext context) {
		this.context = context;
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_PID, FACTORY_PID);		
		registration = context.registerService(ManagedServiceFactory.class, this, properties );
	}

	@Override
	public String getName() {
		return FACTORY_PID;
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		ServiceRegistration<Repository> r = repos.get(pid);
		if (r == null) {
			String u = (String) properties.get(Repository.URL);
			String name = (String) properties.get(REPOSITORY_NAME);
		    try {
			    InputSource input = new InputSource(u);
				RepoImpl rep = new RepoImpl(input);
				if (Objects.equals(name, rep.name)) {
				    Dictionary<String, Object> p = new Hashtable<String, Object>();
				    p.put(Repository.URL, u);
				    if (rep.name != null) p.put(REPOSITORY_NAME, rep.name);
				    p.put("repository.increment", Long.valueOf(rep.increment));
				    rep.setContext(context);
				    r = context.registerService(Repository.class, rep, properties);
				    repos.put(pid, r);
				} else {
					throw new ConfigurationException(REPOSITORY_NAME, "no match");
				}
		    } catch (ConfigurationException e) { throw e;
			} catch (Exception e) {
				throw new ConfigurationException(null, e.toString(), e);
			}
		}
	}

	@Override
	public void deleted(String pid) {
		ServiceRegistration<Repository> r = repos.remove(pid);
		r.unregister();
	}

	public ServiceRegistration<ManagedServiceFactory> getRegistration() {
		return registration;
	}

}
