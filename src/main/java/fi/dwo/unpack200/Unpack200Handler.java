package fi.dwo.unpack200;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;

import org.osgi.framework.BundleContext;
import org.osgi.service.url.AbstractURLStreamHandlerService;

class Unpack200Handler extends AbstractURLStreamHandlerService {

	BundleContext context;
	
	public static final String PROTOCOL = "pack200";
	
	Unpack200Handler(BundleContext context2) {
		context = context2;
	}

	@Override
	public URLConnection openConnection(URL url) throws IOException {
// Parse the url:
        String full = url.toExternalForm();
// Remove pack200:
        if (full.startsWith(PROTOCOL+':')) {
            full = full.substring(PROTOCOL.length()+1);
        }
// Remove '/' or '//'
        while (full.startsWith("/")) {
            full = full.substring(1);
        }
        full = full.trim();
		URLConnection connection = new URL(full).openConnection();
		InputStream in = connection.getInputStream();
// optional gzip?
		BufferedInputStream bin = new BufferedInputStream(in);
		byte[] header = new byte[3];
		bin.mark(header.length);
		bin.read(header);
		bin.reset();
		in = bin;
		if(iszip(header))
		{
			in = new GZIPInputStream(in);
		}
		Unpacker u = Pack200.newUnpacker();
		File temp = context.getDataFile("temp");
		if(temp != null) 
			temp.mkdirs();
		File filename = File.createTempFile("pack", ".jar", temp);
		JarOutputStream out = new JarOutputStream(new FileOutputStream(filename));
		u.unpack(in, out);
		out.close();
		connection = filename.toURI().toURL().openConnection();
		filename.deleteOnExit();
		return connection;
	}

	private boolean iszip(byte[] header) {
		int magic = ((header[1]&0xFF) << 8)|(header[0]&0xFF); // Little Endian
		return (magic == GZIPInputStream.GZIP_MAGIC) && (header[2] == 8);
	}

}
