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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.MonitoringStrategy;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AuditLogServiceImpl extends BaseOpenmrsService implements AuditLogService {
	
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
	 * @see AuditLogService#isMonitored(Class)
	 * @param clazz
	 */
	@Transactional(readOnly = true)
	public boolean isMonitored(Class<? extends OpenmrsObject> clazz) {
		return dao.isMonitored(clazz);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getAuditLogs(java.util.List,
	 *      java.util.List, java.util.Date, java.util.Date, boolean, java.lang.Integer,
	 *      java.lang.Integer)
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> getAuditLogs(List<Class<? extends OpenmrsObject>> clazzes, List<Action> actions, Date startDate,
	                                   Date endDate, boolean excludeChildAuditLogs, Integer start, Integer length) {
		if (OpenmrsUtil.compareWithNullAsEarliest(startDate, new Date()) > 0) {
			throw new APIException(Context.getMessageSourceService().getMessage(
			    AuditLogConstants.MODULE_ID + ".exception.startDateInFuture"));
		}
		
		List<Class<? extends OpenmrsObject>> classesToMatch = null;
		if (clazzes != null) {
			classesToMatch = new ArrayList<Class<? extends OpenmrsObject>>();
			for (Class clazz : clazzes) {
				classesToMatch.add(clazz);
				for (Class subclass : dao.getPersistentConcreteSubclasses(clazz)) {
					classesToMatch.add(subclass);
				}
			}
		}
		
		return dao.getAuditLogs(null, classesToMatch, actions, startDate, endDate, excludeChildAuditLogs, start, length);
	}
	
	/**
	 * @see AuditLogService#getObjectById(java.lang.Class, java.lang.Integer)
	 */
	@Override
	@Transactional(readOnly = true)
	public <T> T getObjectById(Class<T> clazz, Integer id) {
		return dao.getObjectById(clazz, id);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getObjectByUuid(java.lang.Class,
	 *      java.lang.String)
	 */
	@Override
	@Transactional(readOnly = true)
	public <T> T getObjectByUuid(Class<T> clazz, String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return null;
		}
		
		return dao.getObjectByUuid(clazz, uuid);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#startMonitoring(java.lang.Class)
	 */
	@Override
	public void startMonitoring(Class<? extends OpenmrsObject> clazz) {
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(clazz);
		startMonitoring(classes);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#startMonitoring(java.util.Set)
	 */
	@Override
	public void startMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		dao.startMonitoring(clazzes);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#stopMonitoring(java.lang.Class)
	 */
	@Override
	public void stopMonitoring(Class<? extends OpenmrsObject> clazz) {
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(clazz);
		stopMonitoring(classes);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#stopMonitoring(java.util.Set)
	 */
	@Override
	public void stopMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		dao.stopMonitoring(clazzes);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getMonitoringStrategy()
	 */
	@Override
	@Transactional(readOnly = true)
	public MonitoringStrategy getMonitoringStrategy() {
		return dao.getMonitoringStrategy();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getMonitoredClasses()
	 */
	@Override
	@Transactional(readOnly = true)
	public Set<Class<? extends OpenmrsObject>> getMonitoredClasses() {
		return dao.getMonitoredClasses();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getUnMonitoredClasses()
	 */
	@Override
	@Transactional(readOnly = true)
	public Set<Class<? extends OpenmrsObject>> getUnMonitoredClasses() {
		return dao.getUnMonitoredClasses();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getAuditLogs(String, Class, List, Date,
	 *      Date, boolean)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> getAuditLogs(String uuid, Class<? extends OpenmrsObject> clazz, List<Action> actions,
	                                   Date startDate, Date endDate, boolean excludeChildAuditLogs) {
		
		if (StringUtils.isBlank(uuid) || clazz == null) {
			throw new APIException("class and uuid are required when fetching AuditLogs for an object");
		}
		
		List<Class<? extends OpenmrsObject>> clazzes = new ArrayList<Class<? extends OpenmrsObject>>();
		clazzes.add(clazz);
		for (Class subclass : dao.getPersistentConcreteSubclasses(clazz)) {
			clazzes.add(subclass);
		}
		
		return dao.getAuditLogs(uuid, clazzes, actions, startDate, endDate, excludeChildAuditLogs, null, null);
	}
	
	/**
	 * @see AuditLogService#getAuditLogs(org.openmrs.OpenmrsObject, java.util.List, java.util.Date,
	 *      java.util.Date, boolean)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<AuditLog> getAuditLogs(OpenmrsObject object, List<Action> actions, Date startDate, Date endDate,
	                                   boolean excludeChildAuditLogs) {
		return getAuditLogs(object.getUuid(), object.getClass(), actions, startDate, endDate, excludeChildAuditLogs);
	}
}
