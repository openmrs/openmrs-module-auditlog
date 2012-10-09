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

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.ConceptNumeric;
import org.openmrs.ConceptSet;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
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
	
	public static void setMonitoringStrategy(MonitoringStrategy strategy) throws Exception {
		if (strategy != AuditLogUtil.getMonitoringStrategy()) {
			AdministrationService as = Context.getAdministrationService();
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.GP_MONITORING_STRATEGY);
			if (gp == null) {
				gp = new GlobalProperty(AuditLogConstants.GP_MONITORING_STRATEGY, MonitoringStrategy.NONE_EXCEPT.name());
			} else {
				gp.setPropertyValue(strategy.name());
			}
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#getMonitoredClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of monitored classes", method = "getMonitoredClassNames()")
	public void getMonitoredClasses_shouldReturnASetOfMonitoredClasses() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		Assert.assertEquals(5, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
	}
	
	/**
	 * @see {@link AuditLogUtil#getUnMonitoredClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of un monitored classes", method = "getUnMonitoredClassNames()")
	public void getUnMonitoredClasses_shouldReturnASetOfUnMonitoredClasses() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		AdministrationService as = Context.getAdministrationService();
		//In case previous tests changed the value
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_UN_MONITORED_CLASSES);
		gp.setPropertyValue(EncounterType.class.getName());
		as.saveGlobalProperty(gp);
		Set<Class<?>> unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
		Assert.assertEquals(1, unMonitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
	}
	
	/**
	 * @see {@link AuditLogUtil#startMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should update the monitored class names global property if the strategy is none_except", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldUpdateTheMonitoredClassNamesGlobalPropertyIfTheStrategyIsNone_except()
	    throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		setMonitoringStrategy(MonitoringStrategy.NONE_EXCEPT);
		Assert.assertEquals(MonitoringStrategy.NONE_EXCEPT, AuditLogUtil.getMonitoringStrategy());
		
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		int originalCount = monitoredClasses.size();
		Assert.assertFalse(OpenmrsUtil.collectionContains(monitoredClasses, ConceptDescription.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
		
		try {
			AuditLogUtil.startMonitoring(ConceptDescription.class);
			
			monitoredClasses = AuditLogUtil.getMonitoredClasses();
			Assert.assertEquals(++originalCount, monitoredClasses.size());
			//Should have added it and maintained the existing ones
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptDescription.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
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
		Set<Class<?>> unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
		int originalCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.startMonitoring(EncounterType.class);
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
			Assert.assertEquals(--originalCount, unMonitoredClasses.size());
			//Should have removed it and maintained the existing ones
			Assert.assertFalse(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
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
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(5, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
		
		Set<Class<?>> unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.startMonitoring(EncounterType.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClasses();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
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
	@Verifies(value = "should not update any global property if the strategy is none", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsNone() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(5, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
		
		Set<Class<?>> unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.NONE.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.startMonitoring(EncounterType.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClasses();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
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
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(5, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
		
		Set<Class<?>> unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClasses();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
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
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		int originalMonitoredCount = monitoredClasses.size();
		Assert.assertEquals(5, monitoredClasses.size());
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
		
		Set<Class<?>> unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
		int originalUnMonitoredCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.NONE.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			
			//Should not have changed
			monitoredClasses = AuditLogUtil.getMonitoredClasses();
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
			Assert.assertEquals(originalMonitoredCount, monitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
			
			Assert.assertEquals(originalUnMonitoredCount, unMonitoredClasses.size());
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
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
		setMonitoringStrategy(MonitoringStrategy.NONE_EXCEPT);
		Assert.assertEquals(MonitoringStrategy.NONE_EXCEPT, AuditLogUtil.getMonitoringStrategy());
		
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		int originalCount = monitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
		Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
		
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			
			monitoredClasses = AuditLogUtil.getMonitoredClasses();
			Assert.assertEquals(originalCount -= 3, monitoredClasses.size());
			//Should have added it and maintained the existing ones
			Assert.assertFalse(OpenmrsUtil.collectionContains(monitoredClasses, Concept.class));
			Assert.assertFalse(OpenmrsUtil.collectionContains(monitoredClasses, ConceptNumeric.class));
			Assert.assertFalse(OpenmrsUtil.collectionContains(monitoredClasses, ConceptComplex.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, EncounterType.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(monitoredClasses, PatientIdentifierType.class));
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
		Set<Class<?>> unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
		int originalCount = unMonitoredClasses.size();
		Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
		Assert.assertFalse(OpenmrsUtil.collectionContains(unMonitoredClasses, Concept.class));
		Assert.assertFalse(OpenmrsUtil.collectionContains(unMonitoredClasses, ConceptNumeric.class));
		Assert.assertFalse(OpenmrsUtil.collectionContains(unMonitoredClasses, ConceptComplex.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		String originalStrategy = gp.getPropertyValue();
		gp.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		try {
			AuditLogUtil.stopMonitoring(Concept.class);
			unMonitoredClasses = AuditLogUtil.getUnMonitoredClasses();
			Assert.assertEquals(originalCount += 3, unMonitoredClasses.size());
			//Should have removed it and maintained the existing ones
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, EncounterType.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, Concept.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, ConceptNumeric.class));
			Assert.assertTrue(OpenmrsUtil.collectionContains(unMonitoredClasses, ConceptComplex.class));
		}
		finally {
			//reset
			AuditLogUtil.startMonitoring(Concept.class);
			//reset
			gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#getPersistentConcreteSubclasses(Class<OpenmrsObject>)}
	 */
	@Test
	@Verifies(value = "should return a list of subclasses for the specified type", method = "getPersistentConcreteSubclasses(Class<OpenmrsObject>)")
	public void getPersistentConcreteSubclasses_shouldReturnAListOfSubclassesForTheSpecifiedType() throws Exception {
		Set<Class<?>> subclasses = AuditLogUtil.getPersistentConcreteSubclasses(Concept.class, null, null);
		Assert.assertEquals(2, subclasses.size());
		Assert.assertTrue(subclasses.contains(ConceptNumeric.class));
		Assert.assertTrue(subclasses.contains(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogUtil#getPersistentConcreteSubclasses(List<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should exclude interfaces and abstract classes", method = "getPersistentConcreteSubclasses(List<Class<OpenmrsObject>>)")
	public void getPersistentConcreteSubclasses_shouldExcludeInterfacesAndAbstractClasses() throws Exception {
		Set<Class<?>> subclasses = AuditLogUtil.getPersistentConcreteSubclasses(OpenmrsObject.class, null, null);
		for (Class<?> clazz : subclasses) {
			Assert.assertFalse("Found interface:" + clazz.getName() + ", interfaces should be excluded",
			    Modifier.isInterface(clazz.getModifiers()));
			Assert.assertFalse("Found abstract class:" + clazz.getName() + ", abstract classes should be excluded",
			    Modifier.isAbstract(clazz.getModifiers()));
		}
	}
	
	/**
	 * @see {@link AuditLogUtil#startMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as monitored", method = "startMonitoring(Set<Class<OpenmrsObject>>)")
	public void startMonitoring_shouldMarkAClassAndItsKnownSubclassesAsMonitored() throws Exception {
		setMonitoringStrategy(MonitoringStrategy.NONE_EXCEPT);
		Assert.assertEquals(MonitoringStrategy.NONE_EXCEPT, AuditLogUtil.getMonitoringStrategy());
		
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		Assert.assertFalse(monitoredClasses.contains(Concept.class));
		Assert.assertFalse(monitoredClasses.contains(ConceptNumeric.class));
		Assert.assertFalse(monitoredClasses.contains(ConceptComplex.class));
		
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(Concept.class);
		AuditLogUtil.startMonitoring(classes);
		monitoredClasses = AuditLogUtil.getMonitoredClasses();
		Assert.assertTrue(monitoredClasses.contains(Concept.class));
		Assert.assertTrue(monitoredClasses.contains(ConceptNumeric.class));
		Assert.assertTrue(monitoredClasses.contains(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogUtil#stopMonitoring(Set<Class<OpenmrsObject>>)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as un monitored", method = "stopMonitoring(Set<Class<OpenmrsObject>>)")
	public void stopMonitoring_shouldMarkAClassAndItsKnownSubclassesAsUnMonitored() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		setMonitoringStrategy(MonitoringStrategy.NONE_EXCEPT);
		Assert.assertEquals(MonitoringStrategy.NONE_EXCEPT, AuditLogUtil.getMonitoringStrategy());
		
		AuditLogUtil.startMonitoring(Concept.class);
		Set<Class<?>> monitoredClasses = AuditLogUtil.getMonitoredClasses();
		Assert.assertTrue(monitoredClasses.contains(Concept.class));
		Assert.assertTrue(monitoredClasses.contains(ConceptNumeric.class));
		Assert.assertTrue(monitoredClasses.contains(ConceptComplex.class));
		
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(Concept.class);
		AuditLogUtil.stopMonitoring(classes);
		Assert.assertFalse(monitoredClasses.contains(Concept.class));
		Assert.assertFalse(monitoredClasses.contains(ConceptNumeric.class));
		Assert.assertFalse(monitoredClasses.contains(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogUtil#getImplicitlyMonitoredClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of implicitly monitored classes", method = "getImplicitlyMonitoredClassNames()")
	public void getImplicitlyMonitoredClasses_shouldReturnASetOfImplicitlyMonitoredClasses() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		as.saveGlobalProperty(new GlobalProperty(AuditLogConstants.GP_MONITORING_STRATEGY, MonitoringStrategy.NONE_EXCEPT
		        .name()));
		AuditLogUtil.startMonitoring(Concept.class);
		Set<Class<?>> implicitlyMonitoredClasses = AuditLogUtil.getImplicitlyMonitoredClasses();
		Assert.assertEquals(6, implicitlyMonitoredClasses.size());
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptName.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptDescription.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptMap.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptSet.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptAnswer.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptNameTag.class));
	}
}
