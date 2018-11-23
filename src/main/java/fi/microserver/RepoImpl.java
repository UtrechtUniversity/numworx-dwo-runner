package fi.microserver;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

class RepoImpl implements Repository {

	String increment;
	String name;
	
	class ResourceImpl implements Resource, RepositoryContent {

		public InputStream getContent() {
			return null;
		}

		public List<Capability> getCapabilities(String namespace) {
			return null;
		}

		public List<Requirement> getRequirements(String namespace) {
			return null;
		}
		
	}
	
	
	private Collection<Resource> resources = new ArrayList<Resource>();
	
	private class ResourceReader extends DefaultHandler {

		private ResourceBuilder builder;
		private CapReqBuilder  capreq;
		
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if ("repository".equals(localName))
			{
				name = attributes.getValue("name");
				increment = attributes.getValue("increment");
			}
			else if ("resource".equals(localName))
				builder = new ResourceBuilder();
			else if ("capability".equals(localName)||"requirement".equals(localName)) {
				String namespace = attributes.getValue("namespace");
				capreq = new CapReqBuilder(namespace);
			}
			else if ("attribute".equals(localName))
			{
				String value = attributes.getValue("value");
				String type =  attributes.getValue("type");
				String name = attributes.getValue("name");
				if("url".equals(name)) {
					value = base.resolve(value).toString();
				}
				capreq.addAttribute(name, toType(value, type));
			}
			else if ("directive".equals(localName))
			{
				capreq.addDirective(attributes.getValue("name"), attributes.getValue("value"));
			}
		}

		private Object toType(String value, String type) {
			if ("Long".equals(type))
					return new Long(value.trim());
			if ("Version".equals(type))
					return new Version(value.trim());
			if ("Double".equals(type))
					return new Double(value.trim());
			return value; // wel of geen trim()?
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if("resource".equals(localName))
			{
				Resource r = builder.build();
				resources.add(r);
			}
			else if ("capability".equals(localName)) {
				builder.addCapability(capreq);
			} else if("requirement".equals(localName)) {
				builder.addRequirement(capreq);
			}
		}
		
	}
	
	URI base;
	
	public RepoImpl(InputSource repo) throws Exception {
		base = URI.create(repo.getSystemId());
		SAXParserFactory spf = SAXParserFactory.newInstance();
	    spf.setNamespaceAware(true);
	    SAXParser saxParser = spf.newSAXParser();
	    XMLReader xmlReader = saxParser.getXMLReader();
	    xmlReader.setContentHandler(new ResourceReader());
	    xmlReader.parse(repo);
	}

	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
		for(Requirement r: requirements) {
			try {
			    String string = r.getDirectives().get("filter");
			    Filter e = FrameworkUtil.createFilter(string);
				Collection<Capability> items = new ArrayList<Capability>();
				String namespace = r.getNamespace();
				for(Resource res : resources) {
					List<Capability> caps = res.getCapabilities(namespace);
					if(caps == null) 
					  continue;
					for (Capability capability : caps) {
						Map<String, Object> map = capability.getAttributes();
						if(e.matches(map))
							items.add(capability);
					}
				}	
				result.put(r,  items);
			} catch (InvalidSyntaxException e) {
				LogService log = null;
				if (log != null)
					log.log(LogService.LOG_ERROR, "findProviders for " + r, e);
				
			}
		}		
		return result;
	}

}
