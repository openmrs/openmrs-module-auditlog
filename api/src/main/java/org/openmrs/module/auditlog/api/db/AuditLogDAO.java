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
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.MonitoringStrategy;
import org.openmrs.module.auditlog.api.AuditLogService;

/**
 * Database access methods for {@link AuditLog}s
 */
public interface AuditLogDAO {
	
	/**
	 * Fetches the audit log entries matching the specified arguments
	 * 
	 * @param uuid the uuid to match against
	 * @param classnames the class names to match against e.g for objects of type {@link Concept}
	 * @param actions the list of {@link Action}s to match against
	 * @param startDate the creation date of the log entries to return should be after or equal to
	 *            this date
	 * @param endDate the creation date of the log entries to return should be before or equal to
	 *            this date
	 * @param start index to start with (defaults to 0 if <code>null<code>)
	 * @param length number of results to return (default to return all matching results if
	 *            <code>null<code>)
	 * @return list of auditlogs
	 */
	public List<AuditLog> getAuditLogs(String uuid, List<String> classnames, List<Action> actions, Date startDate,
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
	 * @see AuditLogService#getObjectById(Class, Integer)
	 */
	public <T> T getObjectById(Class<T> clazz, Integer id);
	
	/**
	 * @see AuditLogService#getObjectByUuid(Class, String)
	 */
	public <T> T getObjectByUuid(Class<T> clazz, String uuid);
	
	/**
	 * Marks the specified classes as monitored
	 * 
	 * @param clazzes
	 */
	public void startMonitoring(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * Un marks the specified classes as monitored
	 * 
	 * @param clazzes
	 */
	public void stopMonitoring(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * @return
	 */
	public MonitoringStrategy getMonitoringStrategy();
	
	/**
	 * @return
	 */
	public Set<Class<?>> getMonitoredClasses();
	
	/**
	 * @return
	 */
	public Set<Class<?>> getUnMonitoredClasses();
	
	/**
	 * Gets a set of concrete subclasses for the specified class recursively, note that interfaces
	 * and abstract classes are excluded
	 * 
	 * @param clazz
	 * @return a set of subclasses
	 * @should return a list of subclasses for the specified type
	 * @should exclude interfaces and abstract classes
	 */
	public Set<Class<?>> getPersistentConcreteSubclasses(Class<?> clazz);
	
	/**
	 * Finds all the types for associations to monitor in as recursive way i.e if a Persistent type
	 * is found, then we also find its collection element types and types for fields mapped as one
	 * to one, note that this only includes sub types of {@link OpenmrsObject}
	 * 
	 * @param clazz
	 * @return a set of found class names
	 */
	public Set<Class<?>> getAssociationTypesToMonitor(Class<?> clazz);
	
	/**
	 * Checks if the monitoring strategy has been set and cached
	 * 
	 * @return
	 */
	public boolean isMonitoringStrategyCached();
	
	/**
	 * Checks if the monitored classes list has been created and cached if necessary
	 * 
	 * @return
	 */
	public boolean areMonitoredClassesCached();
	
	/**
	 * Checks if the un monitored classes list has been created and cached if necessary
	 * 
	 * @return
	 */
	public boolean areUnMonitoredClassesCached();
	
	/**
	 * Gets implicitly monitored classes, this are generated as a result of their owning entity
	 * types being marked as monitored if they are not explicitly marked as monitored themselves,
	 * i.e if Concept is marked as monitored, then ConceptName, ConceptDesctiption, ConceptMapping
	 * etc implicitly get marked as monitored
	 * 
	 * @return a set of implicitly monitored classes
	 * @should return a set of implicitly monitored classes
	 */
	public Set<Class<?>> getImplicitlyMonitoredClasses();
}
