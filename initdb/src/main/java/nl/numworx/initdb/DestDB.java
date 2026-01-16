package nl.numworx.initdb;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.eclipse.persistence.internal.security.PrivilegedGetDeclaredFields;

import fi.dwo.commons.persistence.entities.PersistentCourse;
import fi.dwo.server.persistence.DwoEmfFactory;
import nl.uu.fi.dwo.rest.dom.entities.util.DelState;

public class DestDB {

	private final EntityManagerFactory _instance;
	public enum Flavor { 
		MYSQL,
		POSTGRES
	}
	
	public Flavor flavor = Flavor.MYSQL;

	public DestDB(String unit, Properties prop) {
		_instance = Persistence.createEntityManagerFactory(unit, prop);
		try {
			flavor = Flavor.valueOf(prop.getProperty("flavor", flavor.name()));
		} catch (Exception oops) {}
	}
    public EntityManager getEntityManager() {
        return _instance.createEntityManager();
    }

    
    public void install() {
    	DwoEmfFactory.set_instance(_instance);
    }
    
    
    Long getLastChangeTimestamp(Class<?> clazz) {
    	EntityManager em = getEntityManager();
    	String name = clazz.getSimpleName();
    	try {
    		String select = "SELECT MAX(p.lastChangeTimeStamp) FROM " + name + " p ";
			TypedQuery<Long> query = em.createQuery(select, Long.class);
    		return query.getSingleResult();
    	} catch(Exception oops) {
    		return 0L;
    	} finally {
    		em.close();
    	}
    }
    
    public void setAutoIncrement(String table, String id, long value) {
    	EntityManager em = getEntityManager();
    	setAutoIncrement(table, id, value, em);
    	em.close();
   }
	protected void setAutoIncrement(String table, String id, long value, EntityManager em) {
		switch(flavor) {
    	case POSTGRES: setAutoIncrement_POSTGRES(table, id, value, em);break;
    	case MYSQL: setAutoIncrement_MYSQL(table, value, em);
    	}
	}

    protected void setAutoIncrement_MYSQL(String table, long value, EntityManager em) {
		String alter = "ALTER TABLE " + table + " AUTO_INCREMENT=" + value; // DIT IS MYSQL
		updateNative(alter, em);
	}
	protected void updateNative(String alter, EntityManager em) {
		try {
			em.getTransaction().begin();
	    	Query q = em.createNativeQuery(alter);
	    	int size = q.executeUpdate();
	    	em.getTransaction().commit();
		} catch(PersistenceException e) {
			e.printStackTrace(); // jammer dan...
		}
	}
    
    protected void setAutoIncrement_POSTGRES(String table, String id, long value, EntityManager em) {
    	String alter = "ALTER SEQUENCE " + table + "_" + id + "_seq RESTART WITH  " + value;
    	updateNative(alter, em);
    	
    }
    
    protected void setNativeID(String table, long before, long after, String field, EntityManager em) {
    	String update = "UPDATE " + table + " SET " + field + "=" + after + " where " + field + " =" + before;
    	em.getTransaction().begin();
    	Query q = em.createNativeQuery(update);
        int size = q.executeUpdate();
        em.getTransaction().commit();
    }
    
    static String tableOf(Class<?> clazz) {
    	Table t;
    	t = clazz.getAnnotation(Table.class);
    	return t.name();
    }

    static String idOf(Class<?> clazz) {
    	Field[] fields = clazz.getDeclaredFields();
    	for (Field f : fields) {
    		Id i = f.getAnnotation(Id.class);
    		if (i != null) {
    			return f.getName();
    		}
    	}
    	throw new IllegalArgumentException(clazz + " @Id not found");
    }
    
    static String nativeIdOf(Class<?> clazz) {
    	Field[] fields = clazz.getDeclaredFields();
    	for (Field f : fields) {
    		Id i = f.getAnnotation(Id.class);
    		if (i != null) {
    			Column c = f.getAnnotation(Column.class);
    			if (c != null) return c.name();
    			return f.getName();
    		}
    	}
    	throw new IllegalArgumentException(clazz + " @Id not found");
    }
    
    
    
	public <T> void update(List<T> list, Class<T> clazz, Function<T, Long> getID, BiConsumer<T, T> updateOptlock) {
		String table = tableOf(clazz);
		EntityManager em = getEntityManager();
		for (T item: list) {
			Long id = getID.apply(item);
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			T ref = em.find(clazz, id);
			if (ref != null) {
				updateOptlock.accept(item, ref);
				item = em.merge(item);
			} else {
				do {
					em.persist(item);
 try {
					transaction.commit();
					if (! id.equals(getID.apply(item)) ) {
						em.detach(item);
						//PersistentCourse c = (PersistentCourse )item;
						setNativeID(table, 
								getID.apply(item), id.longValue(), nativeIdOf(clazz), em);
						//c.setCourseID(id);
						//item = (T) c;
						transaction.begin();
						break;
					}
 } catch(RollbackException oops) {
	 				//System.err.println(oops);  // duplicate key, autoincrement wrong.
	 				setAutoIncrement(table, nativeIdOf(clazz), id.longValue(), em);
	 				transaction.begin();
	 				em.persist(item);
	 				transaction.commit();
 }
					
					transaction.begin();
				} while (! id.equals(getID.apply(item)));
			}
			transaction.commit();
		}
		em.close();
	}

	public void setAutoIncrement(Class<?> class1, long longValue) {
		setAutoIncrement(tableOf(class1), nativeIdOf(class1), longValue);		
	}

	public Long getMaxID(Class<?> class1) {
		String table = class1.getSimpleName();
		String id = idOf(class1);
		EntityManager em = getEntityManager();
		try {
			TypedQuery<Long> q = em.createQuery("SELECT MAX(p."+id+") FROM " + table + " p", Long.class);
			return q.getSingleResult();
		} finally {
			em.close();
		}
	}
	public <T, K> void updateUnOrdered(List<T> list, Class<T> class1, Function<T, K> getID, BiConsumer<T, T> updateOptlock) {
		EntityManager em = getEntityManager();
		for (T item: list) {
			K id = getID.apply(item);
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			T ref = em.find(class1, id);
			if (ref != null) {
				updateOptlock.accept(item, ref);
				item = em.merge(item);
			} else {
					em.persist(item);
			}
			transaction.commit();
		}
		em.close();
	}
    
}
