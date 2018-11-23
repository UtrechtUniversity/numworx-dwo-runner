package fi.microserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class ResourceBuilder {

  static class ResourceImpl implements Resource {

    private Map<String,List<Capability>> capabilities;
    private Map<String, List<Requirement>> requirements;;

    @Override
    public List<Capability> getCapabilities(String namespace) {
      return capabilities.getOrDefault(namespace, Collections.EMPTY_LIST);
    }

    @Override
    public List<Requirement> getRequirements(String namespace) {
      return requirements.getOrDefault(namespace, Collections.EMPTY_LIST);
    }

    private ResourceImpl(Map<String, List<CapReqBuilder>> cap,
        Map<String, List<CapReqBuilder>> req) {
      capabilities = new HashMap<>();
      requirements = new HashMap<>();
      cap.forEach((k,v) -> { 
        capabilities.put(k, v.stream().map(r -> r.buildCapability(this)).collect(Collectors.toList()));
      });
      req.forEach((k,v) -> {
        requirements.put(k, v.stream().map(r -> r.buildRequirement(this)).collect(Collectors.toList()));
      });
    }
    
  }
  
  private Map<String,List<CapReqBuilder>> capabilities = new HashMap<>();
  private Map<String, List<CapReqBuilder>> requirements = new HashMap<>();

  
  public void addCapability(CapReqBuilder capreq) {
    String namespace = capreq.namespace;
    List<CapReqBuilder> list = capabilities.get(namespace);
    if (list == null) { 
      list = new ArrayList<CapReqBuilder>();
      capabilities.put(namespace, list);
    }
    list.add(capreq);
  }

  public void addRequirement(CapReqBuilder capreq) {
    String namespace = capreq.namespace;
    List<CapReqBuilder> list = requirements.get(namespace);
    if (list == null) { 
      list = new ArrayList<CapReqBuilder>();
      requirements.put(namespace, list);
    }
    list.add(capreq);
  }

  public Resource build() {
    return new ResourceImpl(capabilities, requirements);
  }

}
