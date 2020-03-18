package fi.dwo.eawt;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.JDialog;
import javax.swing.JFrame;

import fi.dwo.eawt.impl.EAWTImpl;

@SuppressWarnings("serial")
public class Main extends JFrame implements BooleanSupplier, Supplier<JDialog> {

  EAWTImpl eawt;
  
  public Main() {
    setTitle(getClass().getName());
    setSize(300,200); 
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    eawt = new EAWTImpl();
    eawt.setQuit(this);
    eawt.setAbout(this);
  }

  @SuppressWarnings("deprecation")
  public static void main(String[] args) {
      Main main = new Main();
      
      main.show();

  }

  @Override
  public boolean getAsBoolean() {
    System.err.println("Dit wordt aangeroepen");
    return true;
  }

  @Override
  public JDialog get() {
    return new JDialog(this, "About...",false);
  }

}
