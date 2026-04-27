package nl.numworx.toolloader;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import fi.beans.scorm.SAMLLoginIF;
import fi.dwo.bootloader.LoaderBuilder;
import fi.dwo.bootloader.LoaderBuilderFactory;
import fi.dwo.bootloader.LoaderBuilder.Update;

public class Activator extends fi.dwo.bootloader.impl.Activator implements BundleActivator {
	
	class Closer extends WindowAdapter {

		@Override
		public void windowClosed(WindowEvent e) {
			stopApplication();
		}

		@Override
		public void windowClosing(WindowEvent e) {
			stopApplication();
		}
		
		
	}

    private JFrame main;
    private BundleContext context;
    private Supplier<fi.beans.scorm.SAMLLoginIF> samlLogin;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		super.start(context);

        main = new JFrame("Teacher Tool");
        main.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        main.addWindowListener(new Closer());
        SettingsPanel content = new SettingsPanel(context, samlLogin, config);
        content.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
		main.setContentPane(content);
		
        main.pack();
        main.show();
    }

    @Override
	protected void boot(ServiceReference<?> serviceReference) throws BundleException, URISyntaxException {
		LoaderBuilder builder;
		LoaderBuilderFactory factory = (LoaderBuilderFactory) context
				.getService(serviceReference);
		builder = factory.newInstance();
		builder.setUpdate(Update.NEVER);
		installSLF4J(builder, factory);
		installConsole("true".equals(context.getProperty("fi.dwo.console")),
				builder);
		installCM(builder);
		installUnpack200(builder);
		
		String jarindex = (String) config.getProperty("fi.dwo.jarindex");
		XmlBackedRepositoryFactory admin = getService();
		try {
			admin.create(jarindex, null, this);
		} catch (Exception e) {
			throw new BundleException(e.getMessage(),e);
		}
		
		List<Bundle> bundles = builder
				.setLocation(URI.create(jarindex).resolve("samllogin.jar").toString())
				.startWrap("nl.numworx.samllogin.SamlLoginPanel");
		// what we want: samlLogin = tracker::getService;
		// what we have:
		samlLogin = () -> {
			try {
				Class<SAMLLoginIF> clazz = (Class<SAMLLoginIF>) bundles.get(0).loadClass("nl.numworx.samllogin.SamlLoginPanel");
				return clazz.newInstance();
			} catch (RuntimeException e) {
				e.printStackTrace();
				throw e;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException(e.getMessage(), e);
			} 
		};
		
		
	}

	private XmlBackedRepositoryFactory getService() {
		ServiceReference<XmlBackedRepositoryFactory> reference = context.getServiceReference(XmlBackedRepositoryFactory.class);
		return context.getService(reference);
	}

	public void stop(BundleContext context) throws Exception {
        main.dispose();
        main = null;
        super.stop(context);
    }

    private void stopApplication() {
		ServiceReference<EventAdmin> ref = context.getServiceReference(EventAdmin.class);
		if (ref != null) 
		{
			EventAdmin admin = context.getService(ref);
			if(admin != null) {
				Event event = new Event("fi/microserver/MicroServer/STOP", new HashMap());
				admin.postEvent(event);
				context.ungetService(ref);
				//return; // zolang er geen versie control is, alleen de harde manier.
			}
		}
		System.exit(0); // the hard way.

    }
    
    
}
