/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.auditlog.api.db.hibernate;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.springframework.transaction.annotation.Transactional;

public class HibernateAuditLogDAO implements AuditLogDAO {
	
	//private static final Log log = LogFactory.getLog(HibernateAuditLogDAO.class);
	
	private SessionFactory sessionFactory;
	
	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.db.AuditLogDAO#getAuditLogs(List, List, java.util.Date,
	 *      java.util.Date, java.lang.Integer, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> getAuditLogs(List<String> classnames, List<Action> actions, Date startDate, Date endDate,
	                                   Integer start, Integer length) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(AuditLog.class);
		if (classnames != null)
			criteria.add(Restrictions.in("className", classnames));
		
		if (actions != null)
			criteria.add(Restrictions.in("action", actions));
		
		if (startDate != null)
			criteria.add(Restrictions.ge("dateCreated", startDate));
		
		if (endDate != null)
			criteria.add(Restrictions.le("dateCreated", endDate));
		
		if (start != null)
			criteria.setFirstResult(start);
		
		if (length != null && length > 0)
			criteria.setMaxResults(length);
		
		//Show the latest logs first
		criteria.addOrder(Order.desc("dateCreated"));
		
		return criteria.list();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.db.AuditLogDAO#save(Object)
	 */
	@Override
	@Transactional
	public <T> T save(T object) {
		sessionFactory.getCurrentSession().save(object);
		return object;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.db.AuditLogDAO#delete(Object)
	 */
	@Override
	@Transactional
	public void delete(Object object) {
		sessionFactory.getCurrentSession().delete(object);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.db.AuditLogDAO#getObjectById(java.lang.Class,
	 *      java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true)
	public <T> T getObjectById(Class<T> clazz, Integer id) {
		return (T) sessionFactory.getCurrentSession().get(clazz, id);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getObjectByUuid(java.lang.Class,
	 *      java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObjectByUuid(Class<T> clazz, String uuid) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(clazz);
		criteria.add(Restrictions.eq("uuid", uuid));
		return (T) criteria.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getPersistentConcreteSubclasses(java.lang.Class)
	 */
	@Override
	public Set<Class<?>> getPersistentConcreteSubclasses(Class<?> clazz) {
		return getPersistentConcreteSubclasseInternal(clazz, null, null);
	}
	
	/**
	 * Gets a set of concrete subclasses for the specified class recursively, note that interfaces
	 * and abstract classes are excluded
	 * 
	 * @param clazz
	 * @param foundSubclasses the list of subclasses found in previous recursive calls, should be
	 *            null for the first call
	 * @param mappedClasses
	 * @return a set of subclasses
	 * @should return a list of subclasses for the specified type
	 * @should exclude interfaces and abstract classes
	 */
	@SuppressWarnings("unchecked")
	private Set<Class<?>> getPersistentConcreteSubclasseInternal(Class<?> clazz, Set<Class<?>> foundSubclasses,
	                                                             Collection<ClassMetadata> mappedClasses) {
		if (foundSubclasses == null)
			foundSubclasses = new HashSet<Class<?>>();
		if (mappedClasses == null)
			mappedClasses = sessionFactory.getAllClassMetadata().values();
		
		if (clazz != null) {
			for (ClassMetadata cmd : mappedClasses) {
				Class<?> possibleSubclass = cmd.getMappedClass(EntityMode.POJO);
				if (!clazz.equals(possibleSubclass) && clazz.isAssignableFrom(possibleSubclass)) {
					if (!Modifier.isAbstract(possibleSubclass.getModifiers()) && !possibleSubclass.isInterface())
						foundSubclasses.add(possibleSubclass);
					foundSubclasses.addAll(getPersistentConcreteSubclasseInternal(possibleSubclass, foundSubclasses,
					    mappedClasses));
				}
			}
		}
		
		return foundSubclasses;
	}
}
