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
package org.openmrs.module.auditlog.util;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.EncounterType;
import org.openmrs.OpenmrsObject;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsUtil;

/**
 * Contains tests for {@link AuditLogUtil} methods
 */
public class AuditLogUtilTest extends BaseModuleContextSensitiveTest {
	
	private static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	/**
	 * Convenience method that marks a given object type as monitored
	 * 
	 * @param clazzes
	 */
	public static void startMonitoring(Class<? extends OpenmrsObject> clazz) {
		Set<Class<? extends OpenmrsObject>> monitoredClasses = new HashSet<Class<? extends OpenmrsObject>>();
		monitoredClasses.add(clazz);
		AuditLogUtil.startMonitoring(monitoredClasses);
	}
	
	/**
	 * Convenience method that marks a given object type as un monitored
	 * 
	 * @param clazzes
	 */
	public static void stopMonitoring(Class<? extends OpenmrsObject> clazz) {
		Set<Class<? extends OpenmrsObject>> monitoredClasses = new HashSet<Class<? extends OpenmrsObject>>();
		monitoredClasses.add(clazz);
		AuditLogUtil.stopMonitoring(monitoredClasses);
	}
	
	/**
	 * @see {@link AuditLogUtil#getMonitoredClassNames()}
	 */
	@Test
	@Verifies(value = "should return a list of monitored class names", method = "getMonitoredClassNames()")
	public void getMonitoredClassNames_shouldReturnAListOfMonitoredClassNames() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Context.clearSession();
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		Assert.assertEquals(4, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
	}
	
	/**
	 * @see {@link AuditLogUtil#startMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should add the class names to the monitored objects global property", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldAddTheClassNamesToTheMonitoredObjectsGlobalProperty() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalCount = monitoredClasses.size();
		Assert.assertFalse(OpenmrsUtil.collectionContains(monitoredClasses, ConceptDescription.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		try {
			startMonitoring(ConceptDescription.class);
			Set<String> newMonitoredClasses = AuditLogUtil.getMonitoredClassNames();
			Assert.assertEquals(++originalCount, newMonitoredClasses.size());
			//Should have added it and maintained the existing ones
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptDescription.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(newMonitoredClasses, Concept.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(newMonitoredClasses, ConceptName.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(newMonitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(newMonitoredClasses, PatientIdentifierType.class.getName()));
		}
		finally {
			//Clear the cache not to affect other tests
			AuditLogUtilTest.stopMonitoring(ConceptDescription.class);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#stopMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should remove the class names from the monitored objects global property", method = "stopMonitoring(Set<Class<OpenmrsObject>>)")
	public void stopMonitoring_shouldRemoveTheClassNamesFromTheMonitoredObjectsGlobalProperty() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalCount = monitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		stopMonitoring(PatientIdentifierType.class);
		Set<String> newMonitoredClasses = AuditLogUtil.getMonitoredClassNames();
		Assert.assertEquals(--originalCount, newMonitoredClasses.size());
		//Should have been removed
		Assert.assertFalse(OpenmrsUtil.collectionContains(newMonitoredClasses, PatientIdentifierType.class.getName()));
		//the existing should have remained
		Assert.assertTrue(OpenmrsUtil.collectionContains(newMonitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(newMonitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(newMonitoredClasses, EncounterType.class.getName()));
	}
}
