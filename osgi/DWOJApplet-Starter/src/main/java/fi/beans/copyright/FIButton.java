package fi.beans.copyright;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;

import fi.dwo.dwojapplet.boot.Starter;

@SuppressWarnings("serial")
public class FIButton extends JLabel implements ActionListener {

  private static final String HE_ENGLISH = "HE (English)";
  private static final String SE_ENGLISH = "SE (English)";
  private static final String VO = "VO";
  private static final String HO = "HO";
  private String title, text[];
  private JComboBox<String> lang;
  private JComboBox<String> profile;
	
	public FIButton() {
	}

	public FIButton(String title, String[] text) {
		this.title = title;
		this.text = text;
		//setText("\uFB01"); // Fi-ligature
		setIcon(new ImageIcon(getClass().getResource("DWO-docent.png")));
		setHorizontalAlignment(CENTER);
//		setBorder(BorderFactory.createLineBorder(Color.black));
		addMouseListener(new Mouse());
	}
	
	class Mouse extends MouseAdapter {

		@Override
		public void mouseReleased(MouseEvent e) {
			openInfo(e);
		}	
	}

    static class WL extends WindowAdapter {

      @Override
      public void windowClosing(WindowEvent e) {
          e.getWindow().dispose();
      }

      @Override
      public void windowDeactivated(WindowEvent e) {
          e.getWindow().dispose();
      }
    }
    
	public void openInfo(MouseEvent e) {
		Frame f = JOptionPane.getFrameForComponent(this);
		JDialog infoDialog = new JDialog(f, title,false);
		infoDialog.addWindowListener(new WL());
		infoDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		Box b = Box.createVerticalBox();
		for(String line : text) b.add(new JLabel(line));
		b.add(new JLabel(System.getProperty("java.vendor")  + " " + System.getProperty("java.version")));
		lang = new JComboBox<String>();
		lang.addItem("nl");
		lang.addItem("de");
		lang.addItem("en");
		lang.addItem("fr");
		lang.setEditable(true);
		lang.setSelectedItem(getLanguage().intern());
		profile = new JComboBox<String>();
		profile.addItem(VO);
        profile.addItem(HO);
		profile.addItem(SE_ENGLISH);
		profile.addItem(HE_ENGLISH);
		profile.setEditable(true);
		profile.setSelectedItem(fromProfile(getProfile()));
		profile.addActionListener(this);		
		b.add(profile);
        b.add(lang);
        
        JButton start = new JButton("Start");
        start.addActionListener(ev -> { 
          String item = (String) lang.getSelectedItem();
          saveLanguage(item);
          item = (String) profile.getSelectedItem();
          item = toProfile(item);
          safeProfile(item);
          restart(); });
        b.add(start);
        
		infoDialog.getContentPane().add(b);
		infoDialog.pack();
		Dimension size = f.getToolkit().getScreenSize();
		size.width -= infoDialog.getWidth()+3;
		size.height -= infoDialog.getHeight()+3;
		int x = e.getXOnScreen()-30;
		x = Math.max(0, x);
		x = Math.min(size.width, x);
		int y = e.getYOnScreen()-30;
		y = Math.max(0, y);
		y = Math.min(size.height, y);		
		infoDialog.setLocation(x, y);
		infoDialog.setVisible(true);
	}

	private String getProfile() {
		Configuration cf = Starter.cm.getConfiguration();
		Dictionary<String,Object> dict = cf.getProperties();
		if (dict == null||dict.get("profile") == null) return "77";
		return dict.get("profile").toString();
	}

	private String getLanguage() {
		Configuration cf = Starter.cm.getConfiguration();
		Dictionary<String,Object> dict = cf.getProperties();
		if (dict == null || dict.get("language") == null) return "nl";
		return dict.get("language").toString();
	}

  private void saveLanguage(String item) {
    Configuration cf = Starter.cm.getConfiguration();
    Dictionary<String, Object> dict = cf.getProperties();
    if (dict == null) dict = new Hashtable<String, Object>();
    if (item.equals(dict.get("language")))
    		return;
    dict.put("language", item);
    try {
    	cf.update(dict);
    	//restart();
    } catch (IOException e1) {
    }
  }

	@Override
	public void actionPerformed(ActionEvent e) {
	    System.out.println(e);
		String item = (String) profile.getSelectedItem();
        System.out.println(item);
        item = toProfile(item);
        toLanguage(item);
	}

  private void safeProfile(String item) {
    try {
			Configuration cf = Starter.cm.getConfiguration();
			Dictionary<String, Object> dict = cf.getProperties();
			if (dict == null) dict = new Hashtable<String, Object>();
			if (item.equals(dict.get("profile")))
					return;
			dict.put("profile", item);
			cf.update(dict);
		} catch(Exception e1) {
		}
  }

	private String toProfile(String item) {
    if (HO.equals(item)) return "99";
    if (VO.equals(item)) return "77";
    if (SE_ENGLISH.equals(item)) return "92";
    if (HE_ENGLISH.equals(item)) return "100";
    return item;
  }
	private String fromProfile(String item) {
      if("99".equals(item))    return HO;
      else if("77".equals(item)) return VO;
      else if("92".equals(item)) return SE_ENGLISH;
      else if("100".equals(item)) return HE_ENGLISH;
 	  return item;
	}
	
	private void toLanguage(String item) {
	  if("99".equals(item))    lang.setSelectedItem("nl");
	  else if("77".equals(item)) lang.setSelectedItem("nl");
	  else if("92".equals(item)) lang.setSelectedItem("en");
	  else if("100".equals(item)) lang.setSelectedItem("en");
      // fetch from server.
	  else try {
	    URL u = new URL("https://app.dwo.nl/dwo/rest/public/profile/get/" + item);
	    Object o = u.openConnection().getContent(new Class[] { String.class } );
	    System.out.println(o);
	    String s = o.toString();
	  } catch(Exception e) {
	    
	  }
	}
	

  private void restart() {
		Bundle b = Starter.cm.getBundleContext().getBundle();
		try {
			b.stop(Bundle.STOP_TRANSIENT);
			b.start(Bundle.START_TRANSIENT);
		} catch (Exception e) {
			java.util.logging.Logger.getLogger(getClass().getName()).log(Level.WARNING, "restart", e);
		}
	}

}
