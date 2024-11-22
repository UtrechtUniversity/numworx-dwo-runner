package fi.dwo.dwojapplet.boot;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import fi.beans.mainframe.MainFrame;
import fi.beans.numworxlf.JLabel;
import fi.dwo.dwojapplet.BUILD;
import fi.dwo.eawt.EAWT;

public class EAWTTracker extends ServiceTracker<EAWT, EAWT> implements Supplier<JDialog> {
	
	final String version;
	final String title;
	final MainFrame frame;
	final BooleanSupplier quit;

	public EAWTTracker(BundleContext context, MainFrame mainFrame, BooleanSupplier quit) {
		super(context, "fi.dwo.eawt.EAWT", null);
		version = context.getProperty("fi.microserver.version");
		title = "Numworx Author";
		frame = mainFrame;
		this.quit = quit;
	}

	@Override
	public EAWT addingService(ServiceReference<EAWT> reference) {
		EAWT service = super.addingService(reference);
		service.setAbout(this);
		service.setQuit(quit);
		return service;
	}

	@Override
	public JDialog get() {
		JDialog dialog = new JDialog(frame);
		JLabel title = new JLabel(this.title);
		title.setFont(title.getFont().deriveFont(18.0f));
		JLabel runner = new JLabel("Runner Version: " + version);
		JLabel dwo    = new JLabel("Author Version: " + BUILD.version);
		Box box = Box.createVerticalBox();
		box.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		dialog.setContentPane(box);
		box.add(title);
		box.add(Box.createVerticalStrut(10));
		box.add(runner);
		box.add(dwo);
		dialog.setModal(true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(frame);
		return dialog;
	}

}
