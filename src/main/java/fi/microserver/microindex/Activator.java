package fi.microserver.microindex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, FilenameFilter,
		ServiceTrackerCustomizer<ResourceIndexer, ResourceIndexer> {

	ServiceTracker<ResourceIndexer, ResourceIndexer> tracker;
	
	boolean waiting = true;
	static Activator activator;
	
	private String indir = "/opt/dwo/webapps/war-shared/jars";
	private String outfile = "/opt/dwo/webapps/war-shared/jars/index.xml";
	private String name;
	
	
	public Activator() {
		super();
		activator = this;
	}

	public void start(BundleContext context) throws Exception {
		
		ResourceAnalyzer analyser = new Analyser();
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		context.registerService(ResourceAnalyzer.class, analyser, properties);
		Class<ResourceIndexer> clazz = ResourceIndexer.class;
		tracker = new ServiceTracker<ResourceIndexer, ResourceIndexer>(context,
				clazz, this);
// configuration
		String f = context.getProperty("microindex.indir");
		if (f != null) indir = f;
		f = context.getProperty("microindex.outfile");
		if (f != null) outfile = f;
		name = context.getProperty(ResourceIndexer.REPOSITORY_NAME);
		if(name == null)
			name = ResourceIndexer.REPOSITORYNAME_DEFAULT;
		tracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		tracker.close();
	}

	public boolean accept(File dir, String name) {
		return name.endsWith(".jar");
	}

	public synchronized ResourceIndexer addingService(
			ServiceReference<ResourceIndexer> ref) {
		ResourceIndexer indexer = tracker.addingService(ref);
		try {
			File d = new File(indir);
			File[] list = d.listFiles(this);
			HashSet<File> set = new HashSet<File>(Arrays.asList(list));
			FileOutputStream out = new FileOutputStream(outfile);
			Map<String, String> config = new HashMap<String, String>();
			config.put(ResourceIndexer.PRETTY, "true");
			config.put(ResourceIndexer.VERBOSE, "true");
			// /config.put(ResourceIndexer.URL_TEMPLATE, "%f");
			config.put(ResourceIndexer.ROOT_URL, d.toURI().toString());
			config.put(ResourceIndexer.REPOSITORY_NAME, name);
			indexer.index(set, out, config);
			out.close();
		} catch (Exception oops) {
		}
		waiting = false;
		notifyAll();
		return null;
	}

	public void modifiedService(ServiceReference<ResourceIndexer> arg0,
			ResourceIndexer arg1) {
	}

	public void removedService(ServiceReference<ResourceIndexer> arg0,
			ResourceIndexer arg1) {

	}

}
