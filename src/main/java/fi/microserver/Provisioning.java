package fi.microserver;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import org.osgi.framework.*;
import org.osgi.service.provisioning.*;

import ch.jm.osgi.provisioning.ProvisioningServiceImpl;

public class Provisioning {
  static String DWOv1 = "#DWOv1";
  static String DWOv2 = "PK";

  @SuppressWarnings({"deprecation"})
  static ProvisioningService install(BundleContext context) {
    ProvisioningServiceImpl s = new ProvisioningServiceImpl(context);
    s.start();
    String uri = context.getProperty("fi.dwo.provisioning");
    if (uri != null) {
      try {
        URL url = new URL(uri);
        URLConnection uc = url.openConnection();
        BufferedInputStream in = new BufferedInputStream(uc.getInputStream());
        in.mark(6);
        byte[] buf = new byte[6];
        in.read(buf);
        String string = new String(buf, 0);
        if (DWOv1.equals(string)) {
          in.reset();
          Properties props = new Properties();
          props.load(in);
          s.addInformation(props);
        } else if (DWOv2.equals(string.substring(0, 2))) {
          in.reset();
          s.addInformation(new ZipInputStream(in));
        }
        in.close();

      } catch (Exception oops) {

      }
    }
    return s;
  }


}
