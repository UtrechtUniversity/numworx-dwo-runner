package fi.microserver.microindex;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.felix.connect.launch.ClasspathScanner;
import org.apache.felix.connect.launch.PojoServiceRegistry;
import org.apache.felix.connect.launch.PojoServiceRegistryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.indexer.ResourceIndexer;

public class Main {

	public static void main(String[] args) throws Exception {
		PojoServiceRegistryFactory factory = ServiceLoader.load(PojoServiceRegistryFactory.class).iterator().next();
		Map<String, Object> map = new HashMap<String,Object>();
		map.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, new ClasspathScanner().scanForBundles());
		String indir = ".";
		if(args.length>0) indir = args[0];
		File infile = new File(indir);
		if(!infile.isDirectory())
		{
			System.err.println(indir + " is not a directory");
			System.err.println("Usage: microindex [dir] [index.xml]");
			System.exit(1);
		}
		String outfile = new File(indir, "index.xml").toString();
		if(args.length>1) outfile = args[1];
		map.put("microindex.indir", indir);
		map.put("microindex.outfile", outfile);
		if(args.length>2)
			map.put(ResourceIndexer.REPOSITORY_NAME, args[2]);
		if(args.length>3)
			map.put("microindex.includes", args[3]);
		PojoServiceRegistry framework = factory.newPojoServiceRegistry(map);
		Activator activator = Activator.activator;
		synchronized(activator) {
			while(activator.waiting)
			{
				activator.wait();
			}
		}
		framework.getBundleContext().getBundle().stop();
	}

}
