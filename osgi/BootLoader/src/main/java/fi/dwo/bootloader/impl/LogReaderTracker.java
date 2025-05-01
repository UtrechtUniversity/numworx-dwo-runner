package fi.dwo.bootloader.impl;

import java.io.PrintStream;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

class LogReaderTracker extends ServiceTracker<LogReaderService,LogReaderService> 
implements LogListener {

	private PrintStream writer;

	public LogReaderTracker(BundleContext context, PrintStream writer) {
		super(context, LogReaderService.class, null);
		this.writer = writer;
	}

	@Override
	public void logged(LogEntry entry) {
// Nog niet te regelen via logadmin.
		if (entry.getLoggerName().startsWith("Events."))
			return; // skip Events
		writer.println(entry);		
	}

	@Override
	public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
		LogReaderService service = super.addingService(reference);
		Enumeration<LogEntry> entries = service.getLog();
		while (entries.hasMoreElements()) {
			LogEntry logEntry = (LogEntry) entries.nextElement();
			logged(logEntry);
		}
		service.addLogListener(this);
		return service;
	}

	@Override
	public void removedService(ServiceReference<LogReaderService> reference, LogReaderService service) {
		service.removeLogListener(this);
		super.removedService(reference, service);
	}

}
