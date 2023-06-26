package fi.dwo.bootloader;

import org.osgi.framework.Bundle;

public interface LoaderBuilderFactory {
	LoaderBuilder newInstance();

	Bundle searchBundle(String name);
}
