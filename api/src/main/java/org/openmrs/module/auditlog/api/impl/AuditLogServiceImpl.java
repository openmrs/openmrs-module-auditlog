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

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;

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
	 * @see org.openmrs.module.auditlog.AuditLogService#getAuditLogs(List, List, java.util.Date,
	 *      java.util.Date, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public List<AuditLog> getAuditLogs(List<Class<OpenmrsObject>> clazzes, List<Action> actions, Date startDate,
	                                   Date endDate, Integer start, Integer length) {
		return dao.getAuditLogs(clazzes, actions, startDate, endDate, start, length);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#get(java.lang.Class, java.lang.Integer)
	 */
	@Override
	public <T> T get(Class<T> clazz, Integer objectId) {
		return dao.get(clazz, objectId);
	}
}
