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

package org.openmrs.module.auditlog.api.db;

import java.util.Date;
import java.util.List;

import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;

/**
 * Database access methods for {@link AuditLog}s
 */
public interface AuditLogDAO {
	
	/**
	 * @see AuditLogService#getAuditLogs(List, List, Date, Date, Integer, Integer)
	 */
	public List<AuditLog> getAuditLogs(List<Class<OpenmrsObject>> clazzes, List<Action> actions, Date startDate,
	                                   Date endDate, Integer start, Integer length);
	
	/**
	 * Saves the specified object to the database
	 * 
	 * @param persistentObject the object to save
	 * @return the saved audit log
	 */
	public <T> T save(T object);
	
	/**
	 * @see AuditLogService
	 */
	public void delete(Object object);
	
	/**
	 * @see AuditLogService#get(Class, Integer)
	 */
	public <T> T get(Class<T> clazz, Integer objectId);
}
