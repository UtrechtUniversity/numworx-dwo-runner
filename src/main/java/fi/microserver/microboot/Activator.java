package fi.microserver.microboot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<ConditionalPermissionAdmin, ConditionalPermissionAdmin> {

  
  private final Logger LOG = Logger.getLogger(getClass().getName());
  ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin> tracker;
  BundleContext context;
  
  @Override
  public void start(BundleContext context) throws Exception {
    this.context = context;
    tracker = new ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin>(context, ConditionalPermissionAdmin.class, this);
    tracker.open();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    tracker.close();
  }

  @Override
  public ConditionalPermissionAdmin addingService(
      ServiceReference<ConditionalPermissionAdmin> reference) {
    ConditionalPermissionAdmin admin = context.getService(reference);
    if (admin == null) return null;
    LOG.info("installing security policy");
    List<String> lines = Collections.emptyList();
    try {
      lines = readPolicyFile(getClass().getResourceAsStream("/info.txt"));
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "read profile", e);
    }
    String line = 
     "ALLOW {"                                          
        + "[org.osgi.service.condpermadmin.BundleLocationCondition \""
        + context.getBundle().getLocation() + "\"]"
        + "(java.security.AllPermission \"*\" \"*\")"
        + "} \"Management Agent Policy\"";
    
    ConditionalPermissionUpdate update = admin.newConditionalPermissionUpdate();
    List<ConditionalPermissionInfo> list = update.getConditionalPermissionInfos();
    list.clear();

    ConditionalPermissionInfo info = admin.newConditionalPermissionInfo(line);
    list.add(info);
    for(String l: lines) {
      info = admin.newConditionalPermissionInfo(l);
      list.add(info);
    }
    if (!update.commit())
      LOG.warning("Commit failed");
      ;
    return admin;
  }

  @Override
  public void modifiedService(ServiceReference<ConditionalPermissionAdmin> reference,
      ConditionalPermissionAdmin service) {
  }

  @Override
  public void removedService(ServiceReference<ConditionalPermissionAdmin> reference,
      ConditionalPermissionAdmin service) {
    ConditionalPermissionUpdate update = service.newConditionalPermissionUpdate();
    List<ConditionalPermissionInfo> list = update.getConditionalPermissionInfos();
    list.clear();
    update.commit();
    context.ungetService(reference);
    LOG.info("removed security policy");
  }


  private List<String> readPolicyFile(InputStream policyFile) throws Exception {
    BufferedReader policyReader = null;
    Exception org = null;
    try
    {
        policyReader = new BufferedReader(new InputStreamReader(policyFile, "UTF-8"));
        List policy = new ArrayList();
        StringBuffer buffer = new StringBuffer();
        for (String input = policyReader.readLine(); input != null; input = policyReader.readLine()) {
            if (!input.trim().startsWith("#")) {
              buffer.append(input);
              if (input.contains("}")) {
                policy.add(buffer.toString());
                buffer = new StringBuffer();
              }
            }
        }
        return policy;
    }
    catch (Exception ex) {
        org = ex;
        throw ex;
    }
    finally {
        if (policyReader != null) {
            try
            {
                policyReader.close();
            }
            catch (Exception ex) {
                if (org == null) {
                    throw ex;
                }
            }
        }
    }
}

}
