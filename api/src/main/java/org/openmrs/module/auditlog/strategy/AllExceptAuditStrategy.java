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

import java.util.Set;

public class AllExceptAuditStrategy extends ExceptionBasedAuditStrategy {
    /**
     * Marks the specified classes as audited by adding their class names to the
     * {@link org.openmrs.GlobalProperty}
     * {@link org.openmrs.module.auditlog.strategy.ExceptionBasedAuditStrategy#GLOBAL_PROPERTY_EXCEPTION}
     *
     * @param clazzes the classes to audit
     * @should update the exception class names global property if the strategy is none_except
     * @should fail if the strategy is set to all
     * @should fail if the strategy is set to none
     * @should update the exception class names global property if the strategy is all_except
     * @should mark a class and its known subclasses as audited
     * @should mark a class and its known subclasses as audited for all_except strategy
     * @should also mark association types as audited
     * @should not mark association types for many to many collections as audited
     */

    /**
	 * @see ConfigurableAuditStrategy#startAuditing(java.util.Set)
	 */
	@Override
	public void startAuditing(Set<Class<?>> clazzes) {
	}
	
	/**
	 * @see ConfigurableAuditStrategy#stopAuditing(java.util.Set)
	 */
	@Override
	public void stopAuditing(Set<Class<?>> clazzes) {
	}
	
	/**
	 * @see ConfigurableAuditStrategy#isAudited(Class)
	 */
	@Override
	public boolean isAudited(Class<?> clazz) {
		return false;
	}
}
