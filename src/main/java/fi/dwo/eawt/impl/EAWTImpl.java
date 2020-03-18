package fi.dwo.eawt.impl;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.JDialog;

import fi.dwo.eawt.EAWT;

public class EAWTImpl implements AboutHandler, QuitHandler, EAWT {
 
  private Desktop desktop;
  private Supplier<JDialog> about;
  private BooleanSupplier quit;

  public EAWTImpl() {
    if ( Desktop.isDesktopSupported()) {
      desktop = Desktop.getDesktop();
    }
  }
  
  public void setAbout(Supplier<JDialog> about) {
    this.about = about;
    try {
      if (desktop.isSupported(Action.APP_ABOUT))
        desktop.setAboutHandler(this);
    } catch (Exception e) {
      // TODO logservice
      //e.printStackTrace();
    }
  }

  public void setQuit(BooleanSupplier quit) {
    this.quit = quit;
    try {
      if(desktop.isSupported(Action.APP_QUIT_HANDLER))
        desktop.setQuitHandler(this);
    } catch (Exception e) {
      // TODO Logservice!
      //e.printStackTrace();
    }
  }
  
  @Override
  public void handleAbout(AboutEvent e) {
      if (about != null) {
          about.get().show();
      }
  }

  @Override
  public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
    if (quit == null||quit.getAsBoolean())
      response.performQuit();
    else
      response.cancelQuit();
    
  }
  
}
