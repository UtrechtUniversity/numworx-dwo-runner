package fi.servlet.dwo_runner;

import java.io.FileOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.beans.licman.LicMan;
import fi.dwo_runner.SecureDWORunner;

import junit.framework.TestCase;

public class ServletTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testDoGetHttpServletRequestHttpServletResponse() throws Exception {
		Servlet s = new Servlet();
// TODO
		s.init();
// TODO.....
		HttpServletResponse response= null;
		HttpServletRequest  request = null;
		
		s.doGet(request, response);
		
		
	}
	
	public void testcreaterunner() throws Exception {
		Servlet s = new Servlet();
		FileOutputStream fos = new FileOutputStream("/tmp/klad.jar");
		s.setRunnerSrc(new URL("file:/home/wim/workspace/DWO_runner/output/jar/dwo_runner.jar"));
		Properties p = new Properties();
		p.put(SecureDWORunner.USERNAME, "peterb");
		p.put(SecureDWORunner.PASSWORD, "passw");
		p.put(SecureDWORunner.PROFILE, "3");
		p.put(LicMan.LICENSE_KEY, "TEST_LICENSE");
		s.createDWORunner(p, fos);
		fos.close();
	}
	
	public void testCreateParameters() throws Exception { 
		Servlet s = new Servlet();
		HashMap attributes = new HashMap();
		attributes.put("DL_FIUUNL_K1K", "D136");
		Properties p = s.createParameters("1", "amadeus", "mozart", attributes);
		p.list(System.out);
		p = s.createParameters("1", "x", null, attributes);
		
		try { 
			p = s.createParameters("1", null, null, new HashMap());
			fail("no license!");
		} catch( IllegalArgumentException e)
		{
			System.out.println(e);
		}
	}
	

}
