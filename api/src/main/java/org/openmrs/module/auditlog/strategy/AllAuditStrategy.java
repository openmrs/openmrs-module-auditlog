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

import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLogHelper;

public final class AllAuditStrategy implements AuditStrategy {
	
	private AuditLogHelper helper = null;
	
	/**
	 * Gets the AuditLogHelper instance
	 * 
	 * @return
	 */
	public AuditLogHelper getHelper() {
		if (helper == null) {
			helper = Context.getRegisteredComponents(AuditLogHelper.class).get(0);
		}
		return helper;
	}
	
	/**
	 * @see AuditStrategy#isAudited(Class)
	 */
	@Override
	public boolean isAudited(Class<?> clazz) {
		return getHelper().isAudited(clazz);
	}
}
