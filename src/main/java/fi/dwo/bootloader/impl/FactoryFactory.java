package fi.dwo.bootloader.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class FactoryFactory implements ServiceFactory<Factory> {

	public Factory getService(Bundle bundle,
			ServiceRegistration<Factory> registration) {
		String base = (String) registration.getReference().getProperty(
				"fi.dwo.bundles");
		return new Factory(bundle.getBundleContext(), base);
	}

	public void ungetService(Bundle bundle,
			ServiceRegistration<Factory> registration, Factory service) {
		service.close();
	}

}
