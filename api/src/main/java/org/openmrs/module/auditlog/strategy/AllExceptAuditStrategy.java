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
	 * @see ConfigurableAuditStrategy#startAuditing(java.util.Set)
	 * @should update the exception class names global property
	 * @should mark a class and its known subclasses as audited
	 * @should also mark association types as audited
	 * @should not mark association types for many to many collections as audited
	 */
	@Override
	public void startAuditing(Set<Class<?>> clazzes) {
		getHelper().updateGlobalProperty(clazzes, true);
	}
	
	/**
	 * @see ConfigurableAuditStrategy#stopAuditing(java.util.Set)
	 * @should update the exception class names global property
	 * @should mark a class and its known subclasses as un audited
	 * @should not remove explicitly monitored association types when the parent is removed
	 */
	@Override
	public void stopAuditing(Set<Class<?>> clazzes) {
		getHelper().updateGlobalProperty(clazzes, false);
	}
	
	/**
	 * @see ConfigurableAuditStrategy#isAudited(Class)
	 */
	@Override
	public boolean isAudited(Class<?> clazz) {
		return getHelper().isAudited(clazz);
	}
}
