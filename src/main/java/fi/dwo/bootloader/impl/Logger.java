package fi.dwo.bootloader.impl;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class Logger implements LogService {

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		switch(level) {
		case LogService.LOG_DEBUG: System.err.print("[DEBUG] ");break;
		case LogService.LOG_ERROR: System.err.print("[ERROR] ");break;
		case LogService.LOG_INFO:  System.err.print("[INFO] ");break;
		case LogService.LOG_WARNING: System.err.print("[WARNING] "); break;
		}
		if(sr != null) System.err.print(sr + " ");
		System.err.println(message);
		if (exception != null)
			exception.printStackTrace();
	}

}
