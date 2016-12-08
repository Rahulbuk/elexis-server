package info.elexis.server.core.connector.elexis.services;

import static info.elexis.server.core.connector.elexis.internal.ElexisEntityManager.*;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import info.elexis.server.core.connector.elexis.jpa.model.annotated.AbstractDBObject;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.AbstractDBObjectIdDeleted;
import info.elexis.server.core.connector.elexis.jpa.model.annotated.AbstractDBObjectIdDeleted_;

/**
 * This class tries to resemble the Query class known by Elexis user by
 * employing JPA CriteriaQueries.
 * 
 * @param <T>
 * @Deprecated please use {@link JPAQuery#count()}
 */
public class JPACountQuery<T extends AbstractDBObject> {

	public static enum QUERY {
		LIKE, EQUALS, LESS_OR_EQUAL, NOT_EQUALS, NOT_LIKE
	};

	private Class<T> clazz;

	private CriteriaBuilder cb;
	private CriteriaQuery<Long> query;
	private Root<T> root;
	private TypedQuery<Long> tq;

	private boolean includeDeleted;

	private List<Predicate> conditions = new LinkedList<Predicate>();

	public JPACountQuery(Class<T> clazz) {
		this(clazz, false);
	}

	public JPACountQuery(Class<T> clazz, boolean includeDeleted) {
		this.clazz = clazz;
		this.includeDeleted = includeDeleted;

		cb = createEntityManager().getCriteriaBuilder();
		query = cb.createQuery(Long.class);
		root = query.from(clazz);
		query.select(cb.count(root));
		tq = null;
	}

	@SuppressWarnings("unchecked")
	public void add(@SuppressWarnings("rawtypes") SingularAttribute attribute, QUERY qt, String string) {
		Predicate pred;

		switch (qt) {
		case LIKE:
			pred = cb.like(root.get(attribute), string);
			break;
		case EQUALS:
			pred = cb.equal(root.get(attribute), string);
			break;
		case NOT_EQUALS:
			pred = cb.not(cb.equal(root.get(attribute), string));
			break;
		case NOT_LIKE:
			pred = cb.not(cb.like(root.get(attribute), string));
			break;
		case LESS_OR_EQUAL:
			Path<Integer> path = root.get(attribute);
			pred = cb.le(path, Integer.parseInt(string));
			break;
		default:
			throw new IllegalArgumentException();
		}

		conditions.add(pred);
	}
	
	public void add(@SuppressWarnings("rawtypes") SingularAttribute attribute, QUERY qt, boolean value) {
		conditions.add(cb.equal(root.get(attribute.getName()), Boolean.valueOf(value)));
	}
	

	public long count() {
		if (!includeDeleted) {
			if (AbstractDBObjectIdDeleted.class.isAssignableFrom(clazz)) {
				conditions.add(cb.equal(root.get(AbstractDBObjectIdDeleted_.deleted.getName()), Boolean.FALSE));
			}
		}
		if (conditions.size() > 0) {
			query = query.where(conditions.toArray(new Predicate[0]));
		}
		tq = createEntityManager().createQuery(query);
		return tq.getSingleResult();
	}
}
