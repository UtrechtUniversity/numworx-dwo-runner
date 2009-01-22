/**
 * 
 */
package fi.dwo_runner;

import fi.beans.licman.*;
import java.applet.Applet;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.swing.JOptionPane;

import fi.beans.mainframe.MainFrame;

/**
 * @author wim
 *
 */
public class SecureDWORunner extends MainFrame implements LicensedApplication {

	private static final String JAR = "jar";
	private static final String MAIN_CLASS = "main-class";
	public static final String USERNAME = "username";
	public static final String PROFILE  = "profile";
	public static final String PASSWORD = "password";
	private static final int WIDTH = 800;
	private static final int HEIGHT= 600;
	private static final String LICENSE = LicMan.LICENSE_KEY;
	static private Properties parameters; 
	
	
	/**
	 * @param applet
	 * @param width
	 * @param height
	 */
	public SecureDWORunner(Applet applet, int width, int height) {
		super(applet, width, height);
		
	}

	public URL getCodeBase() {
		String codebase = "jar:" + parameters.getProperty(JAR) + "!/";
		try {
			return new URL(codebase);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public String getParameter(String name) {
		return parameters.getProperty(name);
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		InputStream in = SecureDWORunner.class.getResourceAsStream("resources/runner.properties");
		parameters = new Properties();
		parameters.load(in);
		
		
		URL url = new URL(parameters.getProperty(JAR));
		JarClassLoader cl = new JarClassLoader(url);
		Class dwoClass = cl.loadClass(parameters.getProperty(MAIN_CLASS));
		Constructor constructor = dwoClass.getConstructor(new Class[] { args.getClass() });
		args = new String[] { 
				parameters.getProperty(PROFILE),
				parameters.getProperty(USERNAME),
				parameters.getProperty(PASSWORD)
		};
		Applet dwo = (Applet) constructor.newInstance(new Object[] { args } );
		MainFrame frame = new SecureDWORunner(dwo, WIDTH, HEIGHT);
// License....
		try {
			LicMan.checkLicense((LicensedApplication)frame);
		} catch (LicenseException e) {
			JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
// copy from DWO class
        frame.setTitle("DWO");
        frame.pack();
        frame.setSize(WIDTH + 10, HEIGHT + 20);
        frame.show();
		
	}

}
