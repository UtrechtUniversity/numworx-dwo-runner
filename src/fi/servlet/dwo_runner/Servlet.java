package fi.servlet.dwo_runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alfaariss.aselect.integration.ASelectException;

import fi.beans.fidentity.FidentityManager;
import fi.beans.fidentity.db.DbAccess;
import fi.beans.licman.LicMan;
import fi.dwo_runner.SecureDWORunner;

public class Servlet extends HttpServlet {
	
	Properties xConfiguration;
	private ASelectServletFilter secureFilter;
	
	protected void doGet(HttpServletRequest xRequest, HttpServletResponse xResponse) throws ServletException, IOException {
		String username="";
		try
        {
            if( secureFilter.authentication(xRequest,xResponse))
            {
                String message = "&nbsp;";
            	username  = secureFilter.getASelectUserId(xRequest);
            	username = URLDecoder.decode(username, "UTF-8");
            	if(username.endsWith("@fi.uu.nl"))
            		username = username.substring(0, username.length()-9);
            	String profileId = xRequest.getParameter("profile");
// empty  => null
            	if("".equals(profileId))
            		profileId = null;
            	int profile = -1;
            	if(profileId != null)
            		profile = Integer.parseInt(profileId);
            	String action    = xRequest.getParameter("action");
            	String password  = xRequest.getParameter("password");
// empty = null
            	if("".equals(password) || password == null)
            		password = null;
            	else if(!fm.checkAccount(username, password))
            	{
            		action = null;
            		message = "Incorrect password";
            	}
            	Map attributes = fm.getAccount(username);
            	Properties parameters = createParameters(profile, username, password, attributes);
            	
            	
            	if("download".equalsIgnoreCase(action))
            	{
                    xResponse.setContentType("application/java-archive");
                    xResponse.setHeader("Content-disposition","attachment; filename=" + "dwo_runner.jar");
                    OutputStream out = xResponse.getOutputStream();
                    createDWORunner(parameters, out);
                    out.close();
                    return;
            	}
            	
            	// display form....
            	
            	
                xResponse.setContentType("text/html");
                xResponse.setCharacterEncoding("UTF-8");
                PrintWriter xOut = xResponse.getWriter(); 
                
                String community = access.getSchoolByID(getCommunity(attributes)).get(access.SCHOOLNAME).toString();
				displayForm(xOut, profile, username, community, message);
                return;
            }
        }
        catch(Exception e)
        {
            try
            {
            	log(username, e);
                PrintWriter xOut = xResponse.getWriter(); 
                xResponse.setContentType("text/html");
                xOut.println("<html><body><br><i>Download DWO runner</i><br>");
                xOut.println("<br><br>U heeft geen toegang tot deze website!");
                if(e.getMessage()!= null)
                	xOut.println("<br>Error : " + e.getMessage());
                xOut.println("<p><hr>Copyright &copy; 2009 Freudenthal Instituut</body></html>");
                xOut.flush(); 
                return;
            }
            catch(Exception e1){}
        }
 	}

	void displayForm(PrintWriter out, int profile, String username, String community, String message) throws IOException {
		InputStream in = getClass().getResourceAsStream("resources/form.html");
		InputStreamReader  reader = new InputStreamReader(in, "UTF-8");
		StringBuffer inbuf = new StringBuffer(in.available());
		int c;
		while (  (c = reader.read()) != -1 ) inbuf.append((char)c);
		Object[] args = new Object[] { Integer.toString(profile), username, community, message } ;
		out.write(MessageFormat.format(inbuf.toString(), args));
	}

	Properties createParameters(int profile, String username, String password, Map attributes) {
		int community = getCommunity(attributes);
		
		Properties parameters = new Properties();
		if(username != null)
			parameters.put(SecureDWORunner.USERNAME, username);
		if(password != null)
			parameters.put(SecureDWORunner.PASSWORD, password);
		if(profile != -1);
			parameters.put(SecureDWORunner.PROFILE, Integer.toString(profile));
// TODO produktie...
		String license = LicMan.getLicense(community, profile, getClass());

		if(license!=null)
			parameters.put(LicMan.LICENSE_KEY, license);
		return parameters;
	}
	
