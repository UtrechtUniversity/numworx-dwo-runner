package fi.dwo.bootloader.impl;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fi.dwo.bootloader.LoaderBuilder;
import fi.dwo.bootloader.LoaderBuilderFactory;

public class Factory implements LoaderBuilderFactory, ServiceTrackerCustomizer<Repository, Repository> {

	private BundleContext context;
	Map<String, Bundle> bundles;
	private String base;
	MainTracker mainTracker, fragmentTracker, symbolicTracker;

	ServiceTracker<Resolver, Resolver> resolver;
	ServiceTracker<Repository, Repository> repository;
	
	public Factory(BundleContext context, String base) {
		this.context = context;
		this.base = base;
		bundles = new Hashtable<String, Bundle>();
		mainTracker = new MainTracker(context, "Main-Class");
		fragmentTracker = new MainTracker(context, Constants.FRAGMENT_HOST);
		symbolicTracker = new MainTracker(context,
				Constants.BUNDLE_SYMBOLICNAME);
		mainTracker.open();
		fragmentTracker.open();
		symbolicTracker.open();
		
		resolver = new ServiceTracker<Resolver, Resolver>(context, Resolver.class, null);
		repository = new ServiceTracker<Repository, Repository>(context, Repository.class, this);
		resolver.open();
		repository.open();
	}

	void close() {
		repository.close();
		resolver.close();
		mainTracker.close();
		fragmentTracker.close();
		symbolicTracker.close();
	}

	void addBundle(Bundle b) {
		Bundle old;
		String main = b.getHeaders().get("Main-Class");
		if (main != null) {
			old = bundles.put(main, b);
		}
	}

	public LoaderBuilder newInstance() {
		return new Builder(this).setContext(context).setBase(base);
	}

	public Bundle searchBundle(String name) {
		Bundle b = bundles.get(name);
		if (b == null)
			return b;
		if (b.getState() == Bundle.UNINSTALLED) {
			bundles.remove(name, b);
			return null;
		}
		String header = b.getHeaders().get(Constants.FRAGMENT_HOST);
		if (header == null)
			return b;
		return searchBundle0(header);
	}

	Bundle searchBundle0(String name) {
		Collection<Bundle> bundles = symbolicTracker.getObject(name);
		if (bundles.isEmpty())
			return null;
		return bundles.iterator().next();
	}

	@Override
	public Repository addingService(ServiceReference<Repository> reference) {
		Object repo = context.getService(reference);
		if (repo instanceof Repository) return (Repository) repo;
		context.ungetService(reference);
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<Repository> reference, Repository service) {
	}

	@Override
	public void removedService(ServiceReference<Repository> reference, Repository service) {
		context.ungetService(reference);
		
	}

}
