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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.openmrs.module.auditlog.AuditLog;

/**
 * An object of this type contains a display String for updated/created/deleted object and in case
 * it is an UPDATE, a mapping of edited property names to the new and previous values' array is
 * included, the new property value is at index 0 and the previous value at index 1 in the array
 */
public class AuditLogDetails {
	
	//Typically it is the name and description for metadata, or the return value
	//of the toString method of the object, otherwise the id of the audited openmrs object
	private String displayString;
	
	//specifies if the original audited object still exists otherwise it was may deleted later
	private boolean objectExists = false;
	
	//The uuid of the updated/deleted/created object
	private Serializable identifier;
	
	private String simpleTypeName;
	
	private String action;
	
	private String uuid;
	
	private String openmrsVersion;
	
	//If UPDATE auditlog, this is mappings of edited property names to their new(index 0) 
	//and previous(index 1) values' array, if DELETED it is all the property and their values
	private Map<String, Object> changes;
	
	private List<AuditLogDetails> childAuditLogDetails;
	
	/**
	 * Convenience constructor that created an {@link AuditLogDetails} from an {@link AuditLog}
	 */
	public AuditLogDetails(String displayString, Serializable identifier, String simpleTypeName, String action, String uuid,
	    String openmrsVersion, boolean objectExists, Map<String, Object> changes) {
		this.displayString = displayString;
		this.objectExists = objectExists;
		this.identifier = identifier;
		this.simpleTypeName = simpleTypeName;
		this.action = action;
		this.uuid = uuid;
		this.openmrsVersion = openmrsVersion;
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
	 * @return the identifier
	 */
	public Serializable getIdentifier() {
		return identifier;
	}
	
	/**
	 * @param identifier the action to set
	 */
	public void setIdentifier(Serializable identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * @return the simpleTypeName
	 */
	public String getSimpleTypeName() {
		return simpleTypeName;
	}
	
	/**
	 * @param simpleTypeName the simpleTypeName to set
	 */
	public void setSimpleTypeName(String simpleTypeName) {
		this.simpleTypeName = simpleTypeName;
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
	 * @return the openmrsVersion
	 */
	public String getOpenmrsVersion() {
		return openmrsVersion;
	}
	
	/**
	 * @param openmrsVersion the openmrsVersion to set
	 */
	public void setOpenmrsVersion(String openmrsVersion) {
		this.openmrsVersion = openmrsVersion;
	}
	
	/**
	 * @return the changes
	 */
	public Map<String, Object> getChanges() {
		return changes;
	}
	
	/**
	 * @param changes the changes to set
	 */
	public void setChanges(Map<String, Object> changes) {
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
