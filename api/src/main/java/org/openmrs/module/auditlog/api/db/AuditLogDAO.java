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

import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.AuditingStrategy;
import org.openmrs.module.auditlog.api.AuditLogService;

/**
 * Database access methods for {@link AuditLog}s
 */
public interface AuditLogDAO {
	
	/**
	 * @see AuditLogService#isAudited(Class)
	 */
	public boolean isAudited(Class<?> clazz);
	
	/**
	 * Checks if the specified type is implicitly audit
	 * 
	 * @should return true if a class is implicitly audited
	 * @should return false if a class is not implicitly audited
	 * @should return false if a class is also marked as audited
	 */
	public boolean isImplicitlyAudited(Class<?> clazz);
	
	/**
	 * Fetches the audit log entries matching the specified arguments
	 * 
	 * @param uuid the uuid to match against
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
	public List<AuditLog> getAuditLogs(String uuid, List<Class<? extends OpenmrsObject>> types, List<Action> actions,
	                                   Date startDate, Date endDate, boolean excludeChildAuditLogs, Integer start,
	                                   Integer length);
	
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
	 * @see AuditLogService#getObjectById(Class, Integer)
	 */
	public <T> T getObjectById(Class<T> clazz, Integer id);
	
	/**
	 * @see AuditLogService#getObjectByUuid(Class, String)
	 */
	public <T> T getObjectByUuid(Class<T> clazz, String uuid);
	
	/**
	 * @see AuditLogService#startAuditing(java.util.Set)
	 */
	public void startAuditing(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * @see AuditLogService#stopAuditing(java.util.Set)
	 */
	public void stopAuditing(Set<Class<? extends OpenmrsObject>> clazzes);
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getAuditingStrategy()
	 */
	public AuditingStrategy getAuditingStrategy();
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getAuditedClasses()
	 */
	public Set<Class<? extends OpenmrsObject>> getAuditedClasses();
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getUnAuditedClasses()
	 */
	public Set<Class<? extends OpenmrsObject>> getUnAuditedClasses();
	
	/**
	 * Gets a set of concrete subclasses for the specified class recursively, note that interfaces
	 * and abstract classes are excluded
	 * 
	 * @param clazz the Super Class
	 * @return a set of subclasses
	 * @should return a list of subclasses for the specified type
	 * @should exclude interfaces and abstract classes
	 */
	public Set<Class<? extends OpenmrsObject>> getPersistentConcreteSubclasses(Class<? extends OpenmrsObject> clazz);
	
	/**
	 * Finds all the types for associations to audit in as recursive way i.e if a Persistent type is
	 * found, then we also find its collection element types and types for fields mapped as one to
	 * one, note that this only includes sub types of {@link OpenmrsObject}
	 * 
	 * @param clazz the Class to match against
	 * @return a set of found class names
	 */
	public Set<Class<? extends OpenmrsObject>> getAssociationTypesToAudit(Class<? extends OpenmrsObject> clazz);
	
	/**
	 * Gets implicitly audited classes, this are generated as a result of their owning entity types
	 * being marked as audited if they are not explicitly marked as audited themselves, i.e if
	 * Concept is marked as audited, then ConceptName, ConceptDescription, ConceptMapping etc
	 * implicitly get marked as audited
	 * 
	 * @return a set of implicitly audited classes
	 * @should return a set of implicitly audited classes
	 */
	public Set<Class<? extends OpenmrsObject>> getImplicitlyAuditedClasses();
}
