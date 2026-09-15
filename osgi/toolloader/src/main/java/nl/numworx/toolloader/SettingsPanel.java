package nl.numworx.toolloader;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import fi.beans.scorm.SAMLLoginIF;
import fi.dwo.bootloader.impl.Config;
import nl.uu.fi.dwo.lms.jclient.lib.rest.cache.PublicProfileCache;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.OAuthManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.SecureTeacherStudentModelManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.SecureUserAccountLoginsManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.SecureUserAccountManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.transport.StoredRestManager;
import nl.uu.fi.dwo.rest.DwoLocale;
import nl.uu.fi.dwo.rest.dom.entities.DomContext;
import nl.uu.fi.dwo.rest.dom.entities.DomDwoProfile;
import nl.uu.fi.dwo.rest.dom.entities.DomHasRole;
import nl.uu.fi.dwo.rest.dom.entities.DomId;
import nl.uu.fi.dwo.rest.dom.entities.DomLoginContext;
import nl.uu.fi.dwo.rest.dom.entities.DomSchoolRoleAndClassV2;
import nl.uu.fi.dwo.rest.dom.entities.DomSchoolsRolesAndClassesV2;
import nl.uu.fi.dwo.rest.dom.entities.DomStudentModelContext;
import nl.uu.fi.dwo.rest.dom.entities.DomStudentModelContextId;
import nl.uu.fi.dwo.rest.dom.entities.DomUserFull;
import nl.uu.fi.dwo.rest.exceptions.Dwo2Exception;
import nl.uu.fi.dwo.rest.exceptions.Dwo2ExceptionCode;
import nl.uu.fi.dwo.rest.util.DWO2ExceptionTranslatorInterface;
import nl.uu.fi.dwo.rest.util.Dwo2ExceptionTranslator;

@SuppressWarnings("serial")
public class SettingsPanel extends JPanel implements ItemListener {

	public class Translator implements DWO2ExceptionTranslatorInterface {

		@Override
		public String encodeJSON(Dwo2ExceptionCode code, String message) {
			// TODO Auto-generated method stub
			return code.name();
		}

		@Override
		public String decodeMessageInJSON(String json) {
			// TODO Auto-generated method stub
			return "";
		}

		@Override
		public Dwo2ExceptionCode decodeCodeInJSON(String json) {
			// TODO Auto-generated method stub
			return Dwo2ExceptionCode.valueOf(json);
		}

		@Override
		public String getLocalizedCodeExplanation(DwoLocale locale, Dwo2ExceptionCode code) {
			// TODO Auto-generated method stub
			return code.toString();
		}

	}


	private BundleContext context;
	
	JTextField name, profile;
	JButton    loginBtn, okBtn, cancelBtn;
	JComboBox<String>  schools, language;

	private Supplier<SAMLLoginIF> login;

	public static URI serverURI = URI.create("https://test.dwo.nl/dwo/");

	private Config config;

	private ServiceRegistration<ManagedService> ref;

	private JComboBox<DomStudentModelContext> modelselect;
	private DomSchoolsRolesAndClassesV2 logins;
	
	@SuppressWarnings("serial")
	class LoginAction extends AbstractAction implements Predicate<Dwo2Exception> , ManagedService {

		private String token;

		@Override
		public void actionPerformed(ActionEvent e) {
			SAMLLoginIF panel = login.get();
			panel.setEndpoint("/dwo/oauth2/entree");
			panel.popup(SettingsPanel.this, serverURI + "oauth2/login3.jsp?idphint=dwo")
			.then(pr -> {
				System.out.println("got " + pr.getValue());
				if (ref != null) ref.unregister();
				ref = null;
			    loginViaSaml(pr.getValue());
				return pr;
			}).then(null, (p) -> {
				p.getFailure().printStackTrace(); // fatal.. or retry
			});
		}

