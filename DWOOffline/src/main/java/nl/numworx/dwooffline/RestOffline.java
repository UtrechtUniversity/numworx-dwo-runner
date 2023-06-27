package nl.numworx.dwooffline;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestOffline extends HttpServlet {
	private static final String ERROR = "{\"msg\":\"offline\",\"Dwo2ExceptionCode\":\"Rest_CanNotReachServer\"}";

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setStatus(400);
		resp.getWriter().write(ERROR);
	}
	
	
}
