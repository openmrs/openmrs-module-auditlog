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
package org.openmrs.module.auditlog.strategy;

/**
 * Super interface for auditing Strategies, an audit strategy encapsulates the logic that determines
 * which persistent types are audited and the ones that are not, in case an audit strategy supports
 * manipulation of which types are audited, then it would typically provide methods to start or stop
 * monitoring a given type.
 * 
 * <pre>
 * Note: Developers are highly recommended to extend BaseAuditStrategy
 * rather than implementing this class directly
 * 
 * <pre/>
 */
public interface AuditStrategy {
	
	AuditStrategy ALL = new AllAuditStrategy();
	
	AuditStrategy NONE = new NoneAuditStrategy();
	
	AuditStrategy NONE_EXCEPT = new NoneExceptAuditStrategy();
	
	AuditStrategy ALL_EXCEPT = new AllExceptAuditStrategy();
	
	String SHORT_NAME_ALL = "ALL";
	
	String SHORT_NAME_ALL_EXCEPT = "ALL_EXCEPT";
	
	String SHORT_NAME_NONE = "NONE";
	
	String SHORT_NAME_NONE_EXCEPT = "NONE_EXCEPT";
	
	/**
	 * Implementations of this method should return true if the specified type is audited otherwise
	 * false
	 * 
	 * @param clazz the class to check
	 */
	boolean isAudited(Class<?> clazz);
	
}
