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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.MonitoringStrategy;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsUtil;

/**
 * Contains tests for {@link AuditLogUtil} methods
 */
public class AuditLogUtilTest extends BaseModuleContextSensitiveTest {
	
	private static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	/**
	 * @see {@link AuditLogUtil#getMonitoredClassNames()}
	 */
	@Test
	@Verifies(value = "should return a set of monitored class names", method = "getMonitoredClassNames()")
	public void getMonitoredClassNames_shouldReturnASetOfMonitoredClassNames() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		Assert.assertEquals(4, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
	}
	
	/**
	 * @see {@link AuditLogUtil#getUnMonitoredClassNames()}
	 */
	@Test
	@Verifies(value = "should return a set of un monitored class names", method = "getUnMonitoredClassNames()")
	public void getUnMonitoredClassNames_shouldReturnASetOfUnMonitoredClassNames() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
		Assert.assertEquals(1, unMonitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
	}
	
	/**
	 * @see {@link AuditLogUtil#startMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should update the monitored class names global property if the strategy is none_except", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldUpdateTheMonitoredClassNamesGlobalPropertyIfTheStrategyIsNone_except()
	    throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Assert.assertEquals(MonitoringStrategy.NONE_EXCEPT, AuditLogUtil.getMonitoringStrategy());
		
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalCount = monitoredClasses.size();
		Assert.assertFalse(OpenmrsUtil.collectionContains(monitoredClasses, ConceptDescription.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		try {
			AuditLogUtil.startMonitoring(ConceptDescription.class);
			
			monitoredClasses = AuditLogUtil.getMonitoredClassNames();
			Assert.assertEquals(++originalCount, monitoredClasses.size());
			//Should have added it and maintained the existing ones
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptDescription.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		}
		finally {
			//reset
			AuditLogUtil.stopMonitoring(ConceptDescription.class);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#startMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should update the un monitored class names global property if the strategy is all_except", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldUpdateTheUnMonitoredClassNamesGlobalPropertyIfTheStrategyIsAll_except()
	    throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
		int originalCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.AUDITLOG_GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.startMonitoring(EncounterType.class);
			
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
			Assert.assertEquals(--originalCount, unMonitoredClasses.size());
			//Should have removed it and maintained the existing ones
			Assert.assertFalse(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		}
		finally {
			//reset
			AuditLogUtil.stopMonitoring(EncounterType.class);
			gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#startMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should not update any global property if the strategy is all", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsAll() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(4, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		Set<String> unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.AUDITLOG_GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.startMonitoring(EncounterType.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClassNames();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		}
		finally {
			//reset
			AuditLogUtil.stopMonitoring(EncounterType.class);
			gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#startMonitoring(Set<QClass<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should not update any global property if the strategy is none", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsNone() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(4, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		Set<String> unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.AUDITLOG_GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.NONE.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.startMonitoring(EncounterType.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClassNames();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		}
		finally {
			AuditLogUtil.stopMonitoring(EncounterType.class);
			gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#stopMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should not update any global property if the strategy is all", method = "stopMonitoring(Set<Class<OpenmrsObject>>)")
	public void stopMonitoring_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsAll() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(4, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		Set<String> unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.AUDITLOG_GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClassNames();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		}
		finally {
			//reset
			AuditLogUtil.startMonitoring(Concept.class);
			gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#stopMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should not update any global property if the strategy is none", method = "stopMonitoring(Set<Class<OpenmrsObject>>)")
	public void stopMonitoring_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsNone() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(4, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		Set<String> unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.AUDITLOG_GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.NONE.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClassNames();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		}
		finally {
			AuditLogUtil.startMonitoring(Concept.class);
			gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#stopMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should update the monitored class names global property if the strategy is none_except", method = "stopMonitoring(Set<Class<OpenmrsObject>>)")
	public void stopMonitoring_shouldUpdateTheMonitoredClassNamesGlobalPropertyIfTheStrategyIsNone_except() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Assert.assertEquals(MonitoringStrategy.NONE_EXCEPT, AuditLogUtil.getMonitoringStrategy());
		
		Set<String> monitoredClasses = AuditLogUtil.getMonitoredClassNames();
		int originalCount = monitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			
			monitoredClasses = AuditLogUtil.getMonitoredClassNames();
			Assert.assertEquals(--originalCount, monitoredClasses.size());
			//Should have added it and maintained the existing ones
			Assert.assertFalse(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptName.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class.getName()));
		}
		finally {
			//reset
			AuditLogUtil.startMonitoring(Concept.class);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#stopMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should update the un monitored class names global property if the strategy is all_except", method = "stopMonitoring(Set<Class<OpenmrsObject>>)")
	public void stopMonitoring_shouldUpdateTheUnMonitoredClassNamesGlobalPropertyIfTheStrategyIsAll_except()
	    throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<String> unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
		int originalCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
		Assert.assertFalse(OpenmrsUtil.collectionContains(unMonitoredClasses, Concept.class.getName()));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.AUDITLOG_GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClassNames();
			Assert.assertEquals(++originalCount, unMonitoredClasses.size());
			//Should have removed it and maintained the existing ones
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class.getName()));
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, Concept.class.getName()));
		}
		finally {
			//reset
			AuditLogUtil.startMonitoring(Concept.class);
			//reset
			gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
}
