package fi.dwo.dwojapplet.boot;

import java.awt.BorderLayout;

import fi.beans.mainframe.AppletContext;
import fi.beans.mainframe.AppletStub;
import fi.beans.mainframe.JApplet;

@SuppressWarnings("serial")
class ParentOf extends JApplet implements AppletStub {
	private final JApplet dwo;
	private AppletStub stub;

	ParentOf(JApplet dwo) {
		this.dwo = dwo;
		dwo.setStub(this);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(dwo, BorderLayout.CENTER);
	}

	public void init() {
		dwo.init();
	}

	public void start() {
		dwo.start();
	}

	public void stop() {
		dwo.stop();
	}

	public void destroy() {
		dwo.destroy();
	}

	public String toString() {
		return dwo.toString();
	}

	@Override
	public void appletResize(int width, int height) {
		
	}
   
	public void setStub(AppletStub stub) {
		this.stub = stub;
		super.setStub(stub);
	}
	public AppletContext getAppletContext() {
		if (stub == null) {
			return super.getAppletContext(); // hier gaat iets mis!
		}
		return stub.getAppletContext();
	}
	
	
}
