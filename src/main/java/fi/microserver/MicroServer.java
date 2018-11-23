package fi.microserver;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.repository.Repository;
import org.xml.sax.InputSource;

//import aQute.bnd.osgi.resource.CapReqBuilder;

public class MicroServer {

	private static Framework framework;
	private static BundleContext context;
	private static ServiceRegistration<EventHandler> registration;
	// Whether Windows/Mac
	static boolean isWindows = (System.getProperty("os.name").indexOf("Windows") >= 0);
	static boolean isMac = (System.getProperty("os.name").indexOf("Mac OS X") >= 0);
	private static ServiceRegistration<Repository> reposRegistration;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) throws Exception {
	    System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

		Preferences pref = Preferences.userRoot().node("fi/microserver");
		String uuid = pref.get("uuid", null);
		String clean = pref.get(Constants.FRAMEWORK_STORAGE_CLEAN, null);
		if(uuid == null) {
			uuid = UUID.randomUUID().toString();
			pref.put("uuid", uuid);
			try {
				pref.flush(); // Jammer als niet werkt, maar niet een showstopper
			} catch (Exception e) {
			}
		}
		String increment = pref.get("increment", "");
		List<String> arglist = Arrays.asList(args);
		FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
		Map<String, String> map = new HashMap<String,String>();
	    if(arglist.contains("-clean"))
	    	clean = Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;
	    map.put(Constants.FRAMEWORK_STORAGE_CLEAN,clean);
	    map.put("fi.dwo.console", Boolean.valueOf(arglist.contains("-console")).toString());
	    map.put("fi.dwo.uuid", uuid);
	    String dir = System.getProperty("user.home");
	    if(isWindows) dir += File.separator + "AppData" + File.separator + "Local";
	    else if(isMac) dir += File.separator + "Library" + File.separator + "Application Support";

		map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javafx.application,javafx.beans.property,javafx.beans.value,javafx.collections,javafx.concurrent,javafx.embed.swing,javafx.event,javafx.scene,javafx.scene.control,javafx.scene.web,javafx.util,javax.swing,javax.swing.border,netscape.javascript"
				+ ",com.apple.eawt"
				+ ",org.osgi.service.cm;version=1.5, org.osgi.service.log;version=1.3"
				+ ",org.osgi.service.event;version=1.3.1"
				+ ",org.osgi.service.repository;version=1.0"
				+ ",aQute.bnd.osgi.resource;version=1.4.0"
				+ ",aQute.bnd.osgi;version=2.3.0"
			);
		Properties props = new Properties();
		InputStream in = MicroServer.class.getResourceAsStream("resources/DWO.properties");
		props.load(in);
		in.close();
		map.putAll((Map)props);
		String target = props.getProperty("fi.dwo.target", "DWO-docent");
		map.put(Constants.FRAMEWORK_STORAGE, dir + File.separator + target + "-cache");
		
		if("true".equals(props.getProperty("fi.dwo.properties")))
			map.put("fi.dwo.documentbase", MicroServer.class.getResource("resources/").toExternalForm());
		else 
			map.remove("fi.dwo.properties");
		String u = props.getProperty("fi.dwo.repository");
		RepoImpl repos = null;
		if(u != null) {
			try {
				InputSource input = new InputSource(u);
				repos = new RepoImpl(input);
				if(!increment.equals(repos.increment)) {
					increment = repos.increment;
				} else {
					if(null == props.getProperty("fi.dwo.boot"))
						map.put("fi.dwo.update", "NEVER");
				}
			} catch (Exception e) {
				displayException(e);
				System.exit(1);
			}
		}
		cleanOnExit(true);
		framework = factory.newFramework(map);
		framework.init();
		context = framework.getBundleContext();

		try {
			//installWrap();
		    JULHandler.install(context);
			installEvent();
			installRepository(repos);
			installBoot();
			int type;
			do { 
				framework.start();
				try {
					bootloader.start(Bundle.START_TRANSIENT);
				} catch (BundleException e) {
					bootloader.uninstall();
					throw e;
				}
				FrameworkEvent event = framework.waitForStop(0);
				type  = event.getType();
			} while (type != FrameworkEvent.STOPPED);
			pref.put("increment", increment);
			pref.flush();
			cleanOnExit(false);
			java.util.logging.Logger.getLogger("").info("DWO exit");
		} catch (InterruptedException e) {
		} catch (Throwable t) {
			displayException(t);			
		}		
		finally {
			System.exit(0);
		}	
	}

	private static void installRepository(RepoImpl repos2) throws Exception {
		String u = context.getProperty("fi.dwo.repository");
		if(u == null) return;
		InputSource input = new InputSource(u);
		RepoImpl repos = repos2;
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(Repository.URL, u);
		properties.put(Constants.SERVICE_PID, "fi.dwo.repository");
		properties.put("repository.name", repos.name);
		properties.put("repository.increment", Long.valueOf(repos.increment));
		reposRegistration = context.registerService(Repository.class, repos, properties);	
	}

	private static void displayException(final Throwable t) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static final String STOP_EVENT = "fi/microserver/MicroServer/STOP";
	//private static BundleActivator wrapActivator;
	private static Bundle bootloader;
	
	private static void installEvent() {
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

	static final String BOOTLOADER  = "fi.dwo.BootLoader";
	private static void installBoot() throws BundleException {
		String BOOT = context.getProperty("fi.dwo.boot");
		CapReqBuilder builder = new CapReqBuilder("osgi.identity");
		builder.addDirective("filter", "(osgi.identity="+ BOOTLOADER +")");
		Requirement r = builder.buildSyntheticRequirement();

		try {
			if (reposRegistration != null && BOOT == null) {
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
			} else if (BOOT == null) {
				BOOT = context.getProperty("fi.dwo.boot0");
			}
 			
			bootloader = context.installBundle(BOOT);
		} catch (BundleException e) {
			int type = e.getType();
			if (type == BundleException.DUPLICATE_BUNDLE_ERROR) { // uninstall asap
				FrameworkWiring fw = context.getBundle(0).adapt(FrameworkWiring.class);
				Collection<BundleCapability> res = fw.findProviders(r);
				for (BundleCapability bc:res) {
					bc.getRevision().getBundle().uninstall();
					installBoot(); return; // recurse
				}
			}
			throw e;
		}
	}

//	private static void installWrap() throws Exception {
//		wrapActivator = new org.ops4j.pax.url.wrap.internal.Activator();
//		wrapActivator.start(context);
//	}

	private static void stop() {
			SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					try {
						//wrapActivator.stop(context);
						registration.unregister();
						if(reposRegistration != null) // optional.
							reposRegistration.unregister();
						framework.stop();
					} catch (Exception e) {
						displayException(e);
					}
				}
			});
	}
	
	private static void cleanOnExit(boolean on) {
		try {
			Preferences pref = Preferences.userRoot().node("fi/microserver");
			if(on)
				pref.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			else
				pref.remove(Constants.FRAMEWORK_STORAGE_CLEAN);
			pref.flush();
		} catch (BackingStoreException e) {
		}
		
	}
	
}