		public LoginAction(String name) {
			super(name);
		}
	    public void loginViaSaml(Properties p) throws Exception {
	       String authToken = p.getProperty("dwoSAMLAuthToken");
		   CookieHandler handler = CookieHandler.getDefault();
			  Map<String, List<String>> responseHeaders = new HashMap<>();
			  List<String> cookies = new ArrayList<>();
			  for(Map.Entry<Object, Object> entry: p.entrySet()) {
			    if (entry.getKey().toString().startsWith("dwo")) {
			      String value = entry.getValue().toString();
			      if (value.contains(":")) // need escape?
			        value = "\"" + value + "\""; // ons kent ons
			      cookies.add( ( entry.getKey() + "=" + value));
			    }
			  }
			  responseHeaders.put("Set-Cookie", cookies); // inject DWO cookies.
			  handler.put(serverURI, responseHeaders);
			
			  String token = p.getProperty("dme.oauth.code");
			  String clientId = p.getProperty("dme.oauth.client_id");
			  String verifier = p.getProperty("dme.oauth.code_verifier");
			  String redirectUri = p.getProperty("dme.oauth.redirect_uri");
			  loginWithToken(token, clientId, verifier, redirectUri);
	    }
	    
	    public void loginWithToken(String authToken, String clientId, String verifier, String redirectUri) throws Dwo2Exception {
	        OAuthManager m = new OAuthManager();
	        token = m.authorization_token(authToken, clientId, verifier, redirectUri);
	        logintail();     
	      }

		private void logintail() throws Dwo2Exception {
			if (token != null) {
	          StoredRestManager.getInstance().setRecover(this);	          
	        }
	        DomLoginContext loginContext = SecureUserAccountManager.getLoginContext();
	        DomContext context = StoredRestManager.getInstance().getAuthenticator().getContext();
	        if (context == null) {
	          StoredRestManager.getInstance().getAuthenticator().setContext(context = new DomContext());
	        }
	        context.setRealm(loginContext.getRealm());
	        context.setDomHasRole(new DomHasRole());
	        context.getDomHasRole().setId(loginContext.getHasRoleId());
	        DomUserFull user = SecureUserAccountManager.getAccountData();
	        name.setText(user.getUserName());
	        name.setEnabled(false);
logins = SecureUserAccountLoginsManager.getSchoolLogins();
	        DefaultComboBoxModel<String>model = new DefaultComboBoxModel<>();
			for (DomSchoolRoleAndClassV2 login : logins.getSchoolsRolesAndClassesList()) {
				if (login.getRole().getRoleName().equals("TEACHER"))
					model.addElement(login.getSchool().getSchoolName());
			}
			schools.setModel(model);
			schools.setSelectedItem(logins.getActiveSchoolRoleAndClass().getSchool().getSchoolName());
			setRefreshToken(token);
			selectAfterLogin();
		}

		@Override
		public boolean test(Dwo2Exception t) {
			return false;
		}

