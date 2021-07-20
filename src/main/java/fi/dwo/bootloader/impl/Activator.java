package fi.dwo.bootloader.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fi.dwo.bootloader.LoaderBuilder;
import fi.dwo.bootloader.LoaderBuilder.Update;
import fi.dwo.bootloader.LoaderBuilderFactory;
import fi.dwo.dwoloader.DwoLoader;

public class Activator implements BundleActivator {

	public class Updater implements ServiceTrackerCustomizer<DwoLoader, Update> {

	private Deferred<Update> defer = new Deferred<>();
	
	Promise<Update> getUpdate() {
	  return defer.getPromise();
	}

	@Override
    public Update addingService(ServiceReference<DwoLoader> reference) {
      try {
        Update update = context.getService(reference).getUpdate();
        defer.resolve(update);
        return update;
      } catch (Exception e) {
        defer.fail(e);
        return null;
      }
    }

    @Override
    public void modifiedService(ServiceReference<DwoLoader> reference, Update service) {
    }

    @Override
    public void removedService(ServiceReference<DwoLoader> reference, Update service) {
    }

  }

	private LogTracker LOGt;
	private LogReaderTracker LOGtt;
	private ServiceTracker<DwoLoader, Update> dwoloader;
	private Updater updater;

	private static final long DELAY = 600000L;

	private CMTracker ct;
	private Config config;

	public void start(final BundleContext context) throws Exception {
		final Bundle me = context.getBundle();
		String u = getUpdate(context);
		if(!Update.NEVER.name().equals(u)) {
		final InputStream modified = new Builder(null).setContext(context)
				.getInputStream(me.getLocation(), me.getLastModified());
		if (modified != null) {
			context.addBundleListener(new BundleListener() {

				public void bundleChanged(BundleEvent event) {
					if (event.getBundle() == me
							&& event.getType() == BundleEvent.STARTED)
						try {
							context.removeBundleListener(this);
							// BundleContext bootcontext =
							// context.getBundle(0).getBundleContext();
							// String location = me.getLocation();
							// me.uninstall();
							me.update(modified);
							// Bundle b = bootcontext.installBundle(location,
							// modified);
							// b.start(Bundle.START_TRANSIENT);
						} catch (BundleException e) {
							Activator.this.context = context;
							fatalError(e);
						}
				}
			});
			return;
		}}
		this.context = context;
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		LOGt = new LogTracker(context);
		LOGt.open();
		config = new Config(context, LOGt);

		String bundles = (String) config.getProperty("fi.dwo.bundles");
		if (bundles == null)
			bundles = BUNDLES;
		properties.put("fi.dwo.bundles", bundles);
		ServiceRegistration<?> registration;
		registration = context.registerService(
				LoaderBuilderFactory.class.getName(), new FactoryFactory(context),
				properties);
		boot(registration.getReference());
	}

  String getUpdate(final BundleContext context) {
    try {
      ServiceReference<?> r = context.getServiceReference("org.osgi.service.provisioning.ProvisioningService");
      if (r != null) {
        ProvisioningService service = (ProvisioningService) context.getService(r);
        Dictionary<?, ?> provision = service.getInformation(); context.ungetService(r);
        Object update = provision.get("fi.dwo.update");
        if (update != null) return update.toString();
      }
    } catch (Exception e) {
    }
    return context.getProperty("fi.dwo.update");
  }

	private String BUNDLES = "http://cdn.dwo.nl/bundles/";
	private String DWOJAPPLET = "https://app.dwo.nl/dwo/DWOJApplet.jar";
	private String CONSOLE = "org.eclipse.concierge.shell-5.0.0.20151029184259.jar";
	//private String EVENT_ADMIN = "org.apache.felix.eventadmin-1.4.8.jar";;
	private String DWOJAPPLET_STARTER = "DWOJApplet-Starter-2.0.jar";
	private String PAX_URL_WRAP = "pax-url-wrap-2.4.7.jar";
	private String UNPACK200 = "unpack200-0.0.1.jar";
	private String SLF4J = "slf4j-jdk14-1.7.21.jar";
	private String DWO_LOADER = "DwoLoader-0.0.2-SNAPSHOT.jar";

	private BundleContext context;

	private String dwojapplet;

	private void boot(ServiceReference<?> serviceReference)
			throws BundleException, URISyntaxException {

		LoaderBuilder builder;
		LoaderBuilderFactory factory = (LoaderBuilderFactory) context
				.getService(serviceReference);
		builder = factory.newInstance();
		builder.setUpdate(Update.NEVER);
		installDwoLoader(builder);
		installSLF4J(builder);
		installConsole("true".equals(config.getProperty("fi.dwo.console")),
				builder);
		
		installJXBrowser(builder);
		
		
		installEventAdmin(builder);
		installCM(builder);
		installWrap(builder, factory);
		installUnpack200(builder);

		config.getPromise().then((p) -> ct.open(p.getValue()))
				.then((p) -> checkVersion(p)).then((p) -> {
					Config config = p.getValue();
					String u = (String) config.getProperty("fi.dwo.update");
					Update update = Update.MAYBE;
					if (u != null) {
						try {
							update = Update.valueOf(u);
						} catch (Exception e) {
							LOGt.log(LogService.LOG_WARNING, e.toString());
						}
					}
					builder.setUpdate(update);
					return installDWO(builder);
				}, (p) -> fatalError(p.getFailure()))
				.onResolve(() -> {context.ungetService(serviceReference);});
;
	}

