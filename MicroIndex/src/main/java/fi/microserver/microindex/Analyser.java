package fi.microserver.microindex;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.Manifest;

import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.Builder;

public class Analyser implements ResourceAnalyzer {

	private static final String APPLET = "java.applet";

	public void analyzeResource(Resource resource,
			List<Capability> capabilities, List<Requirement> requirements)
			throws Exception {
		String pack = resource.getLocation() + ".pack.gz";
		File packedFile = new File(pack);
		if(packedFile.exists()) {
			int slash = pack.lastIndexOf('/');
			pack = pack.substring(slash+1);
			Builder builder = new Builder();
			builder.setNamespace("osgi.content");
			builder.addAttribute("url", pack);
			builder.addAttribute("size", packedFile.length());
			builder.addAttribute("mime", "application/x-java-pack200");
			capabilities.add(builder.buildCapability());
		}
		Manifest manifest = resource.getManifest();
		if(manifest == null) return;
		String main = manifest.getMainAttributes().getValue("Main-Class");
		if (main != null && main.length() > 0) {
			Builder builder = new Builder();
			builder.setNamespace(APPLET);
			builder.addAttribute(APPLET, main);
			Capability extra = builder.buildCapability();
			
			try { 
				main = main.replace('.', '/') + ".class";
				Resource r = resource.getChild(main);
				if(r != null)
					capabilities.add(extra);
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			
		}
//		String path = manifest.getMainAttributes().getValue("Class-Path");
//		if(path != null && path.length() > 0) {
//			String[] jars = path.split(" ");
//			StringBuilder string = new StringBuilder();
//			for (String item : jars) {
//				if(!item.endsWith(".jar")) 
//					continue;
//				string.append("(url=").append(item).append(")");
//			}
//			Builder	builder = new Builder();
//			builder.setNamespace(Namespaces.NS_CONTENT);
//			if(jars.length > 1) string.insert(0, "(&").append(")");
//				builder.addDirective(Namespaces.DIRECTIVE_FILTER, string.toString());
//			if(jars.length > 0)
//				requirements.add(builder.buildRequirement());
//		}
		
	}
}
