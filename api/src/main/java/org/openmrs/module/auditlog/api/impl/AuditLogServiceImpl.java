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
package org.openmrs.module.auditlog.api.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.MonitoredObject;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogUtil;

public class AuditLogServiceImpl extends BaseOpenmrsService implements AuditLogService {
	
	private static final Log log = LogFactory.getLog(AuditLogServiceImpl.class);
	
	private AuditLogDAO dao;
	
	/**
	 * @param dao the dao to set
	 */
	public void setDao(AuditLogDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * setter for AuditLogServiceDAO
	 */
	public void setAuditLogServiceDAO(AuditLogDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#getAuditLogs(java.lang.Class, List,
	 *      java.util.Date, java.util.Date, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public List<AuditLog> getAuditLogs(Class<?> clazz, List<Action> actions, Date startDate, Date endDate, Integer start,
	                                   Integer length) {
		return dao.getAuditLogs(clazz, actions, startDate, endDate, start, length);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#createMonitoredObjects(Class<C>,
	 *      List<Class<? extends C>>)
	 */
	@Override
	public <C extends OpenmrsObject> List<MonitoredObject> markAsMonitoredObjects(Class<C> clazz,
	                                                                              List<Class<? extends C>> subclassesToInclude) {
		String userDetails = AuditLogUtil.getUserDetails(Context.getAuthenticatedUser());
		Date dateCreated = new Date();
		List<MonitoredObject> savedMonitoredObjects = new ArrayList<MonitoredObject>();
		
		MonitoredObject monitoredObject = new MonitoredObject(clazz.getName());
		monitoredObject.setUuid(UUID.randomUUID().toString());
		monitoredObject.setCreatorDetails(userDetails);
		monitoredObject.setDateCreated(dateCreated);
		savedMonitoredObjects.add(dao.save(monitoredObject));
		
		//mark the subclasses as monitored if any
		if (subclassesToInclude != null) {
			for (Class<?> subclass : subclassesToInclude) {
				MonitoredObject subMonitoredObject = new MonitoredObject(subclass.getName());
				subMonitoredObject.setUuid(UUID.randomUUID().toString());
				subMonitoredObject.setCreatorDetails(userDetails);
				subMonitoredObject.setDateCreated(dateCreated);
				savedMonitoredObjects.add(dao.save(subMonitoredObject));
			}
			
		}
		
		return savedMonitoredObjects;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#getAllMonitoredObjects()
	 */
	@Override
	public List<MonitoredObject> getAllMonitoredObjects() {
		return dao.getAllMonitoredObjects();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#purgeMonitoredObject(org.openmrs.module.auditlog.MonitoredObject)
	 */
	@Override
	public void purgeMonitoredObject(MonitoredObject monitoredObject) {
		dao.delete(monitoredObject);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#get(java.lang.Class, java.lang.Integer)
	 */
	@Override
	public <T> T get(Class<T> clazz, Integer objectId) {
		return dao.get(clazz, objectId);
	}
}
