package fi.microserver;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import fi.microserver.JULHandler.LogReference;

public class JULHandler extends Handler  {

  static class LogReference implements ServiceReference {

    final LogRecord record;
    final Bundle bundle;
    LogReference(LogRecord record, Bundle bundle) {
      this.record = record;
      this.bundle = bundle;
    }

    final static String OBJECT_CLASS = "objectClass";
    final static String[] keys = { OBJECT_CLASS };
    @Override
    public Object getProperty(String key) {
      if ( OBJECT_CLASS.equals(key))
        return new String[] { record.getSourceClassName() };
      return null;
    }

    @Override
    public String[] getPropertyKeys() {
      return keys;
    }

    @Override
    public Bundle getBundle() {
      return bundle;
    }

    @Override
    public Bundle[] getUsingBundles() {
      return null;
    }

    @Override
    public boolean isAssignableTo(Bundle bundle, String className) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public int compareTo(Object reference) {
      // TODO Auto-generated method stub
      return 0;
    }

  }

  final private BundleContext context;
  private ServiceTracker<LogService, LogService> tracker;
  private SecurityManagerEx securityManager = new SecurityManagerEx();

  static void install(BundleContext context) throws SecurityException, IOException {
    // String properties =
    // "handlers= "
    // + JULHandler.class.getName()
    // + "\n" +
    // ".level= FINEST\n" +
    // "";
    // ByteArrayInputStream ins = new ByteArrayInputStream(properties.getBytes());
    // LogManager.getLogManager().readConfiguration(ins);
    Logger root = Logger.getLogger("");
    Handler[] old = root.getHandlers();
    if (old != null) for (Handler o : old)
      root.removeHandler(o);
    root.addHandler(new JULHandler(context));
    root.setLevel(Level.FINE);
    root.setFilter(JULHandler::classFilter);
  }

  static boolean classFilter(LogRecord record) {
    String src = record.getSourceClassName();
    return !(src.startsWith("java.awt")||src.startsWith("sun.")||src.startsWith("javax.swing"));
  }
  
  JULHandler(BundleContext context) {
    this.context = context;
    this.tracker = new ServiceTracker<LogService, LogService>(context, LogService.class, null);
    this.tracker.open();
    setFormatter(new SimpleFormatter());
    setFilter(JULHandler::classFilter);
  }

  @Override
  public void publish(LogRecord record) {
    if(!isLoggable(record))
      return;
    LogService service = tracker.getService();
    int level = record.getLevel().intValue();
    String message;
    message = getFormatter().formatMessage(record);
    if (service != null) {
      int lvl = LogService.LOG_DEBUG + 1;
      if(level>=Level.SEVERE.intValue())
        lvl = LogService.LOG_ERROR;
      else if (level >= Level.WARNING.intValue())
        lvl = LogService.LOG_WARNING;
      else if (level >= Level.INFO.intValue())
        lvl = LogService.LOG_INFO;
      else if (level >= Level.FINE.intValue())
        lvl = LogService.LOG_DEBUG;
      LogReference sr = new LogReference(record, getCallerBundle());
      service.log(sr, lvl, message, record.getThrown());
    } else {
      System.err.println(getFormatter().format(record));
    }
  }

  @Override
  public void flush() {}

  @Override
  public void close() throws SecurityException {
    tracker.close();
  }

  private Bundle getCallerBundle() {
    Bundle ret = null;
    Class[] classCtx = securityManager.getClassContext();
    for (int i = 0; i < classCtx.length; i++) {
        if (!classCtx[i].getName().startsWith("fi.microserver")
            && !classCtx[i].getName().startsWith("java.util.logging")) {
            ret = FrameworkUtil.getBundle(classCtx[i]);
            break;
        }
    }
    return ret;
}

static class SecurityManagerEx extends SecurityManager
{
    public Class[] getClassContext()
    {
        return super.getClassContext();
    }
}

}
