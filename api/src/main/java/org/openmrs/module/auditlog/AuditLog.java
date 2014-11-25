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
package org.openmrs.module.auditlog;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.openmrs.User;


/**
 * Encapsulates data for a single audit log entry
 */
public class AuditLog implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String uuid = UUID.randomUUID().toString();
	
	private Integer auditLogId;
	
	//the fully qualified java class name of the create/updated/deleted object
	private Class<?> type;
	
	//the uuid of the created/updated/deleted object
	private Serializable identifier;
	
	//the performed operation that which could be a create, update or delete
	private Action action;
	
	private User user;
	
	private Date dateCreated;
	
	private String openmrsVersion;
	
	private String moduleVersion;
	
	private AuditLog parentAuditLog;
	
	private Set<AuditLog> childAuditLogs;
	
	/**
	 * Used to store Json for field new and old values for updated items or last properties values
	 * of deleted items
	 */
	private Blob serializedData;
	
	public enum Action {
		CREATED, UPDATED, DELETED
	}
	
	/**
	 * Default constructor
	 */
	public AuditLog() {
	}
	
	/**
	 * Convenience constructor
	 * 
	 * @param type the fully qualified classname of the Object type
	 * @param identifier the id of the object
	 * @param action the operation performed on the object
	 * @param user the user that triggered the operation
	 * @param dateCreated the date when the operation was done
	 */
	public AuditLog(Class<?> type, Serializable identifier, Action action, User user, Date dateCreated) {
		this();
		this.type = type;
		this.identifier = identifier;
		this.action = action;
		this.user = user;
		this.dateCreated = dateCreated;
	}
	
	/**
	 * @return the auditLogId
	 */
	public Integer getAuditLogId() {
		return auditLogId;
	}
	
	/**
	 * @param auditLogId the auditLogId to set
	 */
	public void setAuditLogId(Integer auditLogId) {
		this.auditLogId = auditLogId;
	}
	
	/**
	 * @return the type
	 */
	public Class<?> getType() {
		return type;
	}
	
	/**
	 * @param type the type to set
	 */
	public void setType(Class<?> type) {
		this.type = type;
	}
	
	/**
	 * @return the identifier
	 */
	public Serializable getIdentifier() {
		return identifier;
	}
	
	/**
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(Serializable identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}
	
	/**
	 * @param action the action to set
	 */
	public void setAction(Action action) {
		this.action = action;
	}
	
	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}
	
	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}
	
	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}
	
	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	/**
	 * @return the openmrsVersion
	 */
	public String getOpenmrsVersion() {
		return openmrsVersion;
	}
	
	/**
	 * @param openmrsVersion the openmrsVersion to set to
	 */
	public void setOpenmrsVersion(String openmrsVersion) {
		this.openmrsVersion = openmrsVersion;
	}
	
	/**
	 * @return moduleVersion
	 */
	public String getModuleVersion() {
		return moduleVersion;
	}
	
	/**
	 * @param moduleVersion the moduleVersion to set to
	 */
	public void setModuleVersion(String moduleVersion) {
		this.moduleVersion = moduleVersion;
	}
	
	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}
	
	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * @return the parentAuditLog
	 */
	public AuditLog getParentAuditLog() {
		return parentAuditLog;
	}
	
	/**
	 * @param parentAuditLog the parentAuditLog to set
	 */
	public void setParentAuditLog(AuditLog parentAuditLog) {
		this.parentAuditLog = parentAuditLog;
	}
	
	/**
	 * @return the childAuditLogs
	 */
	public Set<AuditLog> getChildAuditLogs() {
		if (childAuditLogs == null) {
			childAuditLogs = new LinkedHashSet<AuditLog>();
		}
		return childAuditLogs;
	}
	
	/**
	 * @param childAuditLogs the childAuditLogs to set
	 */
	public void setChildAuditLogs(Set<AuditLog> childAuditLogs) {
		this.childAuditLogs = childAuditLogs;
	}
	
	/**
	 * @return the serializedData
	 */
	public Blob getSerializedData() {
		return serializedData;
	}
	
	/**
	 * @param serializedData the serializedData to set
	 */
	public void setSerializedData(Blob serializedData) {
		this.serializedData = serializedData;
	}
	
	/**
	 * Returns the simple forms of the classname property e.g 'Concept Name' will be returned for
	 * ConceptName
	 * 
	 * @return the classname
	 */
	public String getSimpleTypeName() {
		String[] sections = StringUtils.splitByCharacterTypeCamelCase(getType().getSimpleName());
		return StringUtils.join(sections, " ");
	}
	
	/**
	 * Adds the specified auditLog as a child
	 * 
	 * @param auditLog the AuditLog to add
	 */
	public void addChildAuditLog(AuditLog auditLog) {
		if (auditLog == null) {
			return;
		}
		auditLog.setParentAuditLog(this);
		getChildAuditLogs().add(auditLog);
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj
		        || (obj instanceof AuditLog && getUuid() != null && ((AuditLog) obj).getUuid().equals(this.getUuid()));
		
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (getUuid() == null) {
			return super.hashCode();
		}
		return getUuid().hashCode();
	}
	
	/**
	 * @see Object#toString() ()
	 */
	@Override
	public String toString() {
		return action + " " + type + " " + identifier;
	}
	
	public boolean hasChildLogs() {
		return getChildAuditLogs().size() > 0;
	}
}
