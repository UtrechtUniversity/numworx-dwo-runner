package fi.microserver.microboot;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.service.repository.Repository;
import org.xml.sax.InputSource;

class StartBoot {

  final BundleContext context;
  private ServiceRegistration<Repository> reposRegistration;
  private ServiceRegistration<ManagedServiceFactory> factoryRegistration;
  
  StartBoot(BundleContext context) {
    this.context = context;
  }

  private String getProperty( String key, ProvisioningService ps) {
    Object value = ps.getInformation().get(key);
    if (value != null) return value.toString();
    return context.getProperty(key);
  }

  private void installRepository(String u, RepoImpl repos) throws Exception {
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(Repository.URL, u);
    properties.put(Constants.SERVICE_PID, "fi.dwo.repository");
    properties.put("repository.name", repos.name);
    properties.put("repository.increment", Long.valueOf(repos.increment));
    reposRegistration = context.registerService(Repository.class, repos, properties);   
}
  
  private void installFactory() throws Exception {
	  factoryRegistration = new RepoFactory(context).getRegistration();
  }
  
  static final String BOOTLOADER  = "fi.dwo.BootLoader";

  private  Bundle installBoot(String BOOT) throws BundleException {
    CapReqBuilder builder = new CapReqBuilder("osgi.identity");
    builder.addDirective("filter", "(osgi.identity="+ BOOTLOADER +")");
    Requirement r = builder.buildSyntheticRequirement();
    try {
        if (BOOT == null) {
            Repository repos = context.getService(reposRegistration.getReference());
            Map<Requirement, Collection<Capability>> providers = repos.findProviders(Collections.singleton(r));
            Collection<Capability> caps = providers.get(r);
            context.ungetService(reposRegistration.getReference());repos = null;
            for(Capability c: caps) {
                Resource res = c.getResource();
                List<Capability> list = res.getCapabilities("osgi.content");
                for(Capability cc: list)
                    BOOT = cc.getAttributes().get("url").toString();
            }
        } 
        return context.installBundle(BOOT);
    } catch (BundleException e) {
        int type = e.getType();
        if (type == BundleException.DUPLICATE_BUNDLE_ERROR) { // uninstall asap
            FrameworkWiring fw = context.getBundle(0).adapt(FrameworkWiring.class);
            Collection<BundleCapability> res = fw.findProviders(r);
            for (BundleCapability bc:res) {
                bc.getRevision().getBundle().uninstall();
                return installBoot(BOOT); // recurse
            }
        }
        throw e;
    }
}

  String increment;

  void start() throws Exception {
    Preferences pref = Preferences.userRoot().node("fi/microserver");
    increment = pref.get("increment", "");
    ServiceReference<ProvisioningService> ref = context.getServiceReference(ProvisioningService.class);
    if (ref == null) return;
    ProvisioningService ps = context.getService(ref);
    String u = getProperty("fi.dwo.repository", ps);
    RepoImpl repos;
    InputSource input = new InputSource(u);
    repos = new RepoImpl(input);
    if(!increment.equals(repos.increment)) {
        increment = repos.increment;
        Properties p = new Properties(); 
        p.setProperty("fi.dwo.update", "MAYBE");
        ps.addInformation(p);
    } else {
        if(!"NEVER".equals(getProperty("fi.dwo.update",ps)))
        {
          Properties p = new Properties(); p.setProperty("fi.dwo.update", "NEVER");
          ps.addInformation(p);
        }
    }
    String BOOT = getProperty("fi.dwo.boot", ps);
    context.ungetService(ref);
    repos.setContext(context);

    installRepository(u,repos);
    installFactory();
    Bundle boot = installBoot(BOOT);
    boot.start(Bundle.START_TRANSIENT);
  }
  
  
  void stop() {
    if (reposRegistration == null) return;
    reposRegistration.unregister(); reposRegistration = null;
    factoryRegistration.unregister();
    Preferences pref = Preferences.userRoot().node("fi/microserver");
    pref.put("increment", increment);
    try {
      pref.flush();
    } catch (BackingStoreException e) {
    }
  }
}
