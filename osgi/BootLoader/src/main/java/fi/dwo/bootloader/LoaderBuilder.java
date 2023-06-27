package fi.dwo.bootloader;

import java.net.URISyntaxException;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public interface LoaderBuilder {
	enum Update {
		NEVER, MAYBE, ALWAYS
	};

	LoaderBuilder setLocation(String location) throws URISyntaxException;

	LoaderBuilder setBase(String base) throws URISyntaxException;

	LoaderBuilder setUpdate(Update strategy);

	void start(String name) throws BundleException;

	void stop(String name) throws BundleException;

	List<Bundle> startWrap(String main) throws BundleException;

	/**
	 * @since 2.1
	 * @return update
	 */
	Update getUpdate();

}
