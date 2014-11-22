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

import org.openmrs.module.auditlog.util.AuditLogConstants;

/**
 * Enumeration of the auditing strategies that can be used to declare which object types to audit
 * and exclude.
 */
public enum AuditingStrategy {
	
	/**
	 * All objects are audited for this strategy
	 */
	ALL(AuditLogConstants.MODULE_ID + ".all", AuditLogConstants.MODULE_ID + ".description"),
	
	/**
	 * Same as {@link #ALL} with the option of defining which classes to ignore
	 */
	ALL_EXCEPT(AuditLogConstants.MODULE_ID + ".allExcept", AuditLogConstants.MODULE_ID + ".allExcept.description"),
	
	/**
	 * Nothing gets audited for this strategy
	 */
	NONE("general.none", AuditLogConstants.MODULE_ID + ".none.description"),
	
	/**
	 * Same as {@link #NONE_EXCEPT} with the option of defining which classes to audit
	 */
	NONE_EXCEPT(AuditLogConstants.MODULE_ID + ".noneExcept", AuditLogConstants.MODULE_ID + ".noneExcept.description");
	
	private final String displayText;
	
	private final String description;
	
	/**
	 * @param displayText
	 * @param description
	 */
	private AuditingStrategy(String displayText, String description) {
		this.displayText = displayText;
		this.description = description;
	}
	
	/**
	 * Returns the displayText to be displayed e.g in the IU
	 * 
	 * @return The displayText for the enum value
	 */
	public String displayText() {
		return displayText;
	}
	
	/**
	 * Returns the description of the enum constant
	 * 
	 * @return the description
	 */
	public String description() {
		return description;
	}
}
