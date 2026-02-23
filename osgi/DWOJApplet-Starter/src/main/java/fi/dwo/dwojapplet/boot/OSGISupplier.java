package fi.dwo.dwojapplet.boot;

import java.util.function.Supplier;

import javax.cache.spi.CachingProvider;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

class OSGISupplier extends ServiceTracker<CachingProvider, CachingProvider> implements Supplier<CachingProvider> {
	
	OSGISupplier(BundleContext context) {
		super(context, CachingProvider.class, null);
	}

	@Override
	public CachingProvider get() {
		return getService();
	}

}
