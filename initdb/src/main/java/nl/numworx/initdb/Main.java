package nl.numworx.initdb;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.Version;

import fi.dwo.commons.persistence.entities.PersistentACL;
import fi.dwo.commons.persistence.entities.PersistentApplet;
import fi.dwo.commons.persistence.entities.PersistentAppletConfig;
import fi.dwo.commons.persistence.entities.PersistentAppletConfigData;
import fi.dwo.commons.persistence.entities.PersistentClassCourse;
import fi.dwo.commons.persistence.entities.PersistentCourse;
import fi.dwo.commons.persistence.entities.PersistentCourseData;
import fi.dwo.commons.persistence.entities.PersistentDwoProfile;
import fi.dwo.commons.persistence.entities.PersistentDwoSystemParameters;
import fi.dwo.commons.persistence.entities.PersistentHasRole;
import fi.dwo.commons.persistence.entities.PersistentHasRolePK;
import fi.dwo.commons.persistence.entities.PersistentImage;
import fi.dwo.commons.persistence.entities.PersistentLoginContext;
import fi.dwo.commons.persistence.entities.PersistentMFA;
import fi.dwo.commons.persistence.entities.PersistentMethod;
import fi.dwo.commons.persistence.entities.PersistentSamlUser;
import fi.dwo.commons.persistence.entities.PersistentSchool;
import fi.dwo.commons.persistence.entities.PersistentSchoolClass;
import fi.dwo.commons.persistence.entities.PersistentSchoolData;
import fi.dwo.commons.persistence.entities.PersistentSchoolGroup;
import fi.dwo.commons.persistence.entities.PersistentSchoolMethod;
import fi.dwo.commons.persistence.entities.PersistentScoContext;
import fi.dwo.commons.persistence.entities.PersistentScoData;
import fi.dwo.commons.persistence.entities.PersistentScoPage;
import fi.dwo.commons.persistence.entities.PersistentStudentModelContext;
import fi.dwo.commons.persistence.entities.PersistentStudentModelData;
import fi.dwo.commons.persistence.entities.PersistentStudentModelItem;
import fi.dwo.commons.persistence.entities.PersistentStudentModelOfClass;
import fi.dwo.commons.persistence.entities.PersistentStudentOfClass;
import fi.dwo.commons.persistence.entities.PersistentStudentScoContext;
import fi.dwo.commons.persistence.entities.PersistentStudentScoData;
import fi.dwo.commons.persistence.entities.PersistentTeacherOfClass;
import fi.dwo.commons.persistence.entities.PersistentUrnResource;
import fi.dwo.commons.persistence.entities.PersistentUser;
import fi.dwo.commons.system.MD5;
import fi.dwo.server.PersistentDataManagers.core.AppletManager;
import fi.dwo.server.PersistentDataManagers.core.DwoProfileManager;
import fi.dwo.server.PersistentDataManagers.core.DwoSystemParametersManager;
import fi.dwo.server.PersistentDataManagers.core.HasRoleManager;
import fi.dwo.server.PersistentDataManagers.core.SchoolGroupManager;
import fi.dwo.server.PersistentDataManagers.core.UserManager;
import nl.uu.fi.dwo.rest.dom.entities.RoleType;
import nl.uu.fi.dwo.rest.dom.entities.util.AboType;

public class Main {

	
	
	
/**
 * copy de source database to the dest database, using JPA.
 * The Persistent Entities:
fi.dwo.commons.persistence.entities.PersistentApplet</class>
fi.dwo.commons.persistence.entities.PersistentAppletConfig</class>
fi.dwo.commons.persistence.entities.PersistentClassCourse</class>
fi.dwo.commons.persistence.entities.PersistentCourse
fi.dwo.commons.persistence.entities.PersistentCourseData
fi.dwo.commons.persistence.entities.PersistentDwoProfile</class>
fi.dwo.commons.persistence.entities.PersistentDwoSystemParameters</class>
    <class>fi.dwo.commons.persistence.entities.PersistentFromTo</class>
fi.dwo.commons.persistence.entities.PersistentHasRole</class>
fi.dwo.commons.persistence.entities.PersistentImage</class>
fi.dwo.commons.persistence.entities.PersistentLoginContext</class>
fi.dwo.commons.persistence.entities.PersistentSamlUser</class>
fi.dwo.commons.persistence.entities.PersistentScoContext</class>
fi.dwo.commons.persistence.entities.PersistentScoData</class>
fi.dwo.commons.persistence.entities.PersistentSchool</class>
fi.dwo.commons.persistence.entities.PersistentSchoolClass</class>
fi.dwo.commons.persistence.entities.PersistentSchoolData</class>
fi.dwo.commons.persistence.entities.PersistentSchoolGroup</class>
fi.dwo.commons.persistence.entities.PersistentStudentOfClass</class>
fi.dwo.commons.persistence.entities.PersistentStudentScoContext</class>
fi.dwo.commons.persistence.entities.PersistentStudentScoData</class>
fi.dwo.commons.persistence.entities.PersistentTeacherOfClass</class>
fi.dwo.commons.persistence.entities.PersistentUrnResource</class>
fi.dwo.commons.persistence.entities.PersistentUser</class>
fi.dwo.commons.persistence.entities.PersistentStudentModelContext</class>
fi.dwo.commons.persistence.entities.PersistentStudentModelData</class>
fi.dwo.commons.persistence.entities.PersistentStudentModelOfClass</class>
fi.dwo.commons.persistence.entities.PersistentACL</class>
fi.dwo.commons.persistence.entities.PersistentStudentModelItem</class>
fi.dwo.commons.persistence.entities.PersistentMethod</class>
fi.dwo.commons.persistence.entities.PersistentSchoolMethod</class>
fi.dwo.commons.persistence.entities.PersistentScoPage</class>
fi.dwo.commons.persistence.entities.PersistentMFA</class>

 * 
 * @param args commandline arguments
 * @throws IOException
 */
	
