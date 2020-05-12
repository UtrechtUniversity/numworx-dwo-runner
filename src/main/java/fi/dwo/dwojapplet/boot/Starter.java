package fi.dwo.dwojapplet.boot;

import java.applet.Applet;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.service.provisioning.ProvisioningService;
import fi.beans.loader.Loader;
import fi.beans.mainframe.MainFrame;
import fi.dwo.bootloader.LoaderBuilder.Update;
import fi.dwo.bootloader.LoaderBuilderFactory;
import fi.dwo.dwojapplet.domain.DWO;
import fi.dwo.dwojapplet.gui.GuiConstants;
import fi.dwo.eawt.EAWT;

public class Starter implements BundleActivator {
	
    public final static String PROVISIONING_UPDATE_COUNT   = "provisioning.update.count";
    private ServiceReference<?> ref;
	private Object factory;
	private Dictionary<String,?> dict;
	private Map<String, String> parameters = new Hashtable<String,String>();
	private ReposAdmin reposAdmin;
	
	class ConfigHandler implements ManagedService
	{
		private BundleContext context;

		ConfigHandler(BundleContext context) {
			this.context = context;
		}

		@Override
		public void updated(Dictionary<String, ?> properties)
				throws ConfigurationException {
			if(properties != null) {
				dict = properties;
				Enumeration<String> keys = properties.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();				
					Object value = properties.get(key);
					if(value instanceof String) {
						if("fi.dwo.jarindex".equals(key)) {
							reposAdmin.setRepository((String) value);
						} else
						if("fi.dwo.codebase".equals(key))
							codebase = value.toString();
						else if ("fi.dwo.documentbase".equals(key))
							documentbase = value.toString();
						else
							parameters.put(key, (String) value);
					}
				}
				
			}
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						try {
							if(dwo == null)
								runDWO(context);
						} catch (MalformedURLException e) {
							log.log(LogService.LOG_ERROR, "runDWO", e);
						}
					}
				});
		}
	}
	
	
	public class StarterCreator {
		BundleContext context;
		WeakHashMap<String, ClassLoader> map = new WeakHashMap<String, ClassLoader>();
		protected String base;
		
		public void setBase(String base) {
			if(!base.endsWith("/")) 
				base += "/";
			this.base = base;
		}

		StarterCreator(BundleContext context) {
			super();
			this.context = context;
		}

		public ClassLoader create(String jar, ClassLoader parent) {
			ClassLoader value;
			value = map.get(jar);
			if (value == null) {
				value = new StarterClassLoader(base ,
				jar, context, parent, (LoaderBuilderFactory) factory);
				map.put(jar, value);
			}
			return value;
		}	
	}
	
	class DummyStarterCreator extends StarterCreator {

		DummyStarterCreator(BundleContext context) {
			super(context);
		}

		public ClassLoader create(String jar, ClassLoader parent) {
			ClassLoader value = map.get(jar);
			if (value == null) {
				URL[] urls;	
				try {	
					urls = new URL[] { new URL(base + jar) };
				} catch (MalformedURLException e){
					urls = new URL[0];
				}
				value = new URLClassLoader(urls, parent);
				map.put(jar, value);
			}
			return value;
		}
	}
	
	public class StarterClassLoader extends ClassLoader {
		private String jar, base;
		private BundleContext context;
		private LoaderBuilderFactory factory;

		public StarterClassLoader(String base, String jar, BundleContext context, ClassLoader parent, LoaderBuilderFactory f) {
			super(parent);
			this.jar = jar;
			this.context = context;
			this.base = base;
			this.factory = f;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
	
			Bundle search = factory.searchBundle(name);
			if(search != null) {
				try { 
					return search.loadClass(name);
				} finally {
				}
			}
			
			List<Bundle> bundleLst = Collections.emptyList();
			try {
				bundleLst = factory.newInstance().setBase(base).setLocation(jar).setUpdate(Update.MAYBE).startWrap(name);
				Class<?> result = bundleLst.get(0).loadClass(name);
				return result;
			} catch (Throwable e) {
				for(Bundle bundle: bundleLst)
					try {
						bundle.uninstall();
					} catch (BundleException e1) {
						log.log(LogService.LOG_ERROR, "loadClass " + name, e1);
					}
				log.log(LogService.LOG_ERROR, "loadClass " + name, e);
			} finally {
			}
			return super.loadClass(name, resolve);
		}
	}
	
	private Applet dwo;
	private Frame frame;
	private String codebase;
	private String documentbase;
	String repository;
	public static Configurator cm;
	private ServiceRegistration<ManagedService> ref1;
	private LogTracker log;
  private Dictionary provisioning;
	
	@Override
	public void start(BundleContext context) throws Exception {
		log = new LogTracker(context);
		log.open();
		try {
			reposAdmin = new ReposAdmin(context, log);
            reposAdmin.open();
			reposAdmin.setRepository(getProperty(context, "fi.dwo.jarindex"));
		} catch(Throwable t) {
		    log.log(LogService.LOG_DEBUG, "repository Admin", t);
		}; // expect errors as ClassNotFoundError
		
		cm = new Configurator(context);
		cm.open();
		
		ref = context.getServiceReference("fi.dwo.bootloader.LoaderBuilderFactory");
		if(ref != null)
		{ 
			factory = context.getService(ref);
			if(factory != null)
				Loader.instance = new StarterCreator(context);
			else 
				Loader.instance = new DummyStarterCreator(context);
		} else
			Loader.instance = new DummyStarterCreator(context);
		String profile = getProperty(context,"fi.dwo.profile");
		codebase = getProperty(context,"fi.dwo.codebase");
		documentbase = getProperty(context,"fi.dwo.documentbase");
		String language = getProperty(context,"fi.dwo.language");
		if(language == null) language = Locale.getDefault().getLanguage();
		if(codebase == null) codebase = "https://app.dwo.nl/dwo/";
		if(profile == null) profile = "1";
		
		parameters.put("language", language);
        parameters.put("profile", profile);
        if(provisioning != null) {
          parameters.put(PROVISIONING_UPDATE_COUNT, provisioning.get(PROVISIONING_UPDATE_COUNT).toString());
        }

        ManagedService manager = new ConfigHandler(context);
        Dictionary<String, Object> props = new Hashtable<String,Object>();
        props.put(Constants.SERVICE_PID, context.getBundle().getSymbolicName());
        ref1 = context.registerService(ManagedService.class, manager, props);
        
		//runDWO(context);
	}

  public String getProperty(BundleContext context, String key) {
    if (provisioning == null) {
      ServiceReference<?> ref = context.getServiceReference("org.osgi.service.provisioning.ProvisioningService");
      if (ref != null) {
        ProvisioningService service = (ProvisioningService) context.getService(ref);
        provisioning = service.getInformation();
        context.ungetService(ref);
      }
    }
    String value = null;
    if (provisioning != null) {
      value = (String) provisioning.get(key);
    }
    if (value == null)
      return context.getProperty(key);
    return value;
  }

	private void runDWO(BundleContext context) throws MalformedURLException {
				
		dwo = new DWO();
		ref1.unregister();
		dwo.addPropertyChangeListener("userName", cm);
		dwo.addPropertyChangeListener("passWord", cm);
		dwo.addPropertyChangeListener("language", cm);
		dwo.addPropertyChangeListener("profile",  cm);
		
        int width = GuiConstants.DWO_WIDTH;
        int height = GuiConstants.DWO_HEIGHT;
        if (dict != null) {
        	Object value = dict.get("fi.dwo.dwojapplet.width");
        	if(value instanceof Number)
        		width = ((Number) value).intValue();
        	value = dict.get("fi.dwo.dwojapplet.height");
        	if (value instanceof Number) {
        		height = ((Number) value).intValue();
        	}
        }
		final URL u = new URL(codebase);
		final URL d = documentbase == null ? null : new URL(documentbase);
		Applet parent = new ParentOf(dwo);
        frame = new MainFrame(parent,  width, height) {

			@Override
			public URL getCodeBase() {
				return u;
			}

			public URL getDocumentBase() {
				return d;
			}

			@Override
			public String getParameter(String name) {
				if (parameters.containsKey(name))
					return parameters.get(name);
				return super.getParameter(name);
			}

			@Override
			public void showDocument(URL url, String target) {
				showDocument(url);
			}

			@Override
			public void showDocument(URL url) {
				if(url == null)
					return;
				try {
					URI uri = url.toURI();
					Desktop.getDesktop().browse(uri);
				} catch (IOException | URISyntaxException e) {
					log.log(LogService.LOG_ERROR, "showDocument " + url, e);
				}
			}

			@Override
			public void showStatus(String status) {
				// TODO Auto-generated method stub
				super.showStatus(status);
			}

      private void registerForOSEvents() {
        ServiceReference<?> ref;
        ref = context.getServiceReference("fi.dwo.eawt.EAWT");
        if (ref != null) {
          EAWT eawt = (EAWT) context.getService(ref);
          eawt.setQuit(() -> { quit(); return true; });
          return;
        }
        super.registerForMacOSXEvents();
      }
      
      {
        registerForOSEvents();
      }

      @Override
      protected void registerForMacOSXEvents() {
      }

      @Override
			public void quit() {
				if(dwo != null) {
					Dimension size = dwo.getSize();
					int width = size.width;
					int height= size.height;
					Configuration c = cm.getConfiguration();
					Dictionary<String, Object> d = c.getProperties();
					if (d == null)
						d = new Hashtable<String,Object>();
					d.put("fi.dwo.dwojapplet.width", width);
					d.put("fi.dwo.dwojapplet.height", height);
					try {
						c.update(d);
					} catch (IOException e) {
					}
				}
				try {
					ServiceReference<EventAdmin> ref = context.getServiceReference(EventAdmin.class);
					Event event = new Event("fi/microserver/MicroServer/STOP", new HashMap());
					if (ref != null) 
					{
						EventAdmin admin = context.getService(ref);
						if(admin != null) {
							admin.postEvent(event);
							context.ungetService(ref);
						}
					} else {
// CHEAT
						String filter = "("+Constants.SERVICE_VENDOR+"=fi.microserver.MicroServer)";
						Collection<ServiceReference<EventHandler>> col = 
								context.getServiceReferences(EventHandler.class, filter);
						if(!col.isEmpty()) {
							EventHandler handler = context.getService(col.iterator().next());
							if(handler != null) {
								handler.handleEvent(event);
								context.ungetService(col.iterator().next());
							}
						}
						
					}
					context.getBundle().stop(); // stop framework
				} catch (Exception e) {
				}
			}
        	
        };
        frame.setTitle("Numworx author");
        frame.pack();
        Insets insets = frame.insets();
        frame.setSize(width+insets.left + insets.right, height + insets.bottom + insets.top);
        // Start applet.
        // that's it.
        frame.setVisible(true);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if(dwo != null) {
			dwo.stop();
			dwo.destroy();
			if(frame != null) frame.dispose();
			frame = null;
			dwo = null;
		}
		if(factory != null) {
			factory = null;
			context.ungetService(ref);
		}
		cm.close();		
		ref = null;
		if (reposAdmin != null) { reposAdmin.close(); reposAdmin = null; }
		log.close();
	}


}
