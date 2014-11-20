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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Set;

import org.junit.Before;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptNumeric;
import org.openmrs.EncounterType;
import org.openmrs.OpenmrsObject;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsUtil;

/**
 * Superclass for Test Classes that need to guarantee that the AuditLog Configurations in the test
 * data set are still the same before each test
 */
public abstract class BaseAuditLogTest extends BaseModuleContextSensitiveTest {
	
	protected static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	protected AuditLogService auditLogService;
	
	@Before
	public void before() throws Exception {
		auditLogService = Context.getService(AuditLogService.class);
		executeDataSet(MODULE_TEST_DATA);
		String exceptionsGpValue = "org.openmrs.Concept,org.openmrs.EncounterType,org.openmrs.PatientIdentifierType";
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, AuditingStrategy.NONE_EXCEPT.name());
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_EXCEPTIONS, exceptionsGpValue);
		
		assertEquals(AuditingStrategy.NONE_EXCEPT, auditLogService.getAuditingStrategy());
		Set<Class<? extends OpenmrsObject>> exceptions = auditLogService.getExceptions();
		assertEquals(5, exceptions.size());
		assertTrue(OpenmrsUtil.collectionContains(exceptions, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, PatientIdentifierType.class));
	}
	
	public void setAuditConfiguration(AuditingStrategy strategy, String exceptionsString) throws Exception {
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, strategy.name());
		assertEquals(strategy, auditLogService.getAuditingStrategy());
		if (exceptionsString != null) {
			AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_EXCEPTIONS, exceptionsString);
		}
		if (strategy == AuditingStrategy.ALL || strategy == AuditingStrategy.NONE) {
			assertEquals(0, auditLogService.getExceptions().size());
		}
	}
}
