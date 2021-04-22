package fi.dwo.bootloader.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class FactoryFactory implements ServiceFactory<Factory> {

	private final BundleContext context;
	
	FactoryFactory(BundleContext bc) {
		context = bc;
	}
	
	public Factory getService(Bundle bundle,
			ServiceRegistration<Factory> registration) {
		String base = (String) registration.getReference().getProperty(
				"fi.dwo.bundles");
		return new Factory(context,bundle, base);
	}

	public void ungetService(Bundle bundle,
			ServiceRegistration<Factory> registration, Factory service) {
		service.close();
	}

}
