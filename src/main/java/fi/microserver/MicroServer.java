package fi.microserver;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.swing.JOptionPane;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class MicroServer {

	private static Framework framework;
	private static BundleContext context;
	private static ServiceRegistration<EventAdmin> registration;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		
		FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

		Map<String, String> map = new HashMap<String,String>();
	    //map.put(Constants.FRAMEWORK_STORAGE_CLEAN,Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		map.put(Constants.FRAMEWORK_STORAGE, System.getProperty("user.home") + "/felix-cache");
		map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javafx.application,javafx.beans.property,javafx.beans.value,javafx.collections,javafx.concurrent,javafx.embed.swing,javafx.event,javafx.scene,javafx.scene.control,javafx.scene.web,javafx.util,javax.swing,javax.swing.border,netscape.javascript"
				+ ",com.apple.eawt"
				+ ",org.osgi.service.cm;version=1.5, org.osgi.service.log;version=1.3"
				+ ",org.osgi.service.event;version=1.3.1"
			);
		Properties props = new Properties();
		InputStream in = MicroServer.class.getResourceAsStream("resources/DWO.properties");
		props.load(in);
		in.close();
		map.putAll((Map)props);
		if("true".equals(props.getProperty("fi.dwo.properties")))
			map.put("fi.dwo.documentbase", MicroServer.class.getResource("resources/").toExternalForm());
		else 
			map.remove("fi.dwo.properties");
		
		framework = factory.newFramework(map);
		framework.init();
		context = framework.getBundleContext();
		try {
			installWrap();
			installEvent();
			framework.start();
			installBoot();
			FrameworkEvent event = framework.waitForStop(0);
		} catch (InterruptedException e) {
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			JOptionPane.showMessageDialog(null, sw);			
		}		
		finally {
			System.exit(0);
		}	
	}

	private static final String STOP_EVENT = "fi/microserver/MicroServer/STOP";
	private static BundleActivator wrapActivator;
	
	private static void installEvent() {
		EventAdmin service;
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		properties.put(Constants.SERVICE_VENDOR, "fi.microserver.MicroServer");
		service = new EventAdmin() {

			public void postEvent(Event event) {
				System.out.println("Post " + event);
				if(STOP_EVENT.equals(event.getTopic()))
					stop();
			}

			public void sendEvent(Event event) {
				System.out.println("Send " + event);
				if(STOP_EVENT.equals(event.getTopic()))
					stop();
			} };
		registration = 
		context.registerService(EventAdmin.class, service, properties);
		
	}

	private static void installBoot() throws BundleException {
		String BOOT = context.getProperty("fi.dwo.boot");
		final Bundle bootloader = context.installBundle(BOOT);
		bootloader.start(Bundle.START_TRANSIENT);
	}

	private static void installWrap() throws Exception {
		wrapActivator = new org.ops4j.pax.url.wrap.internal.Activator();
		wrapActivator.start(context);
	}

	private static void stop() {
			new Thread() {
				public void run() {
					try {
						wrapActivator.stop(context);
						registration.unregister();			
						framework.stop();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}.start();
	}
	
}
