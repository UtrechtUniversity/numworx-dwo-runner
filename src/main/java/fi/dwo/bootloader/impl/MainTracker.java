package fi.dwo.bootloader.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;

class MainTracker extends BundleTracker<Collection<Bundle>> {

	/**
	 * 
	 */
	private String header;
	private Map<String, Collection<Bundle>> map = new Hashtable<String, Collection<Bundle>>();

	MainTracker(BundleContext context, String string) {
		super(context, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE, null);
		header = string;
	}

	@Override
	public Collection<Bundle> addingBundle(Bundle bundle, BundleEvent event) {
		String main = bundle.getHeaders().get(header);
		if (main == null)
			return null;
		// strip ;opties.
		int semi = main.indexOf(';');
		if(semi >= 0) {
			main = main.substring(0, semi);
		}
		main = main.trim();
		Collection<Bundle> value = map.get(main);
		if (value == null) {
			value = new HashSet<Bundle>();
			map.put(main, value);
		}
		value.add(bundle);
		return value;
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event,
			Collection<Bundle> object) {
		object.remove(bundle);
	}

	@Override
	public Collection<Bundle> getObject(Bundle bundle) {
		Collection<Bundle> object = super.getObject(bundle);
		if (object == null)
			return Collections.emptySet();
		return new ArrayList<Bundle>(object);
	}

	public Collection<Bundle> getObject(String key) {
		Collection<Bundle> object = map.get(key);
		if (object == null)
			return Collections.emptySet();
		return new ArrayList<Bundle>(object);
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event,
			Collection<Bundle> object) {
		super.modifiedBundle(bundle, event, object);
	}

}