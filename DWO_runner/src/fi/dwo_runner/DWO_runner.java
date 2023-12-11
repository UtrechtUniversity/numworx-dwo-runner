package fi.dwo_runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;

import java.lang.reflect.InvocationTargetException;

    /**
     * Runs a jar application from any url. Usage is 'java JarRunner url [args..]'
     * where url is the url of the jar file and args is optional arguments to
     * be passed to the application's main method.
     */
    public class DWO_runner {

    	private static final String JAR = "jar";
    	private static final String MAIN_CLASS = "main-class";
    	private static final String TITLE = "title";
    	public static final String USERNAME = "username";
    	public static final String PROFILE  = "profile";
    	public static final String PASSWORD = "password";
    	public static final String PARAM = "param.";
    	private static final int WIDTH = 800;
    	private static final int HEIGHT= 600;
    	static private Properties parameters; 

    	
    	private static void addArg(Vector<String> v, String arg)
    	{
    		if(arg != null)
    			v.add(arg);
    	}
    	
    	/**
    	 * Start de DWO met parameters die in een property file staan.
    	 * Resources/runner.properties bevat:
    	 * <ul>
    	 * <li>jar
    	 * <li>profile (optioneel)
    	 * <li>param.<em>n</em> (start met 1)
    	 * <li>username (optioneel)
    	 * <li>password (optioneel)
    	 * <li>main-class (word niet gebruikt)
    	 * @param args not used
    	 * @throws Exception
    	 */
    	public static void main(String[] args) throws Exception {
            //if (args.length < 1) {
            //    usage();
            //}
    		InputStream in = DWO_runner.class.getResourceAsStream("resources/runner.properties");
    		parameters = new Properties();
    		parameters.load(in);
    		Vector<String> vargs = new Vector<String>();
    		vargs.add (parameters.getProperty(JAR));
    		for (int i = 1; true; i++)
    		{
    			String param = parameters.getProperty(PARAM + i);
    			if(param == null)
    				break;
    			addArg(vargs, param);
    		}
    		addArg(vargs, parameters.getProperty(PROFILE));
    		addArg(vargs, parameters.getProperty(USERNAME));
    		addArg(vargs, parameters.getProperty(PASSWORD));
    		args = new String[vargs.size()];
    		vargs.copyInto(args);

    		URL url = null;
            try {
                url = new URL(args[0]);
            } catch (MalformedURLException e) {
                fatal("Invalid URL: " + args[0]);
            }
            // Create the class loader for the application jar file
            @SuppressWarnings("resource")
			JarClassLoader cl = new JarClassLoader(url);
            // Get the application's main class name
            String name = null;
            try {
                name = cl.getMainClassName();
            } catch (UnknownHostException e) {
            	message("No internet, cannot find " + e.getMessage());
            	e.printStackTrace();
            	System.exit(1);
            } catch (ConnectException e) {
            	message("Cannot connect: " + e.getMessage());
            	e.printStackTrace();
            	System.exit(1);
            } catch (FileNotFoundException e) {
            	message("Unable to retrieve executable: " + e.getMessage());
            	e.printStackTrace();
            	System.exit(1);
            } catch (IOException e) {
                message("I/O error while loading JAR file:" + e.toString());
                e.printStackTrace();
                System.exit(1);
            }
            if (name == null) {
                fatal("Specified jar file does not contain a 'Main-Class'" +
                      " manifest attribute");
            }
            //Get arguments for the application
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            //Invoke application's main class
            
            //name = "fi.dwo.client.domain.DWO";
            try {
                cl.invokeClass(name, newArgs);
                
            } catch (ClassNotFoundException e) {
                fatal("Class not found: " + name);
            } catch (NoSuchMethodException e) {
                fatal("Class does not define a 'main' method: " + name);
            } catch (InvocationTargetException e) {
            	message(e.getTargetException().toString());
                e.getTargetException().printStackTrace();
                System.exit(1);
            }
        }

        private static void fatal(String s) {
        	message(s);
        	System.err.println(s);
            System.exit(1);
        }

        private static void message(Object m) {
        	try {
				JOptionPane.showMessageDialog(null, m, "Numworx Author", JOptionPane.ERROR_MESSAGE);
			} catch (Exception e) {
			}
        }
        
    }


