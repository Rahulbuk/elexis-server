package info.elexis.server.core.connector.elexis.services;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.elexis.server.core.connector.elexis.internal.ElexisEntityManager;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.Config;

public class ConfigService {

	private static Logger log = LoggerFactory.getLogger(ConfigService.class);

	public static ConfigService INSTANCE = InstanceHolder.INSTANCE;

	protected EntityManager em;

	private static final class InstanceHolder {
		static final ConfigService INSTANCE = new ConfigService();
	}

	private ConfigService() {
		em = ElexisEntityManager.em();
	}

	/**
	 * Find an object by its primary id.
	 * 
	 * @param id
	 * @param entityClass
	 * @return
	 */
	public Config findById(Object id) {
		return em.find(Config.class, id);
	}

	/**
	 * 
	 * @param param
	 * @param performCommit
	 * @return
	 */
	public Config create(String param, final boolean performCommit) {
		Config obj = new Config();
		if (performCommit)
			em.getTransaction().begin();
		if (param != null) {
			obj.setParam(param);
		}
		em.persist(obj);
		if (performCommit)
			em.getTransaction().commit();
		return obj;
	}
	
	/**
	 * Create a transaction and flush all open changes onto the database
	 */
	public void flush() {
		em.getTransaction().begin();
		em.flush();
		em.getTransaction().commit();
	}
}