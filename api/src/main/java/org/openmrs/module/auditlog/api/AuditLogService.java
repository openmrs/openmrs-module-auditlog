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
import org.openmrs.module.auditlog.MonitoringStrategy;
import org.openmrs.module.auditlog.util.AuditLogConstants;

/**
 * Contains service methods related to {@link AuditLog}s
 */
public interface AuditLogService extends OpenmrsService {
	
	/**
	 * Checks if the specified type is monitored
	 * 
	 * @param clazz the class to check
	 * @return true if the object is a monitored one otherwise false
	 * @should true if the class is monitored
	 * @should false if the class is not monitored
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public boolean isMonitored(Class<? extends OpenmrsObject> clazz);
	
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
	 * Convenience method that marks a given object type as monitored
	 * 
	 * @param clazz the type to start monitoring
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void startMonitoring(Class<? extends OpenmrsObject> clazz);
	
	/**
	 * Marks the specified classes as monitored by adding their class names to the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * 
	 * @param clazzes the classes to monitor
	 * @should update the monitored class names global property if the strategy is none_except
	 * @should not update any global property if the strategy is all
	 * @should not update any global property if the strategy is none
	 * @should update the un monitored class names global property if the strategy is all_except
	 * @should mark a class and its known subclasses as monitored
	 * @should mark a class and its known subclasses as monitored for all_except strategy
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void startMonitoring(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * Convenience method that marks a given object type as un monitored
	 * 
	 * @param clazz the type to stop monitoring
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void stopMonitoring(Class<? extends OpenmrsObject> clazz);
	
	/**
	 * Marks the specified classes as not monitored by removing their class names from the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * 
	 * @param clazzes the class to stop monitoring
	 * @should update the monitored class names global property if the strategy is none_except
	 * @should not update any global property if the strategy is all
	 * @should not update any global property if the strategy is none
	 * @should update the un monitored class names global property if the strategy is all_except
	 * @should mark a class and its known subclasses as un monitored
	 * @should mark a class and its known subclasses as un monitored for all_except strategy
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public void stopMonitoring(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * Gets the {@link MonitoringStrategy} which is the value of the
	 * {@link AuditLogConstants#GP_MONITORING_STRATEGY} global property
	 * 
	 * @return the monitoringStrategy
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public MonitoringStrategy getMonitoringStrategy();
	
	/**
	 * Convenience method that returns a set of monitored classes as specified by the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * 
	 * @return a set of monitored classes
	 * @should return a set of monitored classes
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public Set<Class<? extends OpenmrsObject>> getMonitoredClasses();
	
	/**
	 * Convenience method that returns a set of un monitored classes as specified by the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_UN_MONITORED_CLASSES}
	 * 
	 * @return a set of monitored classes
	 * @should return a set of un monitored classes
	 */
	@Authorized(AuditLogConstants.PRIV_MANAGE_AUDITLOG)
	public Set<Class<? extends OpenmrsObject>> getUnMonitoredClasses();
	
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
