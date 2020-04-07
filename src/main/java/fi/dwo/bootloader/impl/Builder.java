package fi.dwo.bootloader.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

import fi.dwo.bootloader.LoaderBuilder;

public class Builder implements LoaderBuilder {

	static private class RequirementImpl implements Requirement {
    private final Map<String,String> directives;
 
    private RequirementImpl(String symbolicname) {
      directives = Collections.singletonMap("filter", "(osgi.identity=" + symbolicname + ")");
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof Requirement)) {
        return false;
      }
      Requirement other = (Requirement) obj;
      if (!getAttributes().equals(other.getAttributes()))
        return false;
      if (!getDirectives().equals(other.getDirectives()))
        return false;
      if (!getNamespace().equals(other.getNamespace()))
          return false;
      if (other.getResource() != null)
            return false;
      return true;
     }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (directives.hashCode());
      return result;
    }

    String namespace = IdentityNamespace.IDENTITY_NAMESPACE;
 
    @Override
    public String getNamespace() {
	return namespace;
    }

    @Override
    public Map<String, String> getDirectives() {
      return directives;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return Collections.EMPTY_MAP;
    }

    @Override
    public Resource getResource() {
      return null;
    }
  }

  private static final String APPLICATION_VND_OSGI_BUNDLE = "application/vnd.osgi.bundle";
	private BundleContext context;
	private URI location;
	private Update update = Update.NEVER;
	private URI base;
	private Factory parent;

	private Collection<Resource> loadResources(ResolveContext context)
			throws ResolutionException {
		Resolver service = parent.resolver.getService();
		if(service == null) return Collections.emptySet();
		Map<Resource, List<Wire>> result = service.resolve(context);
		return result.keySet();
	}

	Builder(Factory parent) {
		super();
		this.parent = parent;
	}

	public Builder setContext(BundleContext context) {
		this.context = context;
		return this;
	}

	public LoaderBuilder setLocation(String location) throws URISyntaxException {
		this.location = new URI(location);
		if (base != null)
			this.location = base.resolve(this.location);
		return this;
	}

	public void start(String symbolicname) throws BundleException {
		start0(symbolicname);
	}
		
	private List<Bundle> start0(String symbolicname) throws BundleException {
		Update update = this.update;
		ResolveContextImpl rc = null;
		List<Bundle> bundles = new ArrayList<Bundle>();
		try {
			rc = fromResolver(symbolicname);
			Collection<Resource> resources = loadResources(rc); 
			update = installResources(update, rc, bundles, resources);
			resolve(bundles);
		} catch (ResolutionException e1) {
			log(LogService.LOG_WARNING, "fromResolver", e1);
		} catch (BundleException e2) {
			log(LogService.LOG_WARNING, "installBundle", e2);
		}

		URI location = this.location;
		location = fromRepository(symbolicname, location, rc);
		Bundle b = context.getBundle(location.toString());
		if (b == null) {
			b = parent.searchBundle0(symbolicname);
			if (b == null) {
				b = context.installBundle(location.toString());
				update = Update.NEVER;
			}
		} if (b.getState() != Bundle.ACTIVE)
		{
			switch (update) {
			case MAYBE:
				long mod = b.getLastModified();
				// if (mod > getModified(location.toString(),mod)) // expire
				// strategy
				// break;
				InputStream in = getInputStream(location.toString(), mod);
				if (in != null)
					b.update(in);
				break;
			case ALWAYS:
				try {
					in = location.toURL().openStream();
					b.update(in);
				} catch (IOException e) {
					throw new BundleException("start",
							BundleException.READ_ERROR, e);
				}
			case NEVER:
				break;
			}
		}
		parent.addBundle(b);
		bundles.remove(b);
		bundles.add(0, b);
		if (b.getHeaders().get(Constants.FRAGMENT_HOST) == null)
			b.start(Bundle.START_TRANSIENT);
		return bundles;
	}

  public Update installResources(Update update, ResolveContextImpl rc, Collection<Bundle> bundles,
      Collection<Resource> resources) throws BundleException {
    for (Resource res : resources) {
        if (res instanceof BundleRevision) {
          Bundle bundle = ((BundleRevision) res).getBundle();
          bundles.add(bundle);
          continue;
        }
    	List<Capability> list = res.getCapabilities("osgi.content");
    	String bundlename = getBundleName(res);
    	for (Capability cc : list) {
    		String mime = cc.getAttributes().get("mime").toString();
    		if(!APPLICATION_VND_OSGI_BUNDLE.equals(mime))
    			continue;

    		String url = cc.getAttributes().get("url").toString();
    		Bundle bundle = findBundle(bundlename, rc);
    		if(bundle == null)
    		{
                update = Update.NEVER;
    		    Version vers = getBundleVersion(res);
    		    bundle = parent.searchBundle0(bundlename);
    		    if (bundle != null) {
    		      if (!bundle.getVersion().equals(vers) && bundle.getState() <= Bundle.RESOLVED) {
    		        bundle = update(url, bundle);
    		      } 
    		    } else
                  bundle = context.installBundle(url);
     		} else {
     		  // check version here.
              Version vers = getBundleVersion(res);
              if (!bundle.getVersion().equals(vers) && bundle.getState() <= Bundle.RESOLVED) {
                bundle = update(url, bundle);
                update = Update.NEVER;
              }
     		}
    		bundles.add(bundle);
    	}
    }
    return update;
  }

  private Bundle update(String url, Bundle bundle) throws BundleException {
      try {
        InputStream in = new URL(url).openStream();
        bundle.update(in);
        in.close();
        return bundle;
      } catch (IOException e) {
        log(LogService.LOG_WARNING, "update failed", e);
      } 
      bundle.uninstall();
      bundle = context.installBundle(url);
      return bundle;
  }

	private String getBundleName(Resource res) {
      List<Capability> list = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
      for (Capability cap: list) {
        return (String) cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
      }
    return null;
  }
	
	private Version getBundleVersion(Resource res) {
	  List<Capability> list = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
	  for(Capability cap: list) {
	    return (Version) cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
	  }
	  return null;
	}

  public Bundle findBundle(String symbolicname, ResolveContextImpl rc) {
		try {
			return rc.find(getRequirement(symbolicname)).iterator().next().getRevision().getBundle();
		} catch (Exception e) {
			return null;
		}
	}

	private URI fromRepository(String symbolicname, URI location, ResolveContext rc) {
		if(rc == null) return location;
		for(Resource res : rc.getMandatoryResources()) {
			if (res instanceof BundleReference) {
				Bundle b = ((BundleReference) res).getBundle();
				location = URI.create(b.getLocation());
			}
			List<Capability> list = res.getCapabilities("osgi.content");
			for (Capability cc : list)
			{
				String mime = cc.getAttributes().get("mime").toString();
				if(!APPLICATION_VND_OSGI_BUNDLE.equals(mime))
					continue;

				location = URI.create(cc.getAttributes().get("url").toString());
			}
		}
		return location;
	}

	final static private Repository[] EMPTY = new Repository[0];
	private ResolveContextImpl fromResolver(String symbolicname)
			throws ResolutionException {
		Repository[] repos = parent.repository.getServices(EMPTY);
		if (repos == null)
			repos = EMPTY;
		ResolveContextImpl rc = new ResolveContextImpl(repos, context);
		Requirement r = getRequirement(symbolicname);
		List<Capability> caps = rc.findProviders(r);
		Collections.sort(caps, new Comparator<Capability>() {

			@Override
			public int compare(Capability o1, Capability o2) {
				Version v1 = (Version) o1.getAttributes().get("version");
				Version v2 = (Version) o2.getAttributes().get("version");
				int r = v1.compareTo(v2);
				if (r == 0) {
// prefer bundle capability if same version
					if(o1 instanceof BundleCapability && !(o2 instanceof BundleCapability)) r = +1;
					else if (o2 instanceof BundleCapability && !(o1 instanceof BundleCapability)) r = -1;
				}
				return -r;
			}});
		for (Capability c : caps) {
			Resource res = c.getResource();
			rc.getMandatoryResources().add(res); break;
		}
		return rc;
	}

	private URI fromRepository(String symbolicname, URI location) {
		Repository[] repos = parent.repository.getServices(new Repository[1]);
		if (repos[0] == null)
			return location;
		// in een 1.1 Repository weer anders
		Requirement r = getRequirement(symbolicname);
		Map<Requirement, Collection<Capability>> providers = repos[0]
				.findProviders(Collections.singleton(r));
		Collection<Capability> caps = providers.get(r);
		for (Capability c : caps) {
			Resource res = c.getResource();
			List<Capability> list = res.getCapabilities("osgi.content");
			for (Capability cc : list)
				location = URI.create(cc.getAttributes().get("url").toString());
		}
		return location;
	}

