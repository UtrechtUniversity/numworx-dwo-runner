package fi.microserver;

import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
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

	private static final String DWOJAPPLET = "file:///Users/wim/Documents/workspace-luna/DWOJApplet/target/" + "DWOJApplet-2.0-SNAPSHOT-jar-with-dependencies.jar" ;
	private static final String CONSOLE = "file://" +"/Users/wim/Downloads/concierge-incubation-5.0.0/bundles/" + "org.eclipse.concierge.shell-5.0.0.20151029184259.jar";
	private static Framework framework;
	private static BundleContext context;
	private static LogService logger;
	private static Configurator cm;
	private static Bundle dwo;
	private static Bundle console;

	public static void main(String[] args) throws Exception {

		FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
		Map<String, String> map = new HashMap<String,String>();
		//map.put(Constants.FRAMEWORK_STORAGE_CLEAN,Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javax.swing,javax.swing.border,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.tree");
		map.put("fi.dwo.profile", "77");
		framework = factory.newFramework(map);
		framework.init();
		context = framework.getBundleContext();
		try {
			framework.start();
			installConsole();
			installServlet();
			installLogging();
			installConfigurator();
			installDWO();
			//installBrowser();
			framework.waitForStop(0);
		} catch (InterruptedException e) {
		} finally {
			System.exit(0);
		}	
	}

	private static void installDWO() {
		try {
			dwo = context.getBundle(DWOJAPPLET);
			if(dwo == null)
				dwo = context.installBundle(DWOJAPPLET);
			else 
			{
				long modified = dwo.getLastModified();
				System.out.println(new Date(modified));
				if(false) dwo.update();
			}
			dwo.start();
		} catch (Exception e) {
			logger.log(LogService.LOG_ERROR, "installing DWO", e);
		}
	}
	
	private static void installConsole() throws BundleException {
		console = context.getBundle(CONSOLE);
		if(console == null)
			console = context.installBundle(CONSOLE);
		console.start();
	}
	
	private static void installBrowser() {
		Browser browser = new Browser(logger);		
		browser.start("status");
	}

	private static void installLogging() {
		logger = new Logger();
		Dictionary<String, ?> dict = new Hashtable<String,String>();
		context.registerService(LogService.class, logger, dict);
	}

	private static void installConfigurator() {
		cm = new Configurator(context, logger);
		cm.open();
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
