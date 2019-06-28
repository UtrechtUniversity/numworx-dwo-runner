package fi.microserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZIP {


  public static void main(String[] args) throws Exception {
    File in = new File(args[0]);
    File out = new File(args[1]);
// convert DWOv1 naar DWOv2
    Properties p = new Properties();
    FileInputStream ins = new FileInputStream(in);
    p.load(ins);
    ins.close();
    FileOutputStream output = new FileOutputStream(out);
    ZipOutputStream zip = new ZipOutputStream(output);
    ZipEntry entry;
    Enumeration<?> names = p.propertyNames();
    while (names.hasMoreElements()) {
      String key = (String) names.nextElement();
      String value = p.getProperty(key);
      entry = new ZipEntry(key);
      entry.setExtra("text".getBytes());
      zip.putNextEntry(entry);
      zip.write(value.getBytes("UTF-8"));
      zip.closeEntry();
    }
    zip.close();

  }

}
