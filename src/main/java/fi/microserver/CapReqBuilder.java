package fi.microserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;


class CapReqBuilder {

  private static class RequirementImpl extends Impl implements Requirement {

    RequirementImpl(String namespace, HashMap<String, String> directives,
        HashMap<String, Object> attributes) {
      super(namespace, directives, attributes);
    }
    RequirementImpl(String namespace, HashMap<String, String> directives,
      HashMap<String, Object> attributes, Resource resource) {
    super(namespace, directives, attributes);
    this.resource = resource;
  }
    @Override
    public String toString() {
      return "Requirement[namespace=" + namespace + ", directives=" + directives + "]";
    }
   
  }
  private static class CapabilityImpl extends Impl implements Capability {

   CapabilityImpl(String namespace, HashMap<String, String> directives,
      HashMap<String, Object> attributes, Resource resource) {
    super(namespace, directives, attributes);
    this.resource = resource;
  }

  @Override
  public String toString() {
    return "Capability[namespace=" + namespace + ", attributes=" + attributes + "]";
  }

 
  
  }
 
  private static abstract class Impl {

    final String namespace;
    final Map<String,String> directives;
    final Map<String,Object> attributes;
    Resource resource;
 
    public Impl(String namespace, HashMap<String, String> directives,
        HashMap<String, Object> attributes) {
      this.namespace = namespace;
      this.attributes = Collections.unmodifiableMap(attributes);
      this.directives = Collections.unmodifiableMap(directives);
    }

    public String getNamespace() {
      return namespace;
    }

    public Map<String, String> getDirectives() {
      return directives;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public Resource getResource() {
      return resource;
    }

  }

  final String namespace;
  final HashMap<String,String> directives = new HashMap<>();
  final HashMap<String,Object> attributes = new HashMap<>();
  
  CapReqBuilder(String namespace) {
    this.namespace = namespace;
  }

  void addDirective(String key, String value) {
    directives.put(key, value);
  }

  public Requirement buildSyntheticRequirement() {
    return new RequirementImpl(namespace, directives, attributes);
  }
  public Requirement buildRequirement(Resource r) {
    return new RequirementImpl(namespace, directives, attributes,r);
  }
  public Capability buildCapability(Resource r) {
    return new CapabilityImpl(namespace, directives, attributes,r);
  }

  public void addAttribute(String key, Object value) {
    attributes.put(key, value);
  }

}
