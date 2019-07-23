package nl.numworx.dwo.maintenance;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import fi.dwo.commons.persistence.Dwo2ExceptionJavaTranslator;
import fi.dwo.commons.system.MD5;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.LoginManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.SecureUserAccountLoginsManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.SecureUserAccountManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.managers.SecuredDwoAdminGarbageManager;
import nl.uu.fi.dwo.lms.jclient.lib.rest.transport.RestAuthenticator;
import nl.uu.fi.dwo.lms.jclient.lib.rest.transport.StoredRestManager;
import nl.uu.fi.dwo.rest.dom.entities.DomClassCourse;
import nl.uu.fi.dwo.rest.dom.entities.DomContext;
import nl.uu.fi.dwo.rest.dom.entities.DomLoginContext;
import nl.uu.fi.dwo.rest.dom.entities.DomSchoolsRolesAndClassesV2;
import nl.uu.fi.dwo.rest.dom.entities.DomUser;
import nl.uu.fi.dwo.rest.dom.entities.DomUserFullwLoginContext;
import nl.uu.fi.dwo.rest.exceptions.Dwo2Exception;
import nl.uu.fi.dwo.rest.util.Dwo2ExceptionTranslator;

public class Maintenance {

  static Logger LOG = Logger.getLogger(Maintenance.class.getName());
  static {
    Dwo2ExceptionTranslator.setTranslator(new Dwo2ExceptionJavaTranslator());
  }

  private String user = "root";
  private String pass = "test";
  private String url  = "http://localhost:8080/dwo/";
  private Integer amount;
  private Long since;
  
  private RestAuthenticator authenticator;
  private StoredRestManager manager;
  private DomContext context = new DomContext();
  private DomLoginContext logincontext;
  private SecuredDwoAdminGarbageManager garbage;
  
  
  public Maintenance() {
    manager = StoredRestManager.getInstance();
    authenticator = manager.getAuthenticator();
    authenticator.setContext(context);
    garbage = new SecuredDwoAdminGarbageManager(manager);
  }

  public void login() throws Dwo2Exception {
    DomUserFullwLoginContext result = LoginManager.basicLogin(user, md5(pass));
    context.setRealm(result.getDomLoginContext().getRealm());
    DomSchoolsRolesAndClassesV2 schools = SecureUserAccountLoginsManager.getSchoolLogins();
    context.setDomHasRole(schools.getActiveSchoolRoleAndClass().getHasRole());
    logincontext = result.getDomLoginContext();
  }
  
  private String md5(String pass) {
    return MD5.getHashString(pass);
  }

  public void logout() throws Dwo2Exception {
    SecureUserAccountManager.logoutUser(logincontext);
  }
  
  public static void main(String[] args) throws Exception {
    Maintenance main = new Maintenance();
    main.setURL(System.getProperty("maintenance.url",main.url));
    main.setUser(System.getProperty("maintenance.user", main.user));
    main.setPass(System.getProperty("maintenance.pass", main.pass));
    main.setAmount(100);
    main.login();
  
    long count = main.getUsers()       
        .map(main::removeUser)
        .filter(Boolean::booleanValue)
        .count();  
    LOG.info("count removed users " + count);
    
    count = main.getContexts()
        .map(main::removeContext)
        .filter(Boolean::booleanValue)
        .count();
    LOG.info("count removed contexts " + count);

    count = main.getClassCourses()
    		.map(main::removeClassCourse)
    		.filter(Boolean::booleanValue)
    		.count();
    LOG.info("count removed classcourses " + count);
    
    
    // classes with stale members.
    

    main.logout();

  }

  private Stream<DomClassCourse> getClassCourses() throws Dwo2Exception {
	return garbage.getClassCourses(amount).stream().limit(amount);
  }

  private Boolean removeClassCourse(DomClassCourse cc) {
	  try {
		  return garbage.removeClassCourse(cc);
	  } catch (Dwo2Exception e) {
	      LOG.log(Level.SEVERE, "remove ClassCourse " + cc.getId(), e);
	      return Boolean.FALSE;
	  }
  }
  
  private Stream<DomUser> getUsers() throws Dwo2Exception {
    return garbage.getUsers(amount, since).stream()
        .limit(getAmount().longValue())
        .filter(this::isOldUser)
        .map(DomUserFullwLoginContext::getDomUserFull);
  }
  
  Date old = new Date(System.currentTimeMillis() - 1000L* 3600 * 24 * 265 * 2);
  private boolean isOldUser(DomUserFullwLoginContext dom) {
    Long register = dom.getDomLoginContext().getRegisterTimeStamp();
    Long login    = dom.getDomLoginContext().getLastLoginTimeStamp();
    Date from = register != null ? new Date(register.longValue()) : new Date(0);
    Date last = login != null ? new Date(login.longValue()) : from;
    LOG.info("user " + dom.getDomUserFull().getUniqueDisplayName() + " from " + from + " last " + last);
    return last.before(old);
  }
  
  
  private Stream<DomLoginContext> getContexts() throws Dwo2Exception {
    return garbage.getContexts(amount).stream().limit(amount.longValue());
  }

  void setURL(String url) throws MalformedURLException {
    authenticator.setServerUrlPath(new URL(url));
    LOG.info("Connect to url " + url);
  }

  private Boolean removeUser(DomUser user) {
    try {
      return garbage.removeUser(user);
    } catch (Dwo2Exception e) {
      LOG.log(Level.SEVERE, "remove User " + user.getUniqueDisplayName(), e);
      return Boolean.FALSE;
    }
  }
  
  private Boolean removeContext(DomLoginContext context) {
    try {
      return garbage.removeContext(context);
    } catch (Dwo2Exception e) {
      LOG.log(Level.SEVERE, "remove Context " + context.getUserId(), e);
      return Boolean.FALSE;
    }
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * @param user the user to set
   */
  public void setUser(String user) {
    this.user = user;
    LOG.info("Connect as " + user);
  }

  /**
   * @return the pass
   */
  public  String getPass() {
    return pass;
  }

  /**
   * @param pass the pass to set
   */
  public void setPass(String pass) {
    this.pass = pass;
  }

  /**
   * @return the amount
   */
  public Integer getAmount() {
    return amount;
  }

  /**
   * @param amount the amount to set
   */
  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  /**
   * @return the since
   */
  public Long getSince() {
    return since;
  }

  /**
   * @param since the since to set
   */
  public void setSince(Long since) {
    this.since = since;
  }
  
  public void setDuration(long time) {
    setSince(System.currentTimeMillis()-time);
  }
}
