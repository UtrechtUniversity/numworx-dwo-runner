package fi.beans.loader;

import fi.dwo.dwojapplet.boot.Starter.StarterCreator;

public class Loader {

	public static StarterCreator instance;

	private Loader() {
	}

	public static void setPrefix(String prefix) {
		instance.setBase(prefix);
	}
	
	public static ClassLoader create(String jar) {
		return create(jar, Loader.class.getClassLoader());
	}
	
	public static ClassLoader create(String jar, ClassLoader parent) {
		return instance.create(jar, parent);
	}
	
	public static ClassLoader create(ClassLoader parent, String... jars) {
		return instance.create(jars[0], parent);
	}
}
