package fi.microserver.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StatusServlet extends HttpServlet {

	String status = "no status";
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter pw = resp.getWriter();
		pw.println("<h1>Status</h1><img src='resources/info.png' >");
		pw.println(status);
	}

	@Override
	public void init() throws ServletException {
		log("init called");
		String param = getInitParameter("status");
		if(param != null) status = param;
	}

}
