package fi.microserver;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.felix.httplite.osgi.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
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

	private static Framework framework;
	private static LogService logger;
	private static Configurator cm;

	public static void main(String[] args) throws Exception {

		FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
		Map<String, String> map = new HashMap<String,String>();
		framework = factory.newFramework(map);
		
		framework.init();
				
//		Activator act = new Activator();
//		act.start(framework.getBundleContext());
		
		installServlet();
		installLogging();
		installConfigurator();
		try {
			framework.start();
			
			installBrowser();
			framework.waitForStop(0);
		} catch (InterruptedException e) {
		} finally {
			System.exit(0);
		}
		
	}

	private static void installBrowser() {
		Browser browser = new Browser(logger);
		
		browser.start("status");
	}

	private static void installLogging() {
		logger = new Logger();
		Dictionary<String, ?> dict = new Hashtable<String,String>();
		framework.getBundleContext().registerService(LogService.class, logger, dict);
	}

	private static void installConfigurator() {
		cm = new Configurator(framework.getBundleContext(), logger);
		cm.open();
	}
	
	
	private static void installServlet() {
		final BundleContext context = framework.getBundleContext();

		ServiceTracker<HttpService,HttpService> tracker;
		ServiceTrackerCustomizer<HttpService, HttpService> customizer = new ServiceTrackerCustomizer<HttpService, HttpService>() {

			final HttpServlet servlet = new StatusServlet();
			@SuppressWarnings("rawtypes")
			final Dictionary initparams = new Properties();

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
