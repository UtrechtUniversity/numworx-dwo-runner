package nl.numworx.initdb;

import javax.cache.Cache;

import nl.uu.fi.dwo.lms.jclient.lib.rest.cache.PublicProfileCache;
import nl.uu.fi.dwo.rest.dom.entities.DomDwoProfileFull;

public class RedisCache {

	public static void main(String[] args) throws InterruptedException {
		final String PROVIDER = "org.redisson.jcache.JCachingProvider";
		final String SPI = "javax.cache.spi.CachingProvider";
		
		System.setProperty(SPI, PROVIDER);
		
		String redis = System.getProperty("REDIS");
		if (redis == null) {
			redis = System.getenv("REDIS");
			if (redis == null) redis = "redis:6379"; // the default in k8s
			System.setProperty("REDIS", redis);
		}
				
		Cache<String, DomDwoProfileFull> cache = null;
		do {
			try {
				cache = PublicProfileCache.cache();
			} catch(Exception oops) {
				oops.printStackTrace();
				Thread.sleep(10000L); // time for retry
			}
		} while (cache == null);
	}

}
