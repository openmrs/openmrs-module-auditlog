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

import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Superclass for Test Classes that contain tests for testing the core functionality of the module
 */
public abstract class BaseBehaviorTest extends BaseModuleContextSensitiveTest {
	
	protected static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	protected ConceptService conceptService;
	
	protected AuditLogService auditLogService;
	
	protected EncounterService encounterService;
	
	@Before
	public void before() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		auditLogService = Context.getService(AuditLogService.class);
		AdministrationService as = Context.getAdministrationService();
		if (AuditingStrategy.NONE_EXCEPT != auditLogService.getAuditingStrategy()) {
			GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
			gp.setPropertyValue(AuditingStrategy.NONE_EXCEPT.name());
			as.saveGlobalProperty(gp);
		}
		
		final String auditedGpValue = "org.openmrs.Concept,org.openmrs.EncounterType,org.openmrs.PatientIdentifierType";
		GlobalProperty auditedGP = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITED_CLASSES);
		if (!auditedGP.getPropertyValue().equals(auditedGpValue)) {
			auditedGP.setPropertyValue(auditedGpValue);
			as.saveGlobalProperty(auditedGP);
		}
		
		final String unAuditedGpValue = "org.openmrs.EncounterType";
		GlobalProperty unAuditedGP = as.getGlobalPropertyObject(AuditLogConstants.GP_UN_AUDITED_CLASSES);
		if (!unAuditedGP.getPropertyValue().equals(unAuditedGpValue)) {
			unAuditedGP.setPropertyValue(unAuditedGpValue);
			as.saveGlobalProperty(unAuditedGP);
		}
		
		conceptService = Context.getConceptService();
		encounterService = Context.getEncounterService();
		
		//No log entries should be existing
		Assert.assertTrue(getAllLogs().isEmpty());
		Assert.assertEquals(AuditingStrategy.NONE_EXCEPT, auditLogService.getAuditingStrategy());
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
	protected List<AuditLog> getAllLogs(String uuid, Class<? extends OpenmrsObject> clazz, List<Action> actions) {
		return auditLogService.getAuditLogs(uuid, clazz, actions, null, null, false);
	}
}
