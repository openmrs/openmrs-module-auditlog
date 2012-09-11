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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;

public class AuditLogListItem {
	
	protected Log log = LogFactory.getLog(getClass());
	
	private Integer auditLogId;
	
	private String classname;
	
	private String simpleClassname;
	
	private String objectUuid;
	
	private String action;
	
	private String userDetails = "";
	
	private String dateCreatedString;
	
	private Map<String, String[]> changesMap;
	
	/**
	 * Convenience constructor that created an {@link AuditLogListItem} from an {@link AuditLog}
	 */
	public AuditLogListItem(AuditLog auditLog) {
		auditLogId = auditLog.getAuditLogId();
		classname = auditLog.getClassName();
		simpleClassname = classname.substring(classname.indexOf(".") + 1);
		//If it is a nested class, use the simple name of the nested class
		if (simpleClassname.indexOf("$") > -1)
			simpleClassname = simpleClassname.substring(simpleClassname.indexOf("$") + 1);
		objectUuid = auditLog.getObjectUuid();
		action = auditLog.getAction().toString();
		if (auditLog.getUser() != null && auditLog.getUser().getPersonName() != null)
			userDetails = auditLog.getUser().getPersonName().getFullName();
		if (auditLog.getUser() != null && StringUtils.isNotBlank(auditLog.getUser().getUsername()))
			userDetails = userDetails + "(" + auditLog.getUser().getUsername() + ")";
		dateCreatedString = Context.getDateFormat().format(auditLog.getDateCreated());
		changesMap = auditLog.getChanges();
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
	 * @return the simpleClassname
	 */
	public String getSimpleClassname() {
		return simpleClassname;
	}
	
	/**
	 * @param simpleClassname the simpleClassname to set
	 */
	public void setSimpleClassname(String simpleClassname) {
		this.simpleClassname = simpleClassname;
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
	 * @return the userDetails
	 */
	public String getUserDetails() {
		return userDetails;
	}
	
	/**
	 * @param userDetails the userDetails to set
	 */
	public void setUserDetails(String userDetails) {
		this.userDetails = userDetails;
	}
	
	/**
	 * @return the dateCreatedString
	 */
	public String getDateCreatedString() {
		return dateCreatedString;
	}
	
	/**
	 * @param dateCreatedString the dateCreatedString to set
	 */
	public void setDateCreatedString(String dateCreatedString) {
		this.dateCreatedString = dateCreatedString;
	}
	
	/**
	 * @return the changesMap
	 */
	public Map<String, String[]> getChangesMap() {
		return changesMap;
	}
	
	/**
	 * @param changesMap the changesMap to set
	 */
	public void setChangesMap(Map<String, String[]> changesMap) {
		this.changesMap = changesMap;
	}
	
}
