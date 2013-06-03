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
package org.openmrs.module.auditlog.web.dwr;

import java.util.List;
import java.util.Map;

import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlog.AuditLog;

/**
 * An object of this type contains a display String for updated/created/deleted
 * {@link OpenmrsObject} and in case it is an UPDATE, a mapping of edited property names to the new
 * and previous values' array is included, the new property value is at index 0 and the previous
 * value at index 1 in the array
 */
public class AuditLogDetails {
	
	//Typically it is the name and description for metadata, or the return value
	//of the toString method of the object, otherwise the id of the audited openmrs object
	private String displayString;
	
	//specifies if the original monitored object still exists otherwise it was may deleted later
	private boolean objectExists = false;
	
	//The uuid of the updated/deleted/created object
	private String objectUuid;
	
	private String classname;
	
	private String action;
	
	//The database Id
	private Integer objectId;
	
	private String uuid;
	
	//Mappings of edited property names to their new(index 0) and previous(index 1) values' array
	private Map<String, String[]> changes;
	
	private List<AuditLogDetails> childAuditLogDetails;
	
	/**
	 * Convenience constructor that created an {@link AuditLogDetails} from an {@link AuditLog}
	 */
	public AuditLogDetails(String displayString, String objectUuid, String classname, String action, Integer objectId,
	    String uuid, boolean objectExists, Map<String, String[]> changes) {
		this.displayString = displayString;
		this.objectExists = objectExists;
		this.objectUuid = objectUuid;
		this.classname = classname;
		this.action = action;
		this.objectId = objectId;
		this.uuid = uuid;
		this.changes = changes;
	}
	
	/**
	 * @return the displayString
	 */
	public String getDisplayString() {
		return displayString;
	}
	
	/**
	 * @param displayString the displayString to set
	 */
	public void setDisplayString(String displayString) {
		this.displayString = displayString;
	}
	
	/**
	 * @return the objectExists
	 */
	public boolean isObjectExists() {
		return objectExists;
	}
	
	/**
	 * @param objectExists the objectExists to set
	 */
	public void setObjectExists(boolean objectExists) {
		this.objectExists = objectExists;
	}
	
	/**
	 * @return
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
	 * @return the classname
	 */
	public String getClassname() {
		return classname;
	}
	
	/**
	 * @param classname the classname to set
	 */
	public void setClassname(String classname) {
		this.classname = classname;
	}
	
	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}
	
	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}
	
	/**
	 * @return the objectId
	 */
	public Integer getObjectId() {
		return objectId;
	}
	
	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(Integer objectId) {
		this.objectId = objectId;
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
	 * @return the changes
	 */
	public Map<String, String[]> getChanges() {
		return changes;
	}
	
	/**
	 * @param changes the changes to set
	 */
	public void setChanges(Map<String, String[]> changes) {
		this.changes = changes;
	}
	
	/**
	 * @return the childAuditLogDetails
	 */
	public List<AuditLogDetails> getChildAuditLogDetails() {
		return childAuditLogDetails;
	}
	
	/**
	 * @param childAuditLogDetails the childAuditLogDetails to set
	 */
	public void setChildAuditLogDetails(List<AuditLogDetails> childAuditLogDetails) {
		this.childAuditLogDetails = childAuditLogDetails;
	}
}
