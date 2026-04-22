package nl.numworx.toolloader;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class Activator implements BundleActivator {
	
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

	public void start(BundleContext context) throws Exception {
		this.context = context;
        main = new JFrame("Teacher Tool");
        main.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        main.addWindowListener(new Closer());
        JLabel content = new JLabel("hier komt de teacher tool");
        content.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
		main.setContentPane(content);
		
        main.pack();
        main.show();
    }

    public void stop(BundleContext context) throws Exception {
        main.dispose();
        main = null;
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
