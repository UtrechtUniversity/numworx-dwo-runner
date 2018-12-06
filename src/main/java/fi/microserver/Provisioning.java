package fi.microserver;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import org.osgi.framework.*;
import org.osgi.service.provisioning.*;

public class Provisioning implements ProvisioningService {
  static String DWOv1 = "#DWOv1";
  
  @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
  static ServiceRegistration<ProvisioningService> install(BundleContext context) {
      String uri = context.getProperty(PROVISIONING_REFERENCE);
      if (uri != null) {
        try {
          URL url = new URL(uri);
          URLConnection uc = url.openConnection();
          BufferedInputStream in = new BufferedInputStream(uc.getInputStream());
          int mod = (int)(uc.getLastModified()/1000L);
          in.mark(6);
          byte[] buf = new byte[6];
          in.read(buf);
          if (DWOv1.equals (new String(buf, 0)))
          {
            in.reset();
            Provisioning p = new Provisioning();
            p.props.load(in);
            in.close();
            p.props.put(PROVISIONING_UPDATE_COUNT, new Integer(mod));
            ServiceRegistration<ProvisioningService> result = context.registerService(ProvisioningService.class, p, (Dictionary)p.props);
            return result;
          }
          in.close();
          
        } catch (Exception oops) {
          
        }
      }
      return null;
  }

  Properties props = new Properties();
  
  @SuppressWarnings("rawtypes")
  @Override
  public void addInformation(Dictionary dict) {
  }

  @Override
  public void addInformation(ZipInputStream arg0) throws IOException {
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Dictionary getInformation() {
    return props;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void setInformation(Dictionary arg0) {
  }
}