	public static void main(String[] args) throws IOException, SQLException {
		Properties properties = new Properties();
		properties = new Properties();
		properties.load(Main.class.getResourceAsStream("/dest.properties"));
		if (args.length != 0) {
			FileInputStream in = new FileInputStream(args[0]);
			properties.load(in);
			in.close();
		}
		DestDB dest = new DestDB("DWO_COPYDB", properties);
		dest.install();
		List<PersistentDwoSystemParameters> parameters = Collections.emptyList();
 		try {
			parameters = DwoSystemParametersManager.findEntities();
		} catch (PersistenceException e) {
			System.err.println(e.getClass());
			Throwable t = e.getCause();
			System.err.println(t.getClass());
			t = t.getCause();
			System.err.println(t.getClass());
			if (t.getMessage().startsWith("Unknown database")) {
				String db = properties.getProperty("javax.persistence.jdbc.url");
				int index = db.indexOf("://")+1;
				String schema = db.substring(0, index);
				db = db.substring(index);
				URI uri = URI.create(db);
				db = uri.getPath().substring(1);
				String q = uri.getQuery();
				uri = uri.resolve("/");
				if(q != null) uri = uri.resolve("?"+q);
				String root = properties.getProperty("javax.persistence.jdbc.user");
				String test = properties.getProperty("javax.persistence.jdbc.password");
				Connection c = DriverManager.getConnection(schema + uri.toString(), root, test);
				Statement s = c.createStatement();
				s.execute("create database " + db);
				dest = new DestDB("DWO_COPYDB", properties);
				dest.install();
				parameters = DwoSystemParametersManager.findEntities(); // should work, empty parameters though
			}
		}
 // Database flavor and version
 		if (parameters.isEmpty()) {
 			PersistentDwoSystemParameters p;
 			EntityManager em = dest.getEntityManager();
 			try {
 // Flavor
 				em.getTransaction().begin();
 				p = new PersistentDwoSystemParameters("DBPlatform");
 				p.setValue(dest.flavor.toString());
 				em.persist(p);parameters.add(p);
// Database scheme Version
 				p = new PersistentDwoSystemParameters("DBVersion Major"); p.setValue("1");em.persist(p);parameters.add(p);
 				p = new PersistentDwoSystemParameters("DBVersion Minor"); p.setValue("5");em.persist(p);parameters.add(p);
 				p = new PersistentDwoSystemParameters("DBVersion Revision"); p.setValue("6"); em.persist(p);parameters.add(p);
 				em.getTransaction().commit();
 			} finally {
 				em.close();
 			}
 		}
// Essential school "Numworx Admin"
 		PersistentDwoSystemParameters adminschool = DwoSystemParametersManager.findEntity("DwoSchoolIndex");
 		if (adminschool == null) {
 			EntityManager em = dest.getEntityManager();
 			em.getTransaction().begin();
 			PersistentSchool school = new PersistentSchool();
 			school.setAboType(AboType.standard);
 			school.setSchoolLogin(UUID.randomUUID().toString().replace("-", ""));
 			school.setSchoolName("Numworx Admin");
 			school.setSchoolRights("_");
 			em.persist(school);
 			em.getTransaction().commit();
 			em.getTransaction().begin();
 			adminschool = new PersistentDwoSystemParameters("DwoSchoolIndex");
 			adminschool.setValue(school.getSchoolID().toString());
 			em.persist(adminschool);
 			parameters.add(adminschool);
 			em.getTransaction().commit();
 			em.close();
 		}
 // Essential school "Dwo free school"		
 		PersistentDwoSystemParameters nullschool = DwoSystemParametersManager.findEntity("NullSchoolIndex");
 		if (nullschool == null) {
 			EntityManager em = dest.getEntityManager();
 			em.getTransaction().begin();
 			PersistentSchool school = new PersistentSchool();
 			school.setAboType(AboType.free);
 			school.setSchoolLogin("null");
 			school.setSchoolName("Numworx Free School");
 			school.setSchoolRights("_");
 			em.persist(school);
 			em.getTransaction().commit();
 			em.getTransaction().begin();
 			nullschool = new PersistentDwoSystemParameters("NullSchoolLogin");
 			nullschool.setValue(school.getSchoolLogin());
 			em.persist(nullschool);
 			parameters.add(nullschool);
 			nullschool = new PersistentDwoSystemParameters("NullSchoolIndex");
 			nullschool.setValue(school.getSchoolID().toString());
 			em.persist(nullschool);
 			parameters.add(nullschool);
 			em.getTransaction().commit();
 			em.close();			
 		}
 	// Essential schoolgroups dwoadmin and free student
 		schoolgroups(adminschool, nullschool, properties);
 		dwoadmin(adminschool,properties);
 		profile("VO", "/vo", "nl", "<html>Dit is de Numworx DWO-omgeving voor voortgezet onderwijs. In deze omgeving is een bibliotheek beschikbaar met veel digitaal lesmateriaal. Er zijn modules met oefeningen , er zijn ook modules die een volledige lessenserie rond een onderwerp bevatten. Het menu aan de linkerzijde geeft toegang tot modules van verschillende niveaus. Klik op een niveau en kies een module. Elke module biedt een aantal activiteiten die je kunt starten en uitvoeren. <br><br> Met een (gratis) persoonlijk DWO-account kun je alle activiteiten uit de bibliotheek gebruiken. Je werk en resultaten worden opgeslagen. Ben je verbonden aan een school met een Numworx DWO-abonnement, dan zijn er in het linkermenu ook modules speciaal voor jouw school.  <br><br> Ben je leerling bij een school met een Numworx abonnement, dan zie je in het linkermenu de modules die je docent speciaal voor jouw klas heeft klaargezet. Je docent heeft ook inzicht in jouw werk. Klik op een module, kies een activiteit en je kunt aan de slag. </html>", "Numworx Modules Voortgezet Onderwijs");
	}

