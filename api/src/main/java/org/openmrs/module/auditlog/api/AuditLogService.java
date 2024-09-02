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
package org.openmrs.module.auditlog.api;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.strategy.AuditStrategy;
import org.openmrs.module.auditlog.util.AuditLogConstants;

/**
 * Contains service methods related to {@link AuditLog}s
 */
public interface AuditLogService extends OpenmrsService {
	
	/**
	 * Checks if the specified type is audited
	 * 
	 * @param clazz the class to check
	 * @return true if the object is an audited one otherwise false
     * @should return false for core exceptions
	 */
	@Authorized(AuditLogConstants.CHECK_FOR_AUDITED_ITEMS)
	public boolean isAudited(Class<?> clazz);
	
	/**
	 * Fetches the audit log entries matching the specified arguments
	 * 
	 * @param clazzes the class type to match against e.g for objects of type {@link Concept}
	 * @param actions the list of {@link Action}s to match against
	 * @param startDate the creation date of the log entries to return should be after or equal to
	 *            this date
	 * @param endDate the creation date of the log entries to return should be before or equal to
	 *            this date
	 * @param excludeChildAuditLogs specifies if AuditLogs for collection items should excluded or
	 *            not
	 * @param start index to start with (defaults to 0 if <code>null<code>)
	 * @param length number of results to return (default to return all matching results if
	 *            <code>null<code>)
	 * @return a list of matching {@link AuditLog}s
	 * @should return all audit logs in the database if all args are null
	 * @should match on the specified audit log actions
	 * @should match on the specified classes
	 * @should return logs created on or after the specified startDate
	 * @should return logs created on or before the specified endDate
	 * @should return logs created within the specified start and end dates
	 * @should reject a start date that is in the future
	 * @should ignore end date it it is in the future
	 * @should sort the logs by date of creation starting with the latest
	 * @should include logs for subclasses when getting logs by type
	 * @should exclude child logs if excludeChildAuditLogsis set to true
	 */
	@Authorized(AuditLogConstants.PRIV_GET_AUDITLOGS)
	public List<AuditLog> getAuditLogs(List<Class<?>> clazzes, List<Action> actions, Date startDate, Date endDate,
	                                   boolean excludeChildAuditLogs, Integer start, Integer length);
	
	/**
	 * Fetches a saved object with the specified objectId
	 * 
	 * @param id the id to match against
	 * @return the matching saved object
	 * @should get the saved object matching the specified arguments
	 */
	@Authorized(AuditLogConstants.PRIV_GET_ITEMS)
	public <T> T getObjectById(Class<T> clazz, Serializable id);
	
	/**
	 * Fetches a saved object with the specified uuid
	 * 
	 * @param uuid the uuid to match against
	 * @return the matching saved object
	 * @should get the saved object matching the specified arguments
	 */
	@Authorized(AuditLogConstants.PRIV_GET_ITEMS)
	public <T> T getObjectByUuid(Class<T> clazz, String uuid);
	
	/**
	 * Gets the {@link org.openmrs.module.auditlog.strategy.AuditStrategy} which is the value of the
	 * {@link org.openmrs.module.auditlog.strategy.ExceptionBasedAuditStrategy#GLOBAL_PROPERTY_EXCEPTION}
	 * global property
	 * 
	 * @return the auditingStrategy
	 */
	@Authorized(AuditLogConstants.PRIV_GET_AUDIT_STRATEGY)
	public AuditStrategy getAuditingStrategy();
	
	/**
	 * Gets all audit logs for the object that matches the specified uuid and class that match the
	 * other specified arguments
	 * 
	 * @param id
	 * @param clazz the Class of the object to match against
	 * @param actions the actions to match against
	 * @param startDate the start date to match against
	 * @param endDate the end date to match against
	 * @param excludeChildAuditLogs specifies if AuditLogs for collection items should excluded or
	 *            not
	 * @return a list of audit logs
	 * @should get all logs for the object matching the specified uuid
	 * @should include logs for subclasses when getting by type
	 * @should exclude child logs for object if excludeChildAuditLogs is set to true
	 */
	@Authorized(AuditLogConstants.PRIV_GET_AUDITLOGS)
	public List<AuditLog> getAuditLogs(Serializable id, Class<?> clazz, List<Action> actions, Date startDate, Date endDate,
	                                   boolean excludeChildAuditLogs);

	/**
	 * Gets all audit logs for the object that matches the specified uuid and class that match the
	 * other specified arguments
	 *
	 * @param ids The list of object ids
	 * @param type the list of Class of the object to match against
	 * @param actions the actions to match against
	 * @param startDate the start date to match against
	 * @param endDate the end date to match against
	 * @param excludeChildAuditLogs specifies if AuditLogs for collection items should excluded or
	 *            not
	 * @return a list of audit logs
	 * @should get all logs for the object matching the specified uuid
	 * @should include logs for subclasses when getting by type
	 * @should exclude child logs for object if excludeChildAuditLogs is set to true
	 */
	@Authorized(AuditLogConstants.PRIV_GET_AUDITLOGS)
	public List<AuditLog> getAuditLogsWithIds(List<String> ids, Class<?> type, List<Action> actions, Date startDate,
									   Date endDate, boolean excludeChildAuditLogs, Integer start, Integer length);
	
	/**
	 * Gets all audit logs for the object that match the other specified arguments
	 * 
	 * @param object the uuid of the object to match against
	 * @param actions the actions to match against
	 * @param startDate the start date to match against
	 * @param endDate the end date to match against
	 * @param excludeChildAuditLogs specifies if AuditLogs for collection items should excluded or
	 *            not
	 * @return a list of audit logs
	 * @should get all logs for the specified object
	 */
	@Authorized(AuditLogConstants.PRIV_GET_AUDITLOGS)
	public List<AuditLog> getAuditLogs(Object object, List<Action> actions, Date startDate, Date endDate,
	                                   boolean excludeChildAuditLogs);
}
