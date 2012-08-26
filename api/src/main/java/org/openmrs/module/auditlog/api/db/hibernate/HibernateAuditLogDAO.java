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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.OpenmrsObject;
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
	public List<AuditLog> getAuditLogs(List<Class<? extends OpenmrsObject>> clazzes, List<Action> actions, Date startDate,
	                                   Date endDate, Integer start, Integer length) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(AuditLog.class);
		if (CollectionUtils.isNotEmpty(clazzes)) {
			List<String> classNames = new ArrayList<String>();
			for (Class<? extends OpenmrsObject> clazz : clazzes) {
				classNames.add(clazz.getName());
			}
			criteria.add(Restrictions.in("className", classNames));
		}
		
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
	 * @see org.openmrs.module.auditlog.db.AuditLogDAO#get(java.lang.Class, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true)
	public <T> T get(Class<T> clazz, Integer objectId) {
		return (T) sessionFactory.getCurrentSession().get(clazz, objectId);
	}
}
