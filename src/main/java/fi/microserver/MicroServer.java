package fi.microserver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import javax.swing.JOptionPane;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class MicroServer {

	private static  String CODEBASE = "file://" + "/opt/dwo/webapps/war-shared/jars/";
	private static  String BOOT = "file://" + "/Users/wim/Documents/workspace-luna/BootLoader/target/" + "BootLoader-0.0.1-SNAPSHOT.jar";
	private static Framework framework;
	private static BundleContext context;
	
	public static void main(String[] args) throws Exception {
		
		CODEBASE = "https://app.dwo.nl/dwo/jars/";
		
		FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
		Map<String, String> map = new HashMap<String,String>();
	    //map.put(Constants.FRAMEWORK_STORAGE_CLEAN,Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		//map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javax.swing,javax.swing.border,javax.swing.event,javax.swing.filechooser,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.table,javax.swing.text,javax.swing.text.html,javax.swing.tree");
		map.put(Constants.FRAMEWORK_STORAGE, System.getProperty("user.home") + "/felix-cache");
		map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "javafx.application,javafx.beans.property,javafx.beans.value,javafx.collections,javafx.concurrent,javafx.embed.swing,javafx.event,javafx.scene,javafx.scene.control,javafx.scene.web,javafx.util,javax.swing,javax.swing.border,netscape.javascript"
				+ ",com.apple.eawt"
				+ ",org.osgi.service.cm;version=1.5, org.osgi.service.log;version=1.3"

			);
		map.put("fi.dwo.profile", "1");
		map.put("fi.dwo.language", "nl");
		map.put("fi.dwo.codebase", CODEBASE);

		framework = factory.newFramework(map);
		framework.init();
		context = framework.getBundleContext();
		try {
			installWrap();
			installBoot();
			framework.start();
			framework.waitForStop(0);
		} catch (InterruptedException e) {
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			JOptionPane.showMessageDialog(null, sw);			
		}		
		finally {
			System.exit(0);
		}	
	}

	private static void installBoot() throws BundleException {
		Bundle bootloader = context.installBundle(BOOT);
		bootloader.start();
	}

	private static void installWrap() {
		BundleActivator activator;
		
		try {
			activator = new org.ops4j.pax.url.wrap.internal.Activator();
			activator.start(context);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
