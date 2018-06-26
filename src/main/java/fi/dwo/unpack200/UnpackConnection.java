package fi.dwo.unpack200;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;

import org.osgi.framework.BundleContext;

class UnpackConnection extends HttpURLConnection {

  public URLConnection getDelegate() throws IOException {
    if (delegate == null) {
      connect();
    }
    return delegate;
  }

  private URL full;
  private BundleContext context;
  private URLConnection delegate, proxy;


  public UnpackConnection(URL url, String full, BundleContext context) throws IOException {
    super(url);
    this.full = new URL(full);
    this.context = context;
    this.proxy = this.full.openConnection();
  }

  private boolean iszip(byte[] header) {
    int magic = ((header[1] & 0xFF) << 8) | (header[0] & 0xFF); // Little Endian
    return (magic == GZIPInputStream.GZIP_MAGIC) && (header[2] == 8);
  }

  private URLConnection fileConnection() throws IOException {
    URLConnection connection = proxy;
    InputStream in = connection.getInputStream();
    // optional gzip?
    BufferedInputStream bin = new BufferedInputStream(in);
    byte[] header = new byte[3];
    bin.mark(header.length);
    int len = bin.read(header);
    if(len < header.length)
      return proxy;
    bin.reset();
    in = bin;
    if (iszip(header)) {
      in = new GZIPInputStream(in);
    }
    Unpacker u = Pack200.newUnpacker();
    File temp = context.getDataFile("temp");
    if (temp != null) temp.mkdirs();
    File filename = File.createTempFile("pack", ".jar", temp);
    JarOutputStream out = new JarOutputStream(new FileOutputStream(filename));
    u.unpack(in, out);
    out.close();
    connection = filename.toURI().toURL().openConnection();
    filename.deleteOnExit();
    return connection;
  }


  @Override
  public void connect() throws IOException {
    proxy.connect();
    delegate = fileConnection();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return getDelegate().getInputStream();
  }

  @Override
  public void disconnect() {
    if (proxy instanceof HttpURLConnection) {
      ((HttpURLConnection) proxy).disconnect();
    }
  }

  @Override
  public boolean usingProxy() {
    return false;
  }

  @Override
  public void setIfModifiedSince(long ifmodifiedsince) {
    proxy.setIfModifiedSince(ifmodifiedsince);
  }

  public void setConnectTimeout(int timeout) {
    proxy.setConnectTimeout(timeout);
  }

  public int getConnectTimeout() {
    return proxy.getConnectTimeout();
  }

  public void setReadTimeout(int timeout) {
    proxy.setReadTimeout(timeout);
  }

  public int getReadTimeout() {
    return proxy.getReadTimeout();
  }

  public long getExpiration() {
    return proxy.getExpiration();
  }

  public long getDate() {
    return proxy.getDate();
  }

  public long getLastModified() {
    return proxy.getLastModified();
  }

  public String getHeaderField(String name) {
    return proxy.getHeaderField(name);
  }

  @Override
  public boolean getInstanceFollowRedirects() {
    if (proxy instanceof HttpURLConnection) return ((HttpURLConnection) proxy).getInstanceFollowRedirects();
    return super.getInstanceFollowRedirects();
  }

  @Override
  public String getRequestMethod() {
    if (proxy instanceof HttpURLConnection) return ((HttpURLConnection) proxy).getRequestMethod();
    return "GET";
  }

  @Override
  public int getResponseCode() throws IOException {
    if (proxy instanceof HttpURLConnection) return ((HttpURLConnection) proxy).getResponseCode();
    return -1;
  }

  @Override
  public String getResponseMessage() throws IOException {
    if (proxy instanceof HttpURLConnection) return ((HttpURLConnection) proxy).getResponseMessage();
    return null;
  }

  public Map<String, List<String>> getHeaderFields() {
    return proxy.getHeaderFields();
  }

  public int getHeaderFieldInt(String name, int Default) {
    return proxy.getHeaderFieldInt(name, Default);
  }

  public long getHeaderFieldLong(String name, long Default) {
    return proxy.getHeaderFieldLong(name, Default);
  }

  public long getHeaderFieldDate(String name, long Default) {
    return proxy.getHeaderFieldDate(name, Default);
  }

  public String getHeaderFieldKey(int n) {
    return proxy.getHeaderFieldKey(n);
  }

  public String getHeaderField(int n) {
    return proxy.getHeaderField(n);
  }

  public Permission getPermission() throws IOException {
    return proxy.getPermission();
  }

  public void setAllowUserInteraction(boolean allowuserinteraction) {
    proxy.setAllowUserInteraction(allowuserinteraction);
  }

  public boolean getAllowUserInteraction() {
    return proxy.getAllowUserInteraction();
  }

  public void setUseCaches(boolean usecaches) {
    proxy.setUseCaches(usecaches);
  }

  public boolean getUseCaches() {
    return proxy.getUseCaches();
  }

  public long getIfModifiedSince() {
    return proxy.getIfModifiedSince();
  }

  public void setDefaultUseCaches(boolean defaultusecaches) {
    proxy.setDefaultUseCaches(defaultusecaches);
  }

  public void setRequestProperty(String key, String value) {
    proxy.setRequestProperty(key, value);
  }

  public String getRequestProperty(String key) {
    return proxy.getRequestProperty(key);
  }

  public Map<String, List<String>> getRequestProperties() {
    return proxy.getRequestProperties();
  }

//  @Override
//  public URL getURL() {
//    return full;
//  }

  public int getContentLength() {
    try {
      return getDelegate().getContentLength();
    } catch (IOException e) {
      return -1;
    }
  }

  public long getContentLengthLong() {
    try {
      return getDelegate().getContentLengthLong();
    } catch (IOException e) {
      return -1L;
    }
  }

  public String getContentType() {
    try {
      return getDelegate().getContentType();
    } catch (IOException e) {
      return null;
    }
  }

  public String getContentEncoding() {
    try {
      return getDelegate().getContentEncoding();
    } catch (IOException e) {
      return null;
    }
  }

  public Object getContent() throws IOException {
    return getDelegate().getContent();
  }

  public Object getContent(Class[] classes) throws IOException {
    return getDelegate().getContent(classes);
  }

}
