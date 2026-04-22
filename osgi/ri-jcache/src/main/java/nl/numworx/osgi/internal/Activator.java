package nl.numworx.osgi.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.cache.spi.CachingProvider;
import org.jsr107.ri.spi.RICachingProvider;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		CachingProvider provider = new RICachingProvider();
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put("name", "JSR107 Cache RI");
		dict.put("provider", "org.jsr107.ri.spi.RICachingProvider");
		context.registerService(CachingProvider.class, provider, dict );
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
