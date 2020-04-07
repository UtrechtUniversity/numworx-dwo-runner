package fi.dwo.bootloader.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

class ResolveContextImpl extends ResolveContext implements Repository {

	Repository repository[];
	BundleContext context;
	List<Resource> mandatory = new ArrayList<>();
	boolean filter;

	public void noFilter() {
	  filter = true;
	}
	
	@Override
	public Collection<Resource> getMandatoryResources() {
		return mandatory;
	}

	ResolveContextImpl(Repository[] repository, BundleContext context) {
		this.repository = repository;
		this.context = context;
		initWirings();
		framework = context.getBundle(0).adapt(FrameworkWiring.class);
	}

	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for(Requirement requirement: requirements)
		result.put(requirement, new ArrayList<Capability>(find(requirement)));
		return result;
	}

	public Collection<BundleCapability> find(Requirement requirement) {
		return framework.findProviders(requirement);
	}

	@Override
	public List<Capability> findProviders(Requirement requirement) {
		ArrayList<Capability> list = new ArrayList<Capability>();
		Set<Requirement> singleton = Collections.singleton(requirement);

		Collection<Capability> c = this.findProviders(singleton).get(requirement);
		// filter installed bundles
		Iterator<Capability> iter = c.iterator();
		while (iter.hasNext()) {
          Capability capability = iter.next();
          Resource r = capability.getResource();
          if (filter || wirings.containsKey(r)) list.add(capability);         
        }
		for(Repository r : repository)
			list.addAll(r.findProviders(singleton).get(requirement));
		return list;
	}

	@Override
	public int insertHostedCapability(List<Capability> capabilities,
			HostedCapability hostedCapability) {
		int sz = capabilities.size();
		capabilities.add(sz, hostedCapability);
		return sz;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		return true;
	}

	private Map<Resource, Wiring> wirings;
	private FrameworkWiring framework;

	@Override
	public Map<Resource, Wiring> getWirings() {
		return wirings; // deze uit BundleContext halen.
	}

	private void initWirings() {
		wirings = new HashMap<Resource, Wiring>();
		Bundle[] bundles = context.getBundles();
		for (Bundle b : bundles) {
		    int state = b.getState();
		    if (state <= Bundle.INSTALLED) continue;
			BundleWiring r = b.adapt(BundleWiring.class);
			BundleRevision res = b.adapt(BundleRevision.class);
			wirings.put(res, r);
		}
	}
}
