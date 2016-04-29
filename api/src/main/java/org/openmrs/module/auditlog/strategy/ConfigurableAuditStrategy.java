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

public abstract class ConfigurableAuditStrategy extends BaseAuditStrategy {
	
	/**
	 * Implementing classes should mark the specified classes as audited
	 * 
	 * @param clazzes the classes to audit
	 */
	public abstract void startAuditing(Set<Class<?>> clazzes);
	
	/**
	 * Implementing classes should un mark the specified classes as audited
	 * 
	 * @param clazzes the classes to stop auditing
	 */
	public abstract void stopAuditing(Set<Class<?>> clazzes);
}
