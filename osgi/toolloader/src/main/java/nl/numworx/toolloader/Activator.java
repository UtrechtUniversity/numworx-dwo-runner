package nl.numworx.toolloader;

import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JRootPane;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import fi.beans.loader.Loader;
import fi.beans.mainframe.AppletContext;
import fi.beans.mainframe.AppletStub;
import fi.beans.mainframe.JApplet;
import fi.beans.scorm.SAMLLoginIF;
import fi.dwo.bootloader.LoaderBuilder;
import fi.dwo.bootloader.LoaderBuilderFactory;
import fi.dwo.bootloader.impl.Config;
import fi.dwo.bootloader.LoaderBuilder.Update;
import fi.dwo.eawt.EAWT;
import nl.uu.fi.dwo.lms.jclient.lib.rest.transport.StoredRestManager;

public class Activator extends fi.dwo.bootloader.impl.Activator implements BundleActivator, BooleanSupplier {
	
	public class Stub implements AppletStub, AppletContext {
		
		final Properties props;

		public Stub(Properties p) {
			props = p;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public URL getDocumentBase() {
			return getCodeBase();
		}

		@Override
		public URL getCodeBase() {
			try {
				return SettingsPanel.serverURI.toURL();
			} catch (MalformedURLException e) {
			}
			return null;
		}

		@Override
		public String getParameter(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void appletResize(int width, int height) {
			main.setSize(width, height);
		}

		@Override
		public Image getImage(URL url) {
			return null;
		}

		@Override
		public void showDocument(URL url) {

		}

		@Override
		public void showDocument(URL url, String target) {

		}

		@Override
		public void showStatus(String status) {
			

		}

		@Override
		public void setStream(String key, InputStream stream) throws IOException {
		}

		@Override
		public InputStream getStream(String key) {
			return null;
		}

		@Override
		public Iterator<String> getStreamKeys() {
			return null;
		}

		@Override
		public AppletContext getAppletContext() {
			return this;
		}

	}

	private class Delegate extends Properties {

		private Config setter;

		public Delegate(Properties p, Config config) {
			super(p);
			super.putAll(p);
			setter = config;
		}

		@Override
		public synchronized Object put(Object key, Object value) {
			if(setter != null) setter.setProperty(key.toString(), value);
			return super.put(key, value);
		}

		@Override
		public String getProperty(String key) {
			// TODO Auto-generated method stub
			String property = super.getProperty(key);
			if (property == null) {
				Object value = setter.getProperty(key);
				if (value instanceof String) {
					property = value.toString();
				}
			}
			return property;
		}

	}

	public class OkAction extends AbstractAction implements Action {

		private SettingsPanel settings;

		public OkAction(SettingsPanel settings, String name) {
			super(name);
			this.settings = settings;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Properties p = new Properties();
			p.setProperty("userName", settings.name.getText());
			p.setProperty("school", settings.schools.getSelectedItem().toString());
			p.setProperty("serverUrlPath", StoredRestManager.getInstance().getAuthenticator().getServerUrlPath().toExternalForm());
			p.setProperty("language", settings.language.getSelectedItem().toString());
			p.setProperty("profile", settings.profile.getText());
			config.setProperty("language", p.getProperty("language"));
			config.setProperty("profile", p.getProperty("profile"));
			config.setProperty("school", p.getProperty("school"));
// all relevant keys here!
			Object m = config.getProperty("studentmodelcontext");
			if (m != null) p.put("studentmodelcontext", m);
// root folder
			m = config.getProperty("rootfolder");
			if (m == null) {
				File dir = context.getDataFile("rootfolder");
				dir.mkdir();
				String rootfolder = dir.getAbsolutePath();
				p.put("rootfolder", rootfolder);
			}
			p = new Delegate(p, config);
			main.hide();
			JApplet applet = startTeacherTool(p);
			if (applet == null) stopApplication();
			AppletStub stub = new Stub(p);
			applet.setStub(stub);
			applet.init();
			JRootPane rootPane = applet.getRootPane();
			rootPane.invalidate();
			main.setRootPane(rootPane);
			main.pack();
			main.show();
			applet.start();
		}

	}

	class Closer extends WindowAdapter {

		@Override
		public void windowClosed(WindowEvent e) {
			stopApplication();
		}

		@Override
		public void windowClosing(WindowEvent e) {
			e.getWindow().removeWindowListener(this);
			stopApplication();
		}
		
		
	}

	class PrimaryFrame extends JFrame {

		PrimaryFrame(String title) throws HeadlessException {
			super(title);
		}

		@Override
		public void setRootPane(JRootPane root) {
			super.setRootPane(root);
		}
		
	}
	
    private PrimaryFrame main;
    private BundleContext context;
    private Supplier<fi.beans.scorm.SAMLLoginIF> samlLogin;
	private Class<? extends JApplet> teachertool;
	private String jarindex;
	private ServiceReference<EAWT> eawtref;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		super.start(context);
		if(config == null) return; // update in progress
		
        main = new PrimaryFrame("Teacher Tool");
        main.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        main.addWindowListener(new Closer());
        SettingsPanel content = new SettingsPanel(context, samlLogin, config);
        content.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
		main.setContentPane(content);
		
		
		content.okBtn.setAction(new OkAction(content, "OK"));
		content.cancelBtn.addActionListener(e -> System.exit(0));
		
        main.pack();
        main.show();
        
        eawtref = context.getServiceReference(EAWT.class);
        if (eawtref != null) {
        	EAWT eawt = context.getService(eawtref);
        	eawt.setQuit(this);
        }
    }