//	private Requirement getRequirement(String symbolicname) {
//		CapReqBuilder builder = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
//		builder.addDirective("filter", "(osgi.identity=" + symbolicname + ")");
//		Requirement r = builder.buildSyntheticRequirement();
//		return r;
//	}

	private Requirement getRequirement(String symbolicname) {  
	  return new RequirementImpl(symbolicname);
	}
	
	
	public LoaderBuilder setUpdate(Update strategy) {
		this.update = strategy;
		return this;
	}

	public LoaderBuilder setBase(String base) {
		if (base == null)
			this.base = null;
		else
			this.base = URI.create(base);
		return this;
	}

	@Deprecated
	long getModified(String location) {
		URLConnection uc;
		try {
			URL u = new URL(location);
			uc = u.openConnection();
			uc.setDoInput(true); // will not work
			uc.setDoOutput(false);
			uc.connect();
			long last = uc.getLastModified();
			// how to close ?
			if (uc instanceof HttpURLConnection)
				((HttpURLConnection) uc).disconnect();
			return last;
		} catch (IOException e) {
			log(LogService.LOG_ERROR, e.toString());
			return 0L;
		}
	}

	long getModified(String location, long since) {
		URLConnection uc;
		try {
			URL u = new URL(location);
			uc = u.openConnection();
			uc.setDoInput(true); // will not work
			uc.setDoOutput(false);
			uc.setIfModifiedSince(since);
			uc.connect();
			long last = uc.getLastModified();
			// how to close ?
			if (uc instanceof HttpURLConnection)
				((HttpURLConnection) uc).disconnect();
			return last;
		} catch (IOException e) {
			log(LogService.LOG_ERROR, e.toString());
			return 0L;
		}
	}

	InputStream getInputStream(String location, long since) {
		URLConnection uc;
		try {
			URL u = new URL(location);
			uc = u.openConnection();
			uc.setIfModifiedSince(since);
			uc.connect();
			int status = 0;
			if (uc instanceof HttpURLConnection)
				status = ((HttpURLConnection) uc).getResponseCode();
			long last = uc.getLastModified();
			if (last == 0L && status != 304 || last > since)
				return uc.getInputStream();
			uc.getInputStream().close();
			if (uc instanceof HttpURLConnection)
				((HttpURLConnection) uc).disconnect();
		} catch (IOException e) {
			log(LogService.LOG_ERROR, e.toString());
		}
		return null;
	}

	public void stop(String name) throws BundleException {
		Bundle b = context.getBundle(location.toString());
		if (b != null) {
			b.stop(Bundle.STOP_TRANSIENT);
		} else {

		}
	}

	private boolean isBundle(String main) {
	   Repository[] repositories = parent.repository.getServices(EMPTY);
	   if (repositories == null) repositories = EMPTY;
	   ResolveContextImpl rc = new ResolveContextImpl(repositories, context);
	   Requirement requirement = getRequirement(main);
	   List<Capability> result = rc.findProviders(requirement);
	   for(Capability cap: result) {
		   Object type = cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
		   if (IdentityNamespace.TYPE_BUNDLE.equals(type))
			   return true;
	   }
	   
	   return false;
	}
	
	
	
	public List<Bundle> startWrap(String main) throws BundleException {
		// prolog

		if (isBundle(main)) {
			return start0(main);
		}
		
		uninstall(main);
		Set<URI> work = new HashSet<URI>();
		Set<URI> done = new HashSet<URI>();
		List<Bundle> bundles = new ArrayList<Bundle>();
		URL U = getClass().getResource("resources/" + main);
		String bnd = "," + getClass().getResource("resources/common.bnd")
				+ "$Bundle-SymbolicName=" + main;
		if (U != null)
			bnd = "," + U;

		work.add(location);
		boolean first = true;
		while (!work.isEmpty()) {
			Iterator<URI> list = work.iterator();
			URI jar = list.next();
			list.remove();
			if (done.add(jar)) {
				String wrap = "wrap:" + jar + bnd;
				Bundle b;
				try {
					log(LogService.LOG_DEBUG, wrap);
					if (first)
						b = istart(jar, wrap);
					else {
						wrap += "&Bundle-SymbolicName=" + jar.getPath().replace('/', '_');
						b = istart(jar, wrap);
					}
					first = false;
					bundles.add(b);
					parent.addBundle(b);
					Dictionary<String, String> headers = b.getHeaders();
					String classPath = headers.get("Class-Path");
					if (classPath != null) {
						String[] jars = classPath.split(" ");
						for (String item : jars) {
							work.add(jar.resolve(item));
						}
					}
				} catch (Exception e) {
					log(LogService.LOG_WARNING, e.toString());
				}
				bnd = "," + getClass().getResource("resources/fragment.bnd") + "$" + Constants.FRAGMENT_HOST + "=" + main;
			}
		}
		
		extraResources(bundles);
		
		resolve(bundles);
		return bundles;
	}

	private void extraResources(List<Bundle> bundles) throws BundleException {
      Repository[] repositories = parent.repository.getServices(EMPTY);
      if (repositories == null) repositories = EMPTY;
      ResolveContextImpl rc = new ResolveContextImpl(repositories, context);   
      rc.noFilter(); // allow resolved/installed bundles.
      bundles.forEach(b -> rc.getMandatoryResources().add(b.adapt(BundleRevision.class)));
      try {
        Collection<Resource> resources = loadResources(rc); // dit gaat mis omdat nu de installed bundles niet meer in de repositories zitten.
        Set<Bundle> set = new HashSet<>(bundles);
        installResources(Update.NEVER, rc, set, resources);
        set.forEach(b -> {if (!bundles.contains(b)) bundles.add(b);});
      } catch (ResolutionException e) {
        log(LogService.LOG_WARNING, "fromResolver", e);
      } catch (BundleException e) {
        log(LogService.LOG_ERROR, "fromResolver", e);      
      } catch (RuntimeException e) {
        log(LogService.LOG_ERROR, "fromResolver", e);
      } catch (Error e) {
        log(LogService.LOG_ERROR, "fromResolver", e);

      }
  }

  private void uninstall(String main) {
		Bundle b = parent.searchBundle0(main);
		if (b == null)
			return;
		String location = b.getLocation();
		if (location.equals(this.location.toString()))
			return;

		try {
			b.uninstall();
			uninstallFragments(main);
		} catch (BundleException e) {
			log(LogService.LOG_ERROR, e.toString());
		}
	}

	private void uninstallFragments(String name) {
		Collection<Bundle> collection = parent.fragmentTracker.getObject(name);
		for (Bundle bundle : collection) {
			try {
				bundle.uninstall();
			} catch (BundleException e) {
				log(LogService.LOG_ERROR, e.toString());
			}
		}
	}

	private Bundle istart(URI location, String wrap) throws BundleException,
			IOException {
		Bundle b = context.getBundle(location.toString()); // if location
															// changes, but
															// symbolicname not,
															// you err.
		InputStream is = null;
		if (b == null) {
			if (wrap != null)
				is = new URL(wrap).openStream();
			b = context.installBundle(location.toString(), is);
		} else {
			switch (update) {
			case MAYBE:
				long mod = b.getLastModified();
				if (mod > getModified(location.toString(), mod)) // expire
																	// strategy
					break;
			case ALWAYS:
				if (wrap != null)
					is = new URL(wrap).openStream();
				b.update(is);
			case NEVER:
				break;
			}
		}
		return b;
	}

	private void resolve(Collection<Bundle> list) throws BundleException {
		FrameworkWiring fw;
		fw = context.getBundle(0).adapt(FrameworkWiring.class);
		if (fw != null)
			fw.resolveBundles(list);
		for (Bundle b : list) {
			if (b.getHeaders().get(Constants.FRAGMENT_HOST) == null)
				b.start(Bundle.START_TRANSIENT);
		}
	}

	private void log(int logDebug, String wrap) {
		log(logDebug, wrap, null);
	}

	private void log(int logDebug, String wrap, Throwable t) {
		ServiceReference<LogService> ref = context
				.getServiceReference(LogService.class);
		if (ref != null) {
			LogService log = context.getService(ref);
			if (log != null) {
				log.log(logDebug, wrap, t);
				context.ungetService(ref);
			}
		}
	}

	@Override
	public Update getUpdate() {
		return update;
	}

}