		@Override
		public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
			if (properties != null) {
				Object token = properties.get("refresh_token");
				if (token != null) {
					OAuthManager m = new OAuthManager();
					this.token = m.refresh_token(token.toString());
					try {
						ref.unregister();ref = null;
						logintail();
						token = properties.get("school");
						if (token != null) {
							schools.setSelectedItem(token);
						}
						
					} catch (Dwo2Exception e) {
						
					}
				}
				
			}
			
		}

		
		
	}
	
	
	public SettingsPanel(BundleContext context, Supplier<SAMLLoginIF> samlLogin, Config config) {
		super(null);
		this.context = context;
		this.login = samlLogin;
		this.config = config;
// install codebase:		
		String codebase = config.getProperty("fi.dwo.codebase").toString();
		try {
			serverURI = new URI(codebase);
			StoredRestManager.getInstance().getAuthenticator().setServerUrlPath(serverURI.toURL());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        Dwo2ExceptionTranslator.setTranslator(new Translator());
        
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		Box line; 
		line = Box.createHorizontalBox();
		line.add(new JLabel("Naam"));
		name = new JTextField((String) config.getProperty("userName"));
		line.add(name);
		add(line);
		line = Box.createHorizontalBox();
		line.add(Box.createGlue());
		LoginAction loginAction = new LoginAction("login");
		loginBtn = new JButton(loginAction);
		line.add(loginBtn);
		add(line);
		line = Box.createHorizontalBox();
		line.add(new JLabel("school"));
		String s = (String) config.getProperty("school");
		schools = s != null ? new JComboBox<>(new String[] { s}) : new JComboBox<>();
		schools.addItemListener(this);
		line.add(schools);
		add(line);
		line = Box.createHorizontalBox();
		line.add(new JLabel("profiel"));
		s = (String) config.getProperty("profile");
		if (s == null) s = "wa";
		profile = new JTextField(s);
		line.add(profile);
		add(line);
		line = Box.createHorizontalBox();
		line.add(new JLabel("taal"));
		language = new JComboBox<String>(new String[] {"nl", "en" });
		s = (String) config.getProperty("language");
		if (s != null) language.setSelectedItem(s);
		line.add(language);
		add(line);
		add(Box.createVerticalStrut(20));
		
		
		List<DomStudentModelContext> list = new ArrayList<>();
//		try {
//			SecureTeacherStudentModelManager modelManager = new SecureTeacherStudentModelManager();
//			DomDwoProfile dwoprofile = PublicProfileCache.get(profile.getText());
//			list = modelManager.getReducedList(dwoprofile);
//		} catch (Dwo2Exception|RuntimeException e) {
//		}
		list.add(0, null);
		StudentComboBoxModel model = new StudentComboBoxModel(list);
		modelselect = new JComboBox<DomStudentModelContext>(model);
		modelselect.setRenderer(new StudentModelRenderer());
		modelselect.addItemListener(this);
		line = Box.createHorizontalBox();
		s = (String) config.getProperty("studentmodelcontext");
		modelselect.setSelectedItem(model.find(s));
		line.add(new JLabel("Model"));
		line.add(modelselect);		
		add(line);

		line = Box.createHorizontalBox();
		okBtn = new JButton("OK");
		line.add(okBtn);
		line.add(Box.createGlue());
		cancelBtn = new JButton("Annuleer");
		line.add(cancelBtn);
		add(line);
		
		Dictionary<String, String> dict = new Hashtable<String, String>();
		dict.put(Constants.SERVICE_PID, context.getBundle().getSymbolicName());
		ref = context.registerService(ManagedService.class, loginAction, dict);

	}

	
	private void selectAfterLogin() {
		List<DomStudentModelContext> list = new ArrayList<>();
		try {
			SecureTeacherStudentModelManager modelManager = new SecureTeacherStudentModelManager();
			DomDwoProfile dwoprofile = PublicProfileCache.get(profile.getText());
			list = modelManager.getReducedList(dwoprofile);
		} catch (Dwo2Exception e) {
		}
		list.add(0, null);
		StudentComboBoxModel model = new StudentComboBoxModel(list);
		String s = (String) config.getProperty("studentmodelcontext");
		modelselect.setModel(model);		
		modelselect.setSelectedItem(model.find(s));

	}
	
	
	
	
	public void setRefreshToken(String token) {
		config.setProperty("refresh_token", token);
		config.setProperty("userName", name.getText());
	}



	String getId() {
		DomStudentModelContext c = (DomStudentModelContext) modelselect.getSelectedItem();
		if (c == null) return null;
		return c.getId().getIdString();
	}



	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == e.SELECTED) {
			Object item = e.getItem();
			if (e.getSource() == modelselect) {		
				if (item instanceof DomStudentModelContext) {
					config.setProperty("studentmodelcontext", ((DomStudentModelContext) item).getId().getIdString());
				}
			} else if (e.getSource() == schools) {
				DomContext context = StoredRestManager.getInstance().getAuthenticator().getContext();
				for (DomSchoolRoleAndClassV2 login : logins.getSchoolsRolesAndClassesList()) {
					if (login.getRole().getRoleName().equals("TEACHER"))
						if (login.getSchool().getSchoolName().equals(item)) {
							DomHasRole hr = login.getHasRole();
							context.setDomHasRole(hr);
							break;
						}
				}
				selectAfterLogin();

			}
		}
		
	}
	
}