    @Override
	protected void boot(ServiceReference<?> serviceReference) throws BundleException, URISyntaxException {
		LoaderBuilder builder;
		LoaderBuilderFactory factory = (LoaderBuilderFactory) context
				.getService(serviceReference);
		Loader.factory = factory;
		builder = factory.newInstance();
		builder.setUpdate(Update.NEVER);
		installSLF4J(builder, factory);
		installConsole("true".equals(context.getProperty("fi.dwo.console")),
				builder);
		installJAXB(builder);
		installCache(builder);
		installCM(builder);
		
		jarindex = (String) config.getProperty("fi.dwo.jarindex");
		XmlBackedRepositoryFactory admin = getService();
		try {
			admin.create(jarindex, null, this);
		} catch (Exception e) {
			throw new BundleException(e.getMessage(),e);
		}
		Loader.setPrefix(jarindex);
		List<Bundle> bundles = builder
				.setBase(jarindex)
				.setLocation("samllogin.jar")
				.startWrap("nl.numworx.samllogin.SamlLoginPanel");
		// what we want: samlLogin = tracker::getService;
		// what we have:
		samlLogin = () -> {
			try {
				Class<SAMLLoginIF> clazz = (Class<SAMLLoginIF>) bundles.get(0).loadClass("nl.numworx.samllogin.SamlLoginPanel");
				return clazz.newInstance();
			} catch (RuntimeException e) {
				LOGt.error("loading samllogin", e);
				throw e;
			} catch (Exception e) {
				LOGt.error("loading samllogin", e);
				throw new RuntimeException(e.getMessage(), e);
			} 
		};
		installTeacherTool(builder);
		
	}

	JApplet startTeacherTool(Properties props) {
    	Constructor<? extends JApplet> constructor;
		try {
			constructor = teachertool.getConstructor(Properties.class);
	    	return constructor.newInstance(props);
		} catch (NoSuchMethodException
				|SecurityException
				|InstantiationException
				|IllegalAccessException
				|IllegalArgumentException
				|InvocationTargetException
				e) {
			LOGt.warning("startTeacherTool", e);
		}
		return null;
    }
    
    
	private void installTeacherTool(LoaderBuilder builder) {
		String base = "file:" + System.getProperty("user.home") + "/git/nolai-teachertool/teachertool/target/";
		String location = "teachertool.jar";
		try {
			String TOOL = "nl.numworx.teachertool.TeacherTool";
			List<Bundle> bundles = builder
					.setBase(jarindex)
					//.setBase(base)
					.setLocation(location)
					.setUpdate(Update.ALWAYS)
					.startWrap(TOOL);
			teachertool = (Class<? extends JApplet>) bundles.get(0).loadClass(TOOL);			
		} catch (BundleException
				|URISyntaxException
				|ClassNotFoundException
				e) {
			LOGt.error("install TeacherTool", e);
		}
		
	}

	private void installCache(LoaderBuilder builder) {
		try {
			builder.setLocation("ri-jcache-2.5.49.jar").start("nl.numworx.osgi.ri-jcache");
		} catch (Exception oops) {
			LOGt.warning("install Cache", oops); 
		}
	}
	
	
	private void installJAXB(LoaderBuilder builder) {
		try {
			builder.setLocation("jaxb-api-2.3.1.jar").start("jaxb-api");
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private XmlBackedRepositoryFactory getService() {
		ServiceReference<XmlBackedRepositoryFactory> reference = context.getServiceReference(XmlBackedRepositoryFactory.class);
		return context.getService(reference);
	}

	public void stop(BundleContext context) throws Exception {
		if (main!=null)
			main.dispose();
        main = null;
    	if (eawtref != null) {
    		EAWT s = context.getService(eawtref);
			if (s != null) s.setQuit(null);
    		context.ungetService(eawtref);
    		eawtref = null;
    	}
        super.stop(context);
    }

    void stopApplication() {
		ServiceReference<EventAdmin> ref = context.getServiceReference(EventAdmin.class);
		if (ref != null) 
		{
			EventAdmin admin = context.getService(ref);
			if(admin != null) {
				Event event = new Event("fi/microserver/MicroServer/STOP", new HashMap<String,Object>());
				admin.postEvent(event);
				context.ungetService(ref);
				return; // zolang er geen versie control is, alleen de harde manier.
			}
		}
		System.exit(0); // the hard way.

    }

	@Override
	public boolean getAsBoolean() {
		stopApplication();
		return false;
	}
    
    
}
