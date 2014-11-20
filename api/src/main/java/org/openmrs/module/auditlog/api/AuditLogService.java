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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.AuditingStrategy;
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
	 * @should return true if the class is audited for none except strategy
	 * @should return false if the class is not audited for none except strategy
	 * @should return true if the class is audited for all except strategy
	 * @should return false if the class is not audited for all except strategy
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public boolean isAudited(Class<? extends OpenmrsObject> clazz);
	
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
	public List<AuditLog> getAuditLogs(List<Class<? extends OpenmrsObject>> clazzes, List<Action> actions, Date startDate,
	                                   Date endDate, boolean excludeChildAuditLogs, Integer start, Integer length);
	
	/**
	 * Fetches a saved object with the specified objectId
	 * 
	 * @param id the id to match against
	 * @return the matching saved object
	 * @should get the saved object matching the specified arguments
	 */
	@Authorized(AuditLogConstants.PRIV_GET_ITEMS)
	public <T> T getObjectById(Class<T> clazz, Integer id);
	
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
	 * Convenience method that marks a given object type as audited
	 * 
	 * @param clazz the type to start auditing
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void startAuditing(Class<? extends OpenmrsObject> clazz);
	
	/**
	 * Marks the specified classes as audited by adding their class names to the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_EXCEPTIONS}
	 * 
	 * @param clazzes the classes to audit
	 * @should update the exception class names global property if the strategy is none_except
	 * @should fail if the strategy is set to all
	 * @should fail if the strategy is set to none
	 * @should update the exception class names global property if the strategy is all_except
	 * @should mark a class and its known subclasses as audited
	 * @should mark a class and its known subclasses as audited for all_except strategy
	 * @should also mark association types as audited
	 * @should not mark association types for many to many collections as audited
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void startAuditing(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * Convenience method that marks a given object type as un audited
	 * 
	 * @param clazz the type to stop auditing
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void stopAuditing(Class<? extends OpenmrsObject> clazz);
	
	/**
	 * Marks the specified classes as not audited by removing their class names from the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_EXCEPTIONS}
	 * 
	 * @param clazzes the class to stop auditing
	 * @should update the exception class names global property if the strategy is none_except
	 * @should fail if the strategy is set to all
	 * @should fail if the strategy is set to none
	 * @should update the exception class names global property if the strategy is all_except
	 * @should mark a class and its known subclasses as un audited
	 * @should mark a class and its known subclasses as un audited for all_except strategy
	 * @should remove association types from audited classes
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void stopAuditing(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * Gets the {@link org.openmrs.module.auditlog.AuditingStrategy} which is the value of the
	 * {@link AuditLogConstants#GP_AUDITING_STRATEGY} global property
	 * 
	 * @return the auditingStrategy
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public AuditingStrategy getAuditingStrategy();
	
	/**
	 * Convenience method that returns a set of exception classes as specified by the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_EXCEPTIONS}
	 * 
	 * @return a set of audited classes
	 * @should return a set of exception classes
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public Set<Class<? extends OpenmrsObject>> getExceptions();
	
	/**
	 * Gets all audit logs for the object that matches the specified uuid and class that match the
	 * other specified arguments
	 * 
	 * @param uuid the uuid of the object to match against
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
	public List<AuditLog> getAuditLogs(String uuid, Class<? extends OpenmrsObject> clazz, List<Action> actions,
	                                   Date startDate, Date endDate, boolean excludeChildAuditLogs);
	
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
	public List<AuditLog> getAuditLogs(OpenmrsObject object, List<Action> actions, Date startDate, Date endDate,
	                                   boolean excludeChildAuditLogs);
}
