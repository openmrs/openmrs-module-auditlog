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

import org.openmrs.BaseOpenmrsObject;

/**
 * Encapsulates data for a single audit log entry
 */
public final class MonitoredObject extends BaseOpenmrsObject implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Integer monitoredObjectId;
	
	private String className;
	
	//We actually store user details as 'user.uuid|user.username|user.fullName' and not id
	//We don't want to have foreign keys to users table so that a user can be purged
	//and we still keep the monitored object they added and their username/fullname
	private String creatorDetails;
	
	private Date dateCreated;
	
	/**
	 * Default constructor
	 */
	public MonitoredObject() {
	}
	
	/**
	 * Convenience constructor that takes in a className
	 * 
	 * @param className
	 */
	public MonitoredObject(String className) {
		this.className = className;
	}
	
	/**
	 * @return the monitoredObjectId
	 */
	public Integer getMonitoredObjectId() {
		return monitoredObjectId;
	}
	
	/**
	 * @param monitoredObjectId the monitoredObjectId to set
	 */
	public void setMonitoredObjectId(Integer monitoredObjectId) {
		this.monitoredObjectId = monitoredObjectId;
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
	 * @return the creatorDetails
	 */
	public String getCreatorDetails() {
		return creatorDetails;
	}
	
	/**
	 * @param creatorDetails the creatorDetails to set
	 */
	public void setCreatorDetails(String creatorDetails) {
		this.creatorDetails = creatorDetails;
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
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (!(obj instanceof MonitoredObject))
			return false;
		
		MonitoredObject other = (MonitoredObject) obj;
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
	
	/**
	 * @see org.openmrs.OpenmrsObject#getId()
	 */
	@Override
	public Integer getId() {
		return getMonitoredObjectId();
	}
	
	/**
	 * @see org.openmrs.OpenmrsObject#setId(java.lang.Integer)
	 */
	@Override
	public void setId(Integer id) {
		setMonitoredObjectId(id);
	}
	
}
