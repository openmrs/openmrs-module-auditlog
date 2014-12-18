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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptNumeric;
import org.openmrs.EncounterType;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.strategy.AuditStrategy;
import org.openmrs.module.auditlog.strategy.ExceptionBasedAuditStrategy;
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
	
	protected AuditLogHelper helper;
	
	@Before
	public void before() throws Exception {
		auditLogService = Context.getService(AuditLogService.class);
		helper = Context.getRegisteredComponents(AuditLogHelper.class).get(0);
		executeDataSet(MODULE_TEST_DATA);
		String exceptionsGpValue = "org.openmrs.Concept,org.openmrs.EncounterType,org.openmrs.PatientIdentifierType";
		setAuditConfiguration(AuditStrategy.NONE_EXCEPT, exceptionsGpValue, false);
		
		assertEquals(AuditStrategy.NONE_EXCEPT, auditLogService.getAuditingStrategy());
		Set<Class<?>> exceptions = helper.getExceptions();
		assertEquals(5, exceptions.size());
		assertTrue(OpenmrsUtil.collectionContains(exceptions, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, PatientIdentifierType.class));
	}
	
	public void setAuditConfiguration(AuditStrategy strategy, String exceptionsString, boolean storeLastStateOfDeletedItems)
	    throws Exception {
		
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, strategy.getClass().getName());
		assertEquals(strategy, auditLogService.getAuditingStrategy());
		if (exceptionsString != null) {
			AuditLogUtil.setGlobalProperty(ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION, exceptionsString);
		}
		String value = storeLastStateOfDeletedItems ? "true" : "false";
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_STORE_LAST_STATE_OF_DELETED_ITEMS, value);
		if (!ExceptionBasedAuditStrategy.class.isAssignableFrom(strategy.getClass())) {
			String exceptionsGpValue = Context.getAdministrationService().getGlobalProperty(
			    ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION);
			assertEquals(true, StringUtils.isBlank(exceptionsGpValue));
		}
	}
}
