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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;

public class HibernateAuditLogDAO implements AuditLogDAO, GlobalPropertyListener {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private static Boolean storeLastStateOfDeletedItemsCache;
	
	private SessionFactory sessionFactory;
	
	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see AuditLogDAO#getAuditLogs(java.io.Serializable, java.util.List, java.util.List,
	 *      java.util.Date, java.util.Date, boolean, Integer, Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<AuditLog> getAuditLogs(Serializable id, List<Class<?>> types, List<Action> actions, Date startDate,
	                                   Date endDate, boolean excludeChildAuditLogs, Integer start, Integer length) {
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(AuditLog.class);
		if (id != null) {
			criteria.add(Restrictions.eq("identifier", AuditLogUtil.serializeObject(id)));
		}
		
		if (types != null) {
			criteria.add(Restrictions.in("type", types));
		}
		if (actions != null) {
			criteria.add(Restrictions.in("action", actions));
		}
		if (excludeChildAuditLogs) {
			criteria.add(Restrictions.isNull("parentAuditLog"));
		}
		if (startDate != null) {
			criteria.add(Restrictions.ge("dateCreated", startDate));
		}
		if (endDate != null) {
			criteria.add(Restrictions.le("dateCreated", endDate));
		}
		if (start != null) {
			criteria.setFirstResult(start);
		}
		if (length != null && length > 0) {
			criteria.setMaxResults(length);
		}
		
		//Show the latest logs first
		criteria.addOrder(Order.desc("dateCreated"));
		
		return criteria.list();
	}
	
	/**
	 * @see AuditLogDAO#save(Object)
	 */
	@Override
	public <T> T save(T object) {
		if (object instanceof AuditLog) {
			AuditLog auditLog = (AuditLog) object;
			//Hibernate has issues with saving the parentAuditLog field if the parent isn't yet saved
			//so we need to first save the parent before its children
			if (auditLog.getParentAuditLog() != null && auditLog.getParentAuditLog().getAuditLogId() == null) {
				save(auditLog.getParentAuditLog());
			}
		}
		
		sessionFactory.getCurrentSession().saveOrUpdate(object);
		return object;
	}
	
	/**
	 * @see AuditLogDAO#delete(Object)
	 */
	@Override
	public void delete(Object object) {
		sessionFactory.getCurrentSession().delete(object);
	}
	
	/**
	 * @see AuditLogDAO#getObjectById(Class, java.io.Serializable)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObjectById(Class<T> clazz, Serializable id) {
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
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#storeLastStateOfDeletedItems()
	 * @return
	 */
	public boolean storeLastStateOfDeletedItems() {
		if (storeLastStateOfDeletedItemsCache == null) {
			String gpValue = Context.getAdministrationService().getGlobalProperty(
			    AuditLogConstants.GP_STORE_LAST_STATE_OF_DELETED_ITEMS);
			storeLastStateOfDeletedItemsCache = Boolean.valueOf(gpValue);
		}
		return storeLastStateOfDeletedItemsCache;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getId(Object)
	 * @return
	 */
	@Override
	public Serializable getId(Object object) {
		return sessionFactory.getClassMetadata(object.getClass()).getIdentifier(object, EntityMode.POJO);
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyChanged(org.openmrs.GlobalProperty)
	 */
	@Override
	public void globalPropertyChanged(GlobalProperty gp) {
		if (AuditLogConstants.GP_STORE_LAST_STATE_OF_DELETED_ITEMS.equals(gp.getProperty())) {
			storeLastStateOfDeletedItemsCache = null;
		}
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyDeleted(java.lang.String)
	 */
	@Override
	public void globalPropertyDeleted(String gpName) {
		if (AuditLogConstants.GP_STORE_LAST_STATE_OF_DELETED_ITEMS.equals(gpName)) {
			storeLastStateOfDeletedItemsCache = null;
		}
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#supportsPropertyName(java.lang.String)
	 */
	@Override
	public boolean supportsPropertyName(String gpName) {
		return AuditLogConstants.GP_STORE_LAST_STATE_OF_DELETED_ITEMS.equals(gpName);
	}
}
