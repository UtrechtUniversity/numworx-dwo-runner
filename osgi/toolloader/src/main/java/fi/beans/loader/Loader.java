package fi.beans.loader;

import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import fi.dwo.bootloader.LoaderBuilderFactory;
import fi.dwo.bootloader.LoaderBuilder.Update;

public class Loader {
	
	public static LoaderBuilderFactory factory;
	
	private static String base = "https://cdn.dwo.nl/jars/";
	
	static class FactoryLoader extends ClassLoader {
		final String jar;

		FactoryLoader(String jar, ClassLoader parent) {
			super(parent);
			this.jar = jar;
		}
		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
	
			Bundle search = factory.searchBundle(name);
			if(search != null) {
				try { 
					return search.loadClass(name);
				} finally {
				}
			}
			
			List<Bundle> bundleLst = Collections.emptyList();
			try {
				bundleLst = factory.newInstance().setBase(base).setLocation(jar).setUpdate(Update.MAYBE).startWrap(name);
				Class<?> result = bundleLst.get(0).loadClass(name);
				return result;
			} catch (Throwable e) {
				for(Bundle bundle: bundleLst)
					try {
						bundle.uninstall();
					} catch (BundleException e1) {
						error("loadClass " + name, e1);
					}
				error("loadClass " + name, e);
			} finally {
			}
			return super.loadClass(name, resolve);
		}
		private void error(String string, Throwable e1) {
			System.err.println(string);
			e1.printStackTrace();			
		}
		
	}
	
	
	private Loader() {
	}

	public static void setPrefix(String prefix) {
		base = prefix;
	}
	
	public static ClassLoader create(String jar) {
		return create(jar, Loader.class.getClassLoader());
	}
	
	public static ClassLoader create(String jar, ClassLoader parent) {
		//return instance.create(jar, parent);
		return new FactoryLoader(jar, parent);
	}
	
}