	private void installDwoLoader(LoaderBuilder builder) throws BundleException, URISyntaxException {
      try {
		builder.setLocation(DWO_LOADER).start("fi.dwo.DwoLoader");
		  updater = new Updater();
		  dwoloader = new ServiceTracker<>(context, DwoLoader.class, updater);
		  dwoloader.open();
// after 0.0.5 never null
		  if (null == context.getProperty(Constants.FRAMEWORK_STORAGE_CLEAN))
		    new Thread() {

		      @Override
		      public void run() {
		        try {
		          Thread.sleep(1000L);
		        } catch (InterruptedException e) {
		        }
		        updater.defer.resolve(Update.MAYBE);
		      }
		    
		  } .start();
	} catch (BundleException e) {
		e.printStackTrace();
	} catch (URISyntaxException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
        
  }

  private void installSLF4J(LoaderBuilder builder) throws BundleException, URISyntaxException {
	  builder.setLocation(SLF4J).start("slf4j.jdk14");
	  try {
        String version = System.getProperty("java.version", "0.0.0");
        if ( Integer.parseInt(version.split("\\.")[0]) >= 9)
          builder.setLocation("fi.dwo.eawt-0.0.1.jar").start("fi.dwo.eawt");
    } catch (Exception e) {
      LOGt.log(LogService.LOG_WARNING, "eawt", e);
    }
  }
  
  private void installJXBrowser(LoaderBuilder builder) {
//	  try {
//		  builder.setLocation("non-existent");
//		  builder.start("com.teamdev.jxbrowser.mac");
//	  } catch (Exception e) {
//	      LOGt.log(LogService.LOG_WARNING, "jxbrowser.mac", e);		  
//	  }
//	  try {
//		  builder.setLocation("non-existent");
//		  builder.start("com.teamdev.jxbrowser.linux");
//	  } catch (Exception e) {
//	      LOGt.log(LogService.LOG_WARNING, "jxbrowser.linux", e);		  
//	  }
//	  try {
//		  builder.setLocation("non-existent");
//		  builder.start("com.teamdev.jxbrowser.windows");
//	  } catch (Exception e) {
//	      LOGt.log(LogService.LOG_WARNING, "jxbrowser.windows", e);		  
//	  }
//	  try {
//		  builder.setLocation("non-existent");
//		  builder.start("com.teamdev.jxbrowser.swing");
//	  } catch (Exception e) {
//	      LOGt.log(LogService.LOG_WARNING, "jxbrowser.swing", e);		  
//	  }
	  
  }
  
  

  private Promise<Config> checkVersion(final Promise<Config> p)
			throws Exception {
		Config config = p.getValue();
		Version microserver = Version.emptyVersion, myversion;
		try {
			microserver = new Version(
					(String) config.getProperty("fi.microserver.version"));
		} catch (Exception e) {
		}
		myversion = new Version(0, 0, 5, ""); // VERSION 0.0.5 minimum
		if (myversion.compareTo(microserver) <= 0)
			return p;
		Long last = (Long) config.getProperty("fi.dwo.boot.last");
		if (last == null)
			last = 0L;
		long now = System.currentTimeMillis();
		if (now < last + DELAY)
			return p;
		final Deferred<Config> df = new Deferred<Config>();
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				String[] options = { "Quit", "Later", "Continue" };
				Object defaultOption = options[0];
				int opt = JOptionPane.showOptionDialog(null, "Update required",
						"Update", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, null);
				if (opt == JOptionPane.NO_OPTION)
					config.setProperty("fi.dwo.boot.last", now);
				if (opt == JOptionPane.YES_OPTION)
					df.fail(NULL);
				else
					df.resolveWith(p);
			}
		});
		return df.getPromise();
	}

	public void stop(BundleContext context) throws Exception {
		if (config != null) {
			config.close();
			config = null;
		}
		if (ct != null) {
			ct.close();
			ct = null;
		}
		if (LOGtt != null) 
			LOGtt.close();
		if(LOGt != null)
			LOGt.close();
	}

	private Promise<?> installDWO(LoaderBuilder builder) throws URISyntaxException,
			BundleException, InvalidSyntaxException, InvocationTargetException, InterruptedException {
		dwojapplet_starter = (String) config.getProperty("fi.dwo.dwojapplet");
		if (dwojapplet_starter == null)
			dwojapplet_starter = DWOJAPPLET_STARTER;
		dwojapplet = (String) config
				.getProperty("fi.dwo.dwojapplet.domain.DWO");
		if (dwojapplet == null)
			dwojapplet = DWOJAPPLET;
		Update update = builder.getUpdate();
		try {
			if(!noProtocol("pack200")) {
			  dwojapplet = "pack200:" + dwojapplet + ".pack.gz"; // use pack200 version, geen ifModifiedSince
			}
			Promise<Update> p = updater.getUpdate();
			return p.then(
			  u -> {
			    installSwingBrowser(builder);
            builder.setLocation(dwojapplet).setUpdate(u.getValue()).start(
					"fi.dwo.dwojapplet.domain.DWO");
			builder.setLocation(dwojapplet_starter).setUpdate(update).start("fi.dwo.dwojapplet");
			return u; }).then(null, u -> {
	           builder.setUpdate(update);
	           LOGt.log(LogService.LOG_ERROR, "installing DWO", u.getFailure());
	           fatalError(u.getFailure());
			}
			);  
		} finally {
		}
	}

	final private Throwable NULL = new Throwable();

	private void fatalError(Throwable throwable) {
		ServiceReference<EventAdmin> ref = context
				.getServiceReference(EventAdmin.class);
		Map<String, Object> hash = new HashMap<String, Object>();

		if (throwable != NULL)
			hash.put(EventConstants.EXCEPTION, throwable);
		Event event = new Event("fi/microserver/MicroServer/STOP", hash);

		if (ref != null) {
			EventAdmin admin = context.getService(ref);
			if (admin != null) {
				admin.postEvent(event);
				context.ungetService(ref);
			}
		} else {
			// CHEAT
			String filter = "(" + Constants.SERVICE_VENDOR
					+ "=fi.microserver.MicroServer)";
			Collection<ServiceReference<EventHandler>> col;
			try {
				col = context.getServiceReferences(EventHandler.class, filter);
				if (!col.isEmpty()) {
					EventHandler handler = context.getService(col.iterator()
							.next());
					if (handler != null) {
						handler.handleEvent(event);
						context.ungetService(col.iterator().next());
					}
				}
			} catch (InvalidSyntaxException e1) {
			}
		}
	}

	private void installConsole(boolean start, LoaderBuilder builder) throws BundleException, URISyntaxException {
		File std = null;
		PrintStream print = null;
		try {
			std = context.getDataFile("DWO-docent.log");
			FileOutputStream out = new FileOutputStream(std);
			print = new PrintStream(out, true, "UTF-8");
			LOGtt = new LogReaderTracker(context, print);
			LOGtt.open();
		} catch (Exception e) {
			LOGt.log(LogService.LOG_WARNING, "redirect to " + std, e);
		}

		if (start)
			builder.setLocation(CONSOLE).start("org.eclipse.concierge.shell");
		else {
			builder.setLocation(CONSOLE).stop("org.eclipse.concierge.shell");
			if (print != null) {
				System.setErr(print);
				System.setOut(print);
		}}
	}

	private final static String WRAP = "org.ops4j.pax.url.wrap";

	private void installWrap(LoaderBuilder builder, LoaderBuilderFactory factory)
			throws BundleException, URISyntaxException {
		if(noProtocol("wrap"))
			builder.setLocation(PAX_URL_WRAP).start(WRAP);
		
	}
	
	private void installSwingBrowser(LoaderBuilder builder) {
	  try {
        builder.setLocation("swingbrowser-jxb.jar").start("nl.numworx.swingbrowser.jxb");
      } catch (Exception e) {
        LOGt.log(LogService.LOG_ERROR, "swingbrowser jxb", e);
      }
//	  try {
//	    builder.setLocation("swingbrowser-jfx.jar").start("nl.numworx.swingbrowser.jfx");
//	  } catch (Exception e) {
//	        LOGt.log(LogService.LOG_WARNING, "swingbrowser jfx", e);
//	  }
	}
		
	private void installUnpack200(LoaderBuilder builder) throws BundleException, URISyntaxException {
	  if (noProtocol("pack200"))
	      builder.setLocation(UNPACK200).start("fi.dwo.unpack200");
	}
	
	
	private boolean noProtocol(String protocol) {
		String clazz = "org.osgi.service.url.URLStreamHandlerService";
		String filter = "(url.handler.protocol="+ protocol + ")";
		try {
			ServiceReference<?>[] result = context.getAllServiceReferences(clazz, filter);
			return result == null || result.length<1;
		} catch (InvalidSyntaxException e) {
			LOGt.log(LogService.LOG_ERROR, "Should not happen: " + protocol, e);
			return false;
		}
	}
	
	private void installEventAdmin(LoaderBuilder builder)
			throws BundleException, URISyntaxException {
		//builder.setLocation(EVENT_ADMIN).start("org.apache.felix.eventadmin");
	}

	private final static String CM = "org.apache.felix.configadmin-1.8.10.jar";

	private String dwojapplet_starter = DWOJAPPLET_STARTER;

	private void installCM(LoaderBuilder builder) throws BundleException,
			URISyntaxException {
		config.open();
		builder.setLocation(CM).start("org.apache.felix.configadmin");
		LogService log = context.getService(context
				.getServiceReference(LogService.class));
		config.setLog(log);
		ct = new CMTracker(context, log);
	}
}
