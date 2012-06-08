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
import java.util.Map;
import java.util.TreeMap;

import org.openmrs.User;
import org.openmrs.module.auditlog.util.AuditLogUtil;

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
	
	//We actually store user details as 'user.uuid|user.username|user.fullName' and not id
	//We don't want to have foreign keys to users table so that a user can be purged
	//and we still keep the audit logs for the changes they made
	private String userDetails;
	
	private Date dateCreated;
	
	private String uuid;
	
	//property name old value map, NOTE we store all old values as Strings
	private Map<String, String> previousValues;
	
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
	 * @param className
	 * @param objectId
	 * @param action
	 * @param user
	 * @param dateCreated
	 */
	public AuditLog(String className, String objectUuid, Action action, User user, Date dateCreated, String uuid) {
		this.className = className;
		this.objectUuid = objectUuid;
		this.action = action;
		this.userDetails = AuditLogUtil.getUserDetails(user);
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
	 * @return the user details
	 */
	public String getUserDetails() {
		return userDetails;
	}
	
	/**
	 * @param userDetails the user to set
	 */
	public void setUserDetails(String userDetails) {
		this.userDetails = userDetails;
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
	 * @return the previousValues
	 */
	public Map<String, String> getPreviousValues() {
		//we use a tree map because we don't expect duplicate property names
		if (previousValues == null)
			previousValues = new TreeMap<String, String>();
		
		return previousValues;
	}
	
	/**
	 * @param previousValues the previousValues to set
	 */
	public void setPreviousValues(Map<String, String> previousValues) {
		this.previousValues = previousValues;
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
