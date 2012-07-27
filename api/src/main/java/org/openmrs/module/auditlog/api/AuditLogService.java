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

import org.openmrs.Concept;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.MonitoredObject;

/**
 * Contains service methods related to {@link AuditLog}s
 */
public interface AuditLogService extends OpenmrsService {
	
	/**
	 * Fetches the audit log entries matching the specified arguments
	 * 
	 * @param clazz the class type to match against e.g for objects of type {@link Concept}
	 * @param actions the list of {@link Action}s to match against
	 * @param startDate the creation date of the log entries to return should be after or equal to
	 *            this date
	 * @param endDate the creation date of the log entries to return should be before or equal to
	 *            this date
	 * @param start index to start with (defaults to 0 if <code>null<code>)
	 * @param length number of results to return (default to return all matching results if
	 *            <code>null<code>)
	 * @return a list of matching {@link AuditLog}s
	 * @should return all audit logs in the database if all args are null
	 * @should match on the specified audit log action
	 */
	public List<AuditLog> getAuditLogs(Class<?> clazz, List<Action> actions, Date startDate, Date endDate, Integer start,
	                                   Integer length);
	
	/**
	 * Marks the specified class and subclasses as monitored by creating corresponding
	 * {@link MonitoredObject}s and saving them to the database
	 * 
	 * @param clazz the class to mark as monitored
	 * @param subclassesToInclude list of subclasses to mark as monitored objects
	 * @return the list of saved monitored objects
	 * @should save the monitored object to the db
	 * @should return a list of all saved monitored objects
	 */
	public <C extends OpenmrsObject> List<MonitoredObject> markAsMonitoredObjects(Class<C> clazz,
	                                                                              List<Class<? extends C>> subclassesToInclude);
	
	/**
	 * Fetches all the monitored objects
	 * 
	 * @return a list of all monitored objects
	 * @should get all the monitored objects in the db
	 */
	public List<MonitoredObject> getAllMonitoredObjects();
	
	/**
	 * Deletes a monitored objects
	 * 
	 * @param monitoredObject the object to delete
	 * @should delete the monitored object from the db
	 */
	public void purgeMonitoredObject(MonitoredObject monitoredObject);
	
	/**
	 * Fetches a saved object with the specified objectId
	 * 
	 * @param objectId the id to match against
	 * @return the matching saved object
	 * @should get the saved object matching the specified arguments
	 */
	public <T> T get(Class<T> clazz, Integer objectId);
}