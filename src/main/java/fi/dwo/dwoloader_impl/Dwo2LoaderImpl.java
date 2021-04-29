package fi.dwo.dwoloader_impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.util.promise.Promise;

import fi.dwo.bootloader.LoaderBuilder.Update;
import fi.dwo.dwoloader.DwoLoader;

public class Dwo2LoaderImpl implements DwoLoader, AutoCloseable {

	private static final String OSGI_IDENTITY = "osgi.identity";
	ServiceReference<Repository> rep;
	BundleContext context;
	private Promise<Map<String, Version>> promise;
	volatile ServiceRegistration<DwoLoader> sr;
	volatile boolean closed;
	
	Dwo2LoaderImpl(BundleContext context, ServiceReference<Repository> srep) {
		this.context = context;
		this.rep = srep;
		this.closed = false;

		Repository rep = context.getService(this.rep);
		RequirementExpression req = rep.newRequirementBuilder(OSGI_IDENTITY).addDirective("filter", "(|(type=osgi.bundle)(type=osgi.fragment))").buildExpression();		
		promise = rep.findProviders(req).map(this::toCap);
		rep = null;
		promise.onResolve( () -> {
			context.ungetService(this.rep);
			register();
		});		
	
	}

	private synchronized void register() {
		if (!closed) sr = context.registerService(DwoLoader.class, this, null);
	}

	Capability toCap(Resource r) {
		List<Capability> caps = r.getCapabilities(OSGI_IDENTITY);
		return caps.get(0);
	}
	
	Map<String,Version> toCap(Collection<Resource> rr) {
		Stream<Resource> stream = rr.stream();
		Stream<Capability> map = stream.map(this::toCap);		
		Function<Capability, String> keyMapper = cap -> (String) cap.getAttributes().get(OSGI_IDENTITY);
		Function<Capability, Version> valueMapper = cap -> (Version) cap.getAttributes().get("version");
		Collector<Capability, ?, Map<String,Version>> list = Collectors.toMap(keyMapper, valueMapper);
		return map.collect(list);
	}
	
	
	
	@Override
	public Update getUpdate() {
		Update result = Update.NEVER;
		Bundle[] bs = context.getBundles();
		try {
			for( Bundle b: bs) {
				String key = b.getSymbolicName();
				if (promise.getValue().containsKey(key))
				{
					if (b.getVersion().equals(promise.getValue().get(key))) {
						result = Update.MAYBE;
						break;
					}
				}
			}
		} catch (InvocationTargetException e) {
			result = Update.MAYBE;
		} catch (InterruptedException e) {
			result = Update.MAYBE;
		}
		// result is Update.Never als alles wat geladen is dezelfde versie heeft.
		return result;
	}

	@Override
	public synchronized void close() throws Exception {
		closed = true;
		if (sr != null) sr.unregister();
		sr = null;
	}

}
