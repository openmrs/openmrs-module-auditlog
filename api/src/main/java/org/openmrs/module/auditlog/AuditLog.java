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
import java.util.Date;

import org.openmrs.User;

/**
 * Encapsulates data for a single audit log entry
 */
public final class AuditLog implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Integer auditLogId;
	
	//the fully qualified java class name of the create/updated/deleted object
	private String className;
	
	//the uuid of the created/updated/deleted object
	private String objectUuid;
	
	//the performed operation that which could be a create, update or delete
	private Action action;
	
	private User user;
	
	private Date dateCreated;
	
	private String uuid;
	
	/**
	 * Xml for new and old values in case of edited fields
	 * 
	 * <pre>
	 * 		<property name="property_name">
	 * 			<previous>.....</previous>
	 * 			<new>....</new>
	 * 		</property>
	 * 		.....
	 * </pre>
	 */
	private String newAndPreviousValuesXml;
	
	public enum Action {
		CREATED, UPDATED, DELETED
		//, REVERTED
	}
	
	/**
	 * Default constructor
	 */
	public AuditLog() {
	}
	
	/**
	 * Convenience constructor
	 * 
	 * @param className the fully qualified classname of the Object type
	 * @param objectId the id of the object
	 * @param action the operation performed on the object
	 * @param user the user that triggered the operation
	 * @param dateCreated the date when the operation was triggered
	 */
	public AuditLog(String className, String objectUuid, Action action, User user, Date dateCreated, String uuid) {
		this.className = className;
		this.objectUuid = objectUuid;
		this.action = action;
		this.user = user;
		this.dateCreated = dateCreated;
		this.uuid = uuid;
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
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}
	
	/**
	 * @return the objectUuid
	 */
	public String getObjectUuid() {
		return objectUuid;
	}
	
	/**
	 * @param objectUuid the objectUuid to set
	 */
	public void setObjectUuid(String objectUuid) {
		this.objectUuid = objectUuid;
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
	 * @return the newAndPreviousValuesXml
	 */
	public String getNewAndPreviousValuesXml() {
		return newAndPreviousValuesXml;
	}
	
	/**
	 * @param newAndPreviousValuesXml the newAndPreviousValuesXml to set
	 */
	public void setNewAndPreviousValuesXml(String newAndPreviousValuesXml) {
		this.newAndPreviousValuesXml = newAndPreviousValuesXml;
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (!(obj instanceof AuditLog))
			return false;
		
		AuditLog other = (AuditLog) obj;
		if (getUuid() == null)
			return false;
		
		return other.getUuid().equals(this.getUuid());
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (getUuid() == null)
			return super.hashCode();
		return getUuid().hashCode();
	}
	
}