	int getCommunity(Map attributes) {
		Iterator keys = attributes.keySet().iterator();
		while (keys.hasNext()) {
			String key = (String) keys.next();
// TODO dit moet veel beter!
			if(key.startsWith("DL_FIUUNL_K"))
				return Integer.parseInt(attributes.get(key).toString().substring(1));
			
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Initializeer servlet.
	 * <br>Parameters
	 * <dl>
	 * <dt>runnerSrc
	 * <dd>url van de locatie van dwo_runner.jar. Default is .../jars/dwo_runner.jar
	 * </dl>
	 * 
	 */
	public void init() throws ServletException {

		String runnerSrc = getInitParameter("runnerSrc");
		if(runnerSrc == null)
			runnerSrc = "http://www.fi.uu.nl/javaclasses/jars/dwo_runner.jar";
		try {
			setRunnerSrc(new URL(runnerSrc));
		} catch (MalformedURLException e1) {
// should not happen
			throw new ServletException(e1.getMessage(), e1);
		}
		
		fm = new FidentityManager();
		access  = new DbAccess();
		
		xConfiguration = new Properties();
        xConfiguration.put("aselect_use_logging","true");
            xConfiguration.put("aselect_log_file","/var/tmp/aselect_auth.log");
            xConfiguration.put("aselect_application_id","publiek"); // TODO Must be DWO_runner
            xConfiguration.put("aselect_agent_address","127.0.0.1");
            xConfiguration.put("aselect_agent_port","1495");
// TESTING FIXME
//          xConfiguration.put("aselect_application_url", "http://delta.fi.uu.nl:8080/servlet/fi.servlet.dwo_runner.Servlet" );
            
            secureFilter = new ASelectServletFilter();
			try {
				secureFilter.init(xConfiguration);
			} catch (ASelectException e) {
				throw new ServletException(e.toString(), e);
			}
	}

	private URL runnerSrc, htmlForm;
	private FidentityManager fm;
	private DbAccess access;
	
	public final String RUNNER_PROPERTIES = "fi/dwo_runner/resources/runner.properties";
	
	public void createDWORunner(Properties parameters, OutputStream out)
	throws IOException
	{
		InputStream in = runnerSrc.openStream();
		ZipInputStream jarin = new ZipInputStream(in);
		ZipOutputStream jarout = new ZipOutputStream(out);
		
		
		
// copy jarin to jarout:
		ZipEntry entry;
		byte[] buffer = new byte[1024];
		
		while( (entry = jarin.getNextEntry()) != null)
		{
			long size = entry.getSize();
			if(entry.getName().equals(RUNNER_PROPERTIES))
			{
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				CheckedOutputStream   cos = new CheckedOutputStream(bos, new CRC32());
				entry = new ZipEntry(entry);	// copy.
				while(size > 0) {
					int toread = (int) Math.min(size, buffer.length);
					int read = jarin.read(buffer, 0, toread);
					cos.write(buffer, 0, read);
					size -= read;
				}
				parameters.store(bos, null);
				cos.close();
				
				entry.setSize(bos.size());
				entry.setCrc(cos.getChecksum().getValue());
				entry.setCompressedSize(-1);
				jarout.putNextEntry(entry);
				bos.writeTo(jarout);
				
			} else { 
				jarout.putNextEntry(entry);
				while(size > 0) { 
					int toread = (int) Math.min(size, buffer.length);
					int read = jarin.read(buffer, 0, toread);
					jarout.write(buffer, 0, read);
					size -= read;
				}
			}
			jarin.closeEntry();
			jarout.closeEntry();			
		}
		jarin.close();
		jarout.finish();
	}

	URL getRunnerSrc() {
		return runnerSrc;
	}

	void setRunnerSrc(URL runnerSrc) {
		this.runnerSrc = runnerSrc;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	
	
	
}
