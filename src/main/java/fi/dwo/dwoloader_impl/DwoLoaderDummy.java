package fi.dwo.dwoloader_impl;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import fi.dwo.bootloader.LoaderBuilder.Update;
import fi.dwo.dwoloader.DwoLoader;

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