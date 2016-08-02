package fi.microserver.client;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import org.osgi.service.log.LogService;

public class Browser {

	LogService LOG; 
	
	URI base;
	
	public Browser(LogService log) {
		base = URI.create("http://localhost:8080/");
		LOG = log;
	}
	
	public void start(String page) {
		URI uri = base.resolve(page);
		try {
			Desktop.getDesktop().browse(uri);
		} catch (IOException e) {
			LOG.log(LogService.LOG_ERROR, "start " + uri, e);
		}
	}
}
