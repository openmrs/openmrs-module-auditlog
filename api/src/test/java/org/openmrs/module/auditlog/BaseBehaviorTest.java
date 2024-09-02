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

import java.io.Serializable;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog.Action;

/**
 * Superclass for Test Classes that contain tests for testing the core functionality of the module
 */

@Ignore
public abstract class BaseBehaviorTest extends BaseAuditLogTest {
	
	protected ConceptService conceptService;
	
	protected EncounterService encounterService;
	
	@Before
	public void setup() throws Exception {
		conceptService = Context.getConceptService();
		encounterService = Context.getEncounterService();
		//No log entries should be existing
		Assert.assertTrue(getAllLogs().isEmpty());
	}
	
	/**
	 * Utility method to get all logs
	 * 
	 * @return a list of {@link AuditLog}s
	 */
	protected List<AuditLog> getAllLogs() {
		return auditLogService.getAuditLogs(null, null, null, null, false, null, null);
	}
	
	/**
	 * Utility method to get all logs for a specific object
	 * 
	 * @return a list of {@link AuditLog}s
	 */
	protected List<AuditLog> getAllLogs(Serializable id, Class<?> clazz, List<Action> actions) {
		return auditLogService.getAuditLogs(id, clazz, actions, null, null, false);
	}
}
