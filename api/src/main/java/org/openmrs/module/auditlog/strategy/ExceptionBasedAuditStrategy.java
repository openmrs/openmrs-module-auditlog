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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLogGlobalPropertyHelper;
import org.openmrs.module.auditlog.util.AuditLogConstants;

public abstract class ExceptionBasedAuditStrategy implements ConfigurableAuditStrategy {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	public static final String GLOBAL_PROPERTY_EXCEPTION = AuditLogConstants.MODULE_ID + ".exceptions";
	
	private AuditLogGlobalPropertyHelper helper = null;
	
	/**
	 * Returns a set of exception classes as specified by the {@link org.openmrs.GlobalProperty}
	 * GLOBAL_PROPERTY_EXCEPTION
	 * 
	 * @return a set of audited classes
	 * @should return a set of exception classes
	 */
	public Set<Class<?>> getExceptions() {
		if (helper == null) {
			helper = Context.getRegisteredComponents(AuditLogGlobalPropertyHelper.class).get(0);
		}
		return helper.getExceptions();
	}
}
