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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;

/**
 * Database access methods for {@link AuditLog}s
 */
public interface AuditLogDAO {
	
	/**
	 * Fetches the audit log entries matching the specified arguments
	 * 
	 * @param id
	 * @param types the class names to match against e.g for objects of type
	 *            {@link org.openmrs.Concept}
	 * @param actions the list of {@link org.openmrs.module.auditlog.AuditLog.Action}s to match
	 *            against
	 * @param startDate the creation date of the log entries to return should be after or equal to
	 *            this date
	 * @param endDate the creation date of the log entries to return should be before or equal to
	 *            this date
	 * @param excludeChildAuditLogs specifies if AuditLogs for collection items should excluded or
	 *            not
	 * @param start index to start with (defaults to 0 if <code>null<code>)
	 * @param length number of results to return (default to return all matching results if
	 *            <code>null<code>)
	 * @return list of auditlogs
	 */
	public List<AuditLog> getAuditLogs(Serializable id, List<Class<?>> types, List<Action> actions, Date startDate,
	                                   Date endDate, boolean excludeChildAuditLogs, Integer start, Integer length);

	/**
	 * Fetches the audit log entries matching the specified arguments
	 *
	 * @param ids the list of Ids
	 * @param type the class name to match against e.g for object of type
	 *            {@link org.openmrs.Concept}
	 * @param actions the list of {@link org.openmrs.module.auditlog.AuditLog.Action}s to match
	 *            against
	 * @param startDate the creation date of the log entries to return should be after or equal to
	 *            this date
	 * @param endDate the creation date of the log entries to return should be before or equal to
	 *            this date
	 * @param excludeChildAuditLogs specifies if AuditLogs for collection items should excluded or
	 *            not
	 * @param start index to start with (defaults to 0 if <code>null<code>)
	 * @param length number of results to return (default to return all matching results if
	 *            <code>null<code>)
	 * @return list of auditlogs
	 */
	public List<AuditLog> getAuditLogsWithIds(List<String> ids, Class<?> type, List<Action> actions, Date startDate,
									   Date endDate, boolean excludeChildAuditLogs, Integer start, Integer length);
	
	/**
	 * Saves the specified object to the database
	 * 
	 * @param object the object to save
	 * @return the saved audit log
	 */
	public <T> T save(T object);
	
	/**
	 * @see AuditLogService
	 */
	public void delete(Object object);
	
	/**
	 * @see AuditLogService#getObjectById(Class, java.io.Serializable)
	 */
	public <T> T getObjectById(Class<T> clazz, Serializable id);
	
	/**
	 * @see AuditLogService#getObjectByUuid(Class, String)
	 */
	public <T> T getObjectByUuid(Class<T> clazz, String uuid);
	
	/**
	 * Returns true or false depending on the value of the
	 * AuditLogConstants#GP_STORE_LAST_STATE_OF_DELETED_ITEMS global property
	 * 
	 * @return true is allowed otherwise false
	 */
	public boolean storeLastStateOfDeletedItems();
	
	/**
	 * Returns unique database identifier for the specified persistent object
	 * 
	 * @return the unique identifier
	 * @should return the database id of the object
	 */
	public Serializable getId(Object object);
}
