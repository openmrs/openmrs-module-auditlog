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

public class AuditLogServiceImpl extends BaseOpenmrsService implements AuditLogService {
	
	//private static final Log log = LogFactory.getLog(AuditLogServiceImpl.class);
	
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
	 */
	public boolean isMonitored(Class<?> clazz) {
		return dao.isMonitored(clazz);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getAuditLogs(java.util.List,
	 *      java.util.List, java.util.Date, java.util.Date, boolean, java.lang.Integer,
	 *      java.lang.Integer)
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public List<AuditLog> getAuditLogs(List<Class<? extends OpenmrsObject>> clazzes, List<Action> actions, Date startDate,
	                                   Date endDate, boolean excludeChildAuditLogs, Integer start, Integer length) {
		if (OpenmrsUtil.compareWithNullAsEarliest(startDate, new Date()) > 0)
			throw new APIException(Context.getMessageSourceService().getMessage(
			    AuditLogConstants.MODULE_ID + ".exception.startDateInFuture"));
		List<String> classesToMatch = null;
		if (clazzes != null) {
			classesToMatch = new ArrayList<String>();
			for (Class clazz : clazzes) {
				if (OpenmrsObject.class.isAssignableFrom(clazz)) {
					classesToMatch.add(clazz.getName());
					for (Class subclass : dao.getPersistentConcreteSubclasses(clazz)) {
						classesToMatch.add(subclass.getName());
					}
				}
			}
		}
		
		return dao.getAuditLogs(null, classesToMatch, actions, startDate, endDate, excludeChildAuditLogs, start, length);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#getObjectById(java.lang.Class,
	 *      java.lang.Integer)
	 */
	@Override
	public <T> T getObjectById(Class<T> clazz, Integer id) {
		return dao.getObjectById(clazz, id);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getObjectByUuid(java.lang.Class,
	 *      java.lang.String)
	 */
	@Override
	public <T> T getObjectByUuid(Class<T> clazz, String uuid) {
		if (StringUtils.isBlank(uuid))
			return null;
		
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
	public MonitoringStrategy getMonitoringStrategy() {
		return dao.getMonitoringStrategy();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getMonitoredClasses()
	 */
	@Override
	public Set<Class<?>> getMonitoredClasses() {
		return dao.getMonitoredClasses();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getUnMonitoredClasses()
	 */
	@Override
	public Set<Class<?>> getUnMonitoredClasses() {
		return dao.getUnMonitoredClasses();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getAuditLogs(String, Class, List, Date,
	 *      Date, boolean)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<AuditLog> getAuditLogs(String uuid, Class<? extends OpenmrsObject> clazz, List<Action> actions,
	                                   Date startDate, Date endDate, boolean excludeChildAuditLogs) {
		
		if (StringUtils.isBlank(uuid) || clazz == null)
			throw new APIException("class and uuid are required");
		
		List<String> clazzes = new ArrayList<String>();
		clazzes.add(clazz.getName());
		for (Class subclass : dao.getPersistentConcreteSubclasses(clazz)) {
			clazzes.add(subclass.getName());
		}
		
		return dao.getAuditLogs(uuid, clazzes, actions, startDate, endDate, excludeChildAuditLogs, null, null);
	}
}
