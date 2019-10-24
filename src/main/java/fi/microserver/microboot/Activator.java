package fi.microserver.microboot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<ConditionalPermissionAdmin, ConditionalPermissionAdmin> {

  
  private final Logger LOG = Logger.getLogger(getClass().getName());
  ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin> tracker;
  BundleContext context;
  private StartBoot boot;
  private static final String LOCATION_PREFIX = "provisioning:";

  @Override
  public void start(BundleContext context) throws Exception {

    this.context = context;
    tracker = new ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin>(context, ConditionalPermissionAdmin.class, this);
    tracker.open();

    JULHandler.install(context);
    installEvent();
// start all "provision" bundles except me.
    Bundle[] bundles = context.getBundles();
    boot = new StartBoot(context);
    Runnable go = (new Runnable() {
      public void run() {
        for (int i = 1; i < bundles.length; i++) {
          Bundle b = bundles[i];
          if (b == context.getBundle()) continue;
          String loc = b.getLocation();
          int state = b.getState();
          String fragment = b.getHeaders().get(Constants.FRAGMENT_HOST);
          if (loc.startsWith(LOCATION_PREFIX) && fragment == null
              && (state == Bundle.INSTALLED || state == Bundle.RESOLVED))
            try {
            b.start();
          } catch (Exception e) {
            LOG.log(Level.WARNING, "starting " + loc, e);
          }
        }
        try {
          boot.start();
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "starting bootloader", e);
          displayException(e);
          stop();
        }

      }
    });
    Executors.newSingleThreadExecutor().execute(go);

  }

  @Override
  public void stop(BundleContext context) throws Exception {
    boot.stop();
    tracker.close();
    JULHandler.uninstall(context);
  }

  private void displayException(final Throwable t) {
    try {
        Runnable run = 
        new Runnable() {
            public void run() {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                pw.close();
                JTextArea area = new JTextArea(sw.toString());
                JOptionPane.showMessageDialog(null, new JScrollPane(area));
            }
        };
        if(SwingUtilities.isEventDispatchThread()) {
          run.run();
        } else {
          SwingUtilities.invokeAndWait(run);
        }
    } catch (InvocationTargetException e) {
        LOG.log(Level.SEVERE, "displayException fails", e);
    } catch (InterruptedException e) {
      LOG.log(Level.SEVERE, "displayException fails", e);
    }
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
 
  private ServiceRegistration<?> registration;
  private static final String STOP_EVENT = "fi/microserver/MicroServer/STOP";

  private void installEvent() {
    EventHandler service;
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
    properties.put(Constants.SERVICE_VENDOR, "fi.microserver.MicroServer");
    properties.put(EventConstants.EVENT_TOPIC, STOP_EVENT);
    service = new EventHandler() {

        public void handleEvent(Event event) {
            if(STOP_EVENT.equals(event.getTopic()))
            {
                Throwable t = (Throwable) event.getProperty(EventConstants.EXCEPTION);
                if(t != null) displayException(t);
                stop();
            }
        } };
    registration = 
    context.registerService(EventHandler.class, service, properties);
    
}

  void stop() {
    SwingUtilities.invokeLater(
    new Runnable() {

        public void run() {
            try {
                registration.unregister();
                context.getBundle(0L).stop();
            } catch (Exception e) {
                displayException(e);
            }
        }
    });
}

}
