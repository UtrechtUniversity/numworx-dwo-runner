package fi.microserver;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.swing.JOptionPane;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fi.microserver.client.Browser;
import fi.microserver.client.Logger;
import fi.microserver.servlet.StatusServlet;

public class MicroServer {

	private static  String CODEBASE = "file://" + "/opt/dwo/webapps/war-shared/jars/";
	private static  String DWOJAPPLET = "file:///Users/wim/Documents/workspace-luna/DWOJApplet/target/" + "DWOJApplet-2.0-SNAPSHOT-jar-with-dependencies.jar" ;
	private static  String CONSOLE = "file://" +"/Users/wim/Downloads/concierge-incubation-5.0.0/bundles/" + "org.eclipse.concierge.shell-5.0.0.20151029184259.jar";
	private static final String PREVIEW = "file://" +"/Users/wim/Documents/workspace-luna/PreviewHTML/target/" + "previewhtml.jar";
	private static Framework framework;
	private static BundleContext context;
	private static LogService logger;
	private static Bundle dwo;
	private static Bundle console;

	
	
	
	public static void main(String[] args) throws Exception {
		
		CODEBASE = "https://app.dwo.nl/dwo/jars/";
		DWOJAPPLET = new File( "C:/Users/wim/workspace-luna/DWOJApplet/target/" + "DWOJApplet-2.0-SNAPSHOT-jar-with-dependencies.jar") .toURL().toString();
		CONSOLE = new File("C:\\Users\\wim\\Downloads\\").toURL() + "org.eclipse.concierge.shell-5.0.0.20151029184259.jar";
		
		
		FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
		Map<String, String> map = new HashMap<String,String>();
	    //map.put(Constants.FRAMEWORK_STORAGE_CLEAN,Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		//map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javax.swing,javax.swing.border,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.tree");
		map.put(Constants.FRAMEWORK_STORAGE, System.getProperty("user.home") + "/felix-cache");
		map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javafx.application,javafx.beans.property,javafx.beans.value,javafx.collections,javafx.concurrent,javafx.embed.swing,javafx.event,javafx.scene,javafx.scene.control,javafx.scene.web,javafx.util,javax.swing,javax.swing.border,netscape.javascript"
				+ ",com.apple.eawt"
			);
		map.put("fi.dwo.profile", "77");
		map.put("fi.dwo.language", "nl");
		map.put("fi.dwo.codebase", CODEBASE);
		framework = factory.newFramework(map);
		framework.init();
		context = framework.getBundleContext();
		FrameworkStartLevel frameLevel;
		frameLevel = framework.adapt(FrameworkStartLevel.class);
		try {
			installWrap();
			installConsole();
			framework.start();
			frameLevel.setInitialBundleStartLevel(10);
			//installServlet();
			installLogging();
			//installConfigurator();
			installDWO();
			installWiskOpdr();
			//installBrowser();
			frameLevel.setStartLevel(10);
			framework.waitForStop(0);
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

	private static void installWiskOpdr() {
		Set<String> work = new HashSet<String>();
		Set<String> done = new HashSet<String>();
		URL U = framework.getResource("fi/microserver/resources/" + "fi.wiskopdr.WiskOpdr");
		
		String bnd = "$Bundle-SymbolicName=fi.wiskopdr.WiskOpdr&DynamicImport-Package=*";
		if (U != null)
			bnd = "," + U;
		
		work.add("wiskopdr.jar");
		while( !work.isEmpty()) {
			Iterator<String> list = work.iterator();
			String jar = list.next();
			list.remove();
			if( done.add(jar)) {
				String wrap = "wrap:" + CODEBASE + jar + bnd;
				Bundle b;
				try {
					System.out.println(wrap);
					b = istart(wrap);
					Dictionary<String, String> headers = b.getHeaders();
					String classPath = headers.get("Class-Path");
					if (classPath != null) {
					String[] jars = classPath.split(" ");
					for (String item : jars) {
						work.add(item);
					}}
				}
				catch(Exception e) {}
				bnd = "$" + Constants.FRAGMENT_HOST + "=fi.wiskopdr.WiskOpdr";	
			}
		}
	}
	
	
	private static void installDWO() {
		try {
			String DWOJAPPLET_STARTER = new File("C:\\Users\\wim\\workspace-luna\\DWOJApplet-Starter\\target").toURL() + "/" + "DWOJApplet-Starter.jar";
			Bundle dwostarter = context.installBundle(DWOJAPPLET_STARTER);
			//istart(PREVIEW);
			dwo = context.getBundle(DWOJAPPLET);
			if(dwo == null)
			{
				dwo = context.installBundle(DWOJAPPLET);
			}
			else 
			{
				long modified = dwo.getLastModified();
				System.out.println(new Date(modified));
				if(false) dwo.update();
			}
			//dwo.start();
			DWOJAPPLET = new File("C:\\Users\\wim\\workspace-luna\\DWOJApplet-Starter\\target").toURL() + "/" + "DWOJApplet-Starter.jar";
			dwostarter.start();
		} catch (Exception e) {
			logger.log(LogService.LOG_ERROR, "installing DWO", e);
		}
	}

	private static Bundle istart(String location) throws BundleException {
		Bundle bundle = context.getBundle(location);
		if(bundle == null) 
			bundle = context.installBundle(location);
		if(bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null)
			bundle.start();
		return bundle;
	}
	
	private static void installConsole() throws BundleException {
		console = istart(CONSOLE);
	}
	
	private static void installBrowser() {
		Browser browser = new Browser(logger);		
		browser.start("status");
	}

	private static void installWrap() {
		BundleActivator activator;
		
		try {
			activator = new org.ops4j.pax.url.wrap.internal.Activator();
			activator.start(context);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void installLogging() {
		logger = new Logger();
		Dictionary<String, ?> dict = new Hashtable<String,String>();
		context.registerService(LogService.class, logger, dict);
	}

	private static void installConfigurator() {
		new Configurator(context, logger).open();
		new ConfiguratorSingleton(context, logger).open();
	}
		
	private static void installServlet() {

		ServiceTracker<HttpService,HttpService> tracker;
		ServiceTrackerCustomizer<HttpService, HttpService> customizer = new ServiceTrackerCustomizer<HttpService, HttpService>() {

			final HttpServlet servlet = new StatusServlet();
			@SuppressWarnings("rawtypes")
			final Dictionary initparams = new Properties();

			{ 
				initparams.put("status", "from installServlet");
			}

			public HttpService addingService(ServiceReference<HttpService> ref) {
				HttpService httpd = context.getService(ref);
				HttpContext defaultContext = httpd.createDefaultHttpContext();
				try {
					httpd.registerServlet("/status", servlet, initparams, defaultContext);
					httpd.registerResources("/resources", "/fi/microserver/resources", defaultContext);
				} catch (ServletException e) {
					e.printStackTrace();
				} catch (NamespaceException e) {
					e.printStackTrace();
				}
				return httpd;
			}

			public void modifiedService(ServiceReference<HttpService> ref,
					HttpService arg1) {				
			}

			public void removedService(ServiceReference<HttpService> arg0,
					HttpService httpd) {
				httpd.unregister("/status");
				httpd.unregister("/resources");
				context.ungetService(arg0);
			}};
		tracker = new ServiceTracker<HttpService, HttpService>(context, HttpService.class, customizer);
		tracker.open();
	}
}
