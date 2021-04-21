package fi.dwo.dwoloader_impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
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
	private Promise<List<Capability>> promise;
	ServiceRegistration<DwoLoader> sr;
	
	Dwo2LoaderImpl(BundleContext context, ServiceReference<Repository> srep) {
		this.context = context;
		this.rep = srep;

		Repository rep = context.getService(this.rep);
		RequirementExpression req = rep.newRequirementBuilder(OSGI_IDENTITY).addDirective("filter", "(type=osgi.bundle)").buildExpression();
		RequirementExpression req2 = rep.newRequirementBuilder(OSGI_IDENTITY).addDirective("filter", "(type=osgi.fragment)").buildExpression();
		req = rep.getExpressionCombiner().or(req, req2);
		
		org.osgi.util.function.Function<? super Collection<Resource>, ? extends List<Capability>> arg0;
		arg0 = new org.osgi.util.function.Function<Collection<Resource>, List<Capability>>() {

			@Override
			public List<Capability> apply(Collection<Resource> arg0) {
				return toCap(arg0);
			}
			
		};
		promise = rep.findProviders(req).map(arg0);
		rep = null;
		context.ungetService(this.rep);		
		sr = context.registerService(DwoLoader.class, this, null);
	
	}

	Capability toCap(Resource r) {
		List<Capability> caps = r.getCapabilities(OSGI_IDENTITY);
		return caps.stream().filter(new Predicate<Capability>() {
			@Override
			public boolean test(Capability cap) {
				return cap.getAttributes().containsKey("version");
			}
		}).findAny().get();
	}
	
	List<Capability> toCap(Collection<Resource> rr) {
		Function<Resource, Capability> function = new Function<Resource, Capability>() {
													@Override
													public Capability apply(Resource r) {
															return toCap(r);
													}
												};
		Stream<Resource> stream = rr.stream();
		Stream<Capability> map = stream.map(function);
		Collector<Capability, ?, List<Capability>> list = Collectors.toList();
		return map.collect(list);
	}
	
	
	
	@Override
	public Update getUpdate() {
		Update result = Update.NEVER;
		Bundle[] bs = context.getBundles();
		Map<String,Version> bmap = new HashMap<>();
		for( Bundle b: bs) {
			bmap.put(b.getSymbolicName(), b.getVersion());
		}
		try {
			for(Capability cap : promise.getValue()) {
				String sym = (String) cap.getAttributes().get(OSGI_IDENTITY);
				Version v   = (Version) cap.getAttributes().get("version");
				Version found = bmap.getOrDefault(sym, v);
				if ( ! found.equals(v) ) {
					result =  Update.MAYBE;
					break;
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
	public void close() throws Exception {
		sr.unregister();
	}

}
