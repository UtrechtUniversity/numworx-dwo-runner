package fi.dwo.dwojapplet.boot;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.BorderLayout;

@SuppressWarnings("serial")
class ParentOf extends Applet implements AppletStub {
	private Applet dwo;

	ParentOf(Applet dwo) {
		this.dwo = dwo;
		dwo.setStub(this);
		setLayout(new BorderLayout());
		add(dwo, BorderLayout.CENTER);
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

	
	
}
