package fi.dwo.unpack200;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

public class Activator implements BundleActivator {

	static String CLAZZ = "java.util.jar.Pack200";
	public void start(BundleContext context) throws Exception {
		try {
			// missing since java 14
			// we run java 17
			getClass().getClassLoader().loadClass(CLAZZ);
		} catch(Throwable oops) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "missing " + CLAZZ, oops);
			return;
		}
		
		URLStreamHandlerService service = new Unpack200Handler(context);
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { Unpack200Handler.PROTOCOL });
		context.registerService(URLStreamHandlerService.class, service, properties);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
