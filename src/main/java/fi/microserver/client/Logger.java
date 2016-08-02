package fi.microserver.client;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class Logger implements LogService {

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);

	}

	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		System.err.println(message);
		if(exception != null) exception.printStackTrace();
	}

}
