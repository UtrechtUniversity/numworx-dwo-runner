package fi.microserver;

public class Numworx {

	public static void main(String[] args) throws Exception {
	    if (args.length == 0) {
	        args = new String[] { "numworx.dwo" };
	    }
		MicroServer.main(args);
	}

}
