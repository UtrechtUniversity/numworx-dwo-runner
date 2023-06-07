package fi.dwo.eawt.impl;

import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  @Override
  public void start(BundleContext context) throws Exception {
    context.registerService(fi.dwo.eawt.EAWT.class, new EAWTImpl(), new Hashtable<>());
  }

  @Override
  public void stop(BundleContext context) throws Exception {
  }

}