	private static void profile(String name, String url, String language, String description, String title) {
		PersistentDwoProfile profile = DwoProfileManager.findEntity(name);
		if (profile == null) {
			profile = new PersistentDwoProfile(null, name);
			profile.setBase(url);
			profile.setLanguage(language);
			profile.setTitle(title);
			profile.setDwoProfileText(description);
			profile.setDwoProfileRights("rp"); // default for HTML5
			profile.setDwoProfileDescription(title);
			DwoProfileManager.create(profile);
		}
	
}

	private static void dwoadmin(PersistentDwoSystemParameters adminschool, Properties properties) {
		PersistentSchool s = new PersistentSchool(Long.valueOf(adminschool.getValue()));
		PersistentSchoolGroup group = SchoolGroupManager.findBySchoolAndRole(s, RoleType.ADMIN);
		String username = properties.getProperty("admin.username", "dwoadmin");
		PersistentUser admin = UserManager.findByUserName(username);
		if (admin == null) {
			String password = properties.getProperty("admin.password", "dwomadmin");
			password = MD5.getHashString(password);
			String email = properties.getProperty("admin.email", "admin@example.com");
			Date now = new Date();
			admin = new PersistentUser(null, "Numworx", "Admin", username, password, email, now);
			admin.setInsertion("");
			admin.setSingleSchoolAccount(Boolean.FALSE);
			admin.setSchoolGroupId(group.getSchoolGroupID());
			admin = UserManager.create(admin);
			PersistentHasRole hasRole = new PersistentHasRole( admin.getId(), group.getSchoolGroupID());
			hasRole.setRegisterDate(now);
			hasRole.setRights("");
			HasRoleManager.create(hasRole);
		}
	
}

	private static void schoolgroups(PersistentDwoSystemParameters adminschool, PersistentDwoSystemParameters nullschool, Properties properties) {
		PersistentSchool admin = new PersistentSchool(Long.valueOf(adminschool.getValue()));
		PersistentSchool nulls = new PersistentSchool(Long.valueOf(nullschool.getValue()));
		PersistentSchoolGroup group = SchoolGroupManager.findBySchoolAndRole(admin, RoleType.ADMIN);
		if (group == null) {
			String password = properties.getProperty("admin.password", "dwoadmin");
			group = new PersistentSchoolGroup(null, RoleType.ADMIN.ordinal(), admin.getSchoolID().intValue(), password);
			SchoolGroupManager.create(group);
		}
		group = SchoolGroupManager.findBySchoolAndRole(nulls, RoleType.STUDENT);
		if (group == null) {
			String password = "null"; // by default
			group = new PersistentSchoolGroup(null, RoleType.STUDENT.ordinal(), nulls.getSchoolID().intValue(), password);			
			SchoolGroupManager.create(group);
		}
		
	}		
			
}
