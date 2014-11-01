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
package org.openmrs.module.auditlog.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.AuditingStrategy;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsUtil;

/**
 * Contains tests for methods in {@link AuditLogService}
 */
public class AuditLogServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	private static final String MODULE_TEST_DATA_AUDIT_LOGS = "moduleTestData-initialAuditLogs.xml";
	
	private AuditLogService service;
	
	@Before
	public void before() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		service = Context.getService(AuditLogService.class);
		setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, AuditingStrategy.NONE_EXCEPT.name());
		assertEquals(AuditingStrategy.NONE_EXCEPT, service.getAuditingStrategy());
	}
	
	private List<AuditLog> getAllAuditLogs() {
		return service.getAuditLogs(null, null, null, null, false, null, null);
	}
	
	private void setGlobalProperty(String property, String propertyValue) throws Exception {
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(property);
		if (gp == null) {
			gp = new GlobalProperty(property, propertyValue);
		} else {
			gp.setPropertyValue(propertyValue);
		}
		as.saveGlobalProperty(gp);
	}
	
	private AuditLogDAO getAuditLogDAO() {
		return Context.getRegisteredComponents(AuditLogDAO.class).get(0);
	}
	
	/**
	 * @see {@link AuditLogService#getObjectById(Class, Integer)}
	 */
	@Test
	@Verifies(value = "should get the saved object matching the specified arguments", method = "get(Class<T>,Integer)")
	public void getObjectById_shouldGetTheSavedObjectMatchingTheSpecifiedArguments() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		AuditLog al = service.getObjectById(AuditLog.class, 1);
		assertEquals("4f7d57f0-9077-11e1-aaa4-00248140a5eb", al.getUuid());
		
		//check the child logs
		assertEquals(2, al.getChildAuditLogs().size());
		String[] childUuids = new String[2];
		int index = 0;
		for (AuditLog child : al.getChildAuditLogs()) {
			childUuids[index] = child.getUuid();
			assertEquals(al, child.getParentAuditLog());
			index++;
		}
		assertTrue(ArrayUtils.contains(childUuids, "5f7d57f0-9077-11e1-aaa4-00248140a5ef"));
		assertTrue(ArrayUtils.contains(childUuids, "6f7d57f0-9077-11e1-aaa4-00248140a5ef"));
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should match on the specified audit log actions", method = "getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldMatchOnTheSpecifiedAuditLogActions() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Action> actions = new ArrayList<Action>();
		actions.add(Action.CREATED);//get only inserts
		assertEquals(3, service.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.add(Action.UPDATED);//get both insert and update logs
		assertEquals(5, service.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.clear();
		actions.add(Action.UPDATED);//get only updates
		assertEquals(2, service.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.clear();
		actions.add(Action.DELETED);//get only deletes
		assertEquals(1, service.getAuditLogs(null, actions, null, null, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should return all audit logs in the database if all args are null", method = "getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnAllAuditLogsInTheDatabaseIfAllArgsAreNull() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(6, getAllAuditLogs().size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should match on the specified classes", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldMatchOnTheSpecifiedClasses() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Class<? extends OpenmrsObject>> clazzes = new ArrayList<Class<? extends OpenmrsObject>>();
		clazzes.add(Concept.class);
		assertEquals(3, service.getAuditLogs(clazzes, null, null, null, false, null, null).size());
		clazzes.add(ConceptName.class);
		assertEquals(4, service.getAuditLogs(clazzes, null, null, null, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created on or after the specified startDate", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedOnOrAfterTheSpecifiedStartDate() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(2012, Calendar.APRIL, 1, 0, 1, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date startDate = cal.getTime();
		assertEquals(3, service.getAuditLogs(null, null, startDate, null, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created on or before the specified endDate", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedOnOrBeforeTheSpecifiedEndDate() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(2012, Calendar.APRIL, 1, 0, 3, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date endDate = cal.getTime();
		assertEquals(5, service.getAuditLogs(null, null, null, endDate, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created within the specified start and end dates", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedWithinTheSpecifiedStartAndEndDates() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(2012, Calendar.APRIL, 1, 0, 0, 1);
		Date startDate = cal.getTime();
		cal.set(2012, Calendar.APRIL, 1, 0, 3, 1);
		Date endDate = cal.getTime();
		assertEquals(2, service.getAuditLogs(null, null, startDate, endDate, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test(expected = APIException.class)
	@Verifies(value = "should reject a start date that is in the future", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldRejectAStartDateThatIsInTheFuture() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 1);
		Date startDate = cal.getTime();
		service.getAuditLogs(null, null, startDate, null, false, null, null);
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should ignore end date it it is in the future", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldIgnoreEndDateItItIsInTheFuture() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 1);
		Date endDate = cal.getTime();
		assertEquals(6, service.getAuditLogs(null, null, null, endDate, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should sort the logs by date of creation starting with the latest", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldSortTheLogsByDateOfCreationStartingWithTheLatest() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<AuditLog> auditLogs = getAllAuditLogs();
		assertFalse(auditLogs.isEmpty());
		Date currMaxDate = auditLogs.get(0).getDateCreated();
		for (AuditLog auditLog : auditLogs) {
			assertTrue(OpenmrsUtil.compare(currMaxDate, auditLog.getDateCreated()) >= 0);
		}
	}
	
	/**
	 * @see {@link AuditLogService#getObjectByUuid(Class, String)}
	 */
	@Test
	@Verifies(value = "should get the saved object matching the specified arguments", method = "getObjectByUuid(Class<T>,String)")
	public void getObjectByUuid_shouldGetTheSavedObjectMatchingTheSpecifiedArguments() throws Exception {
		assertNull(service.getObjectByUuid(GlobalProperty.class, "Unknown uuid"));
		GlobalProperty gp = service.getObjectByUuid(GlobalProperty.class, "abc05786-9019-11e1-aaa4-00248140a5eb");
		assertNotNull(gp);
		assertEquals(AuditLogConstants.GP_AUDITING_STRATEGY, gp.getProperty());
		
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should include logs for subclasses when getting logs by type", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldIncludeLogsForSubclassesWhenGettingLogsByType() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Class<? extends OpenmrsObject>> clazzes = new ArrayList<Class<? extends OpenmrsObject>>();
		clazzes.add(OpenmrsObject.class);
		assertEquals(6, service.getAuditLogs(clazzes, null, null, null, false, null, null).size());
		clazzes.clear();
		clazzes.add(Concept.class);
		assertEquals(3, service.getAuditLogs(clazzes, null, null, null, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditedClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of audited classes", method = "getAuditedClasses()")
	public void getAuditedClasses_shouldReturnASetOfAuditedClasses() throws Exception {
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		assertEquals(5, auditedClasses.size());
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
	}
	
	/**
	 * @see {@link AuditLogService#getUnAuditedClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of un audited classes", method = "getUnAuditedClasses()")
	public void getUnAuditedClasses_shouldReturnASetOfUnAuditedClasses() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		//In case previous tests changed the value
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_UN_AUDITED_CLASSES);
		gp.setPropertyValue(EncounterType.class.getName());
		as.saveGlobalProperty(gp);
		Set<Class<? extends OpenmrsObject>> unAuditedClasses = service.getUnAuditedClasses();
		assertEquals(1, unAuditedClasses.size());
		assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the audited class names global property if the strategy is none_except", method = "startAuditing(Set<Class<OpenmrsObject>>)")
	public void startAuditing_shouldUpdateTheAuditedClassNamesGlobalPropertyIfTheStrategyIsNone_except() throws Exception {
		assertEquals(AuditingStrategy.NONE_EXCEPT, service.getAuditingStrategy());
		
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		int originalCount = auditedClasses.size();
		assertFalse(OpenmrsUtil.collectionContains(auditedClasses, ConceptDescription.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		
		try {
			service.startAuditing(ConceptDescription.class);
			
			auditedClasses = service.getAuditedClasses();
			assertEquals(++originalCount, auditedClasses.size());
			//Should have added it and maintained the existing ones
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptDescription.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		}
		finally {
			//reset
			service.stopAuditing(ConceptDescription.class);
		}
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should not update any global property if the strategy is all", method = "startAuditing(Set<Class<OpenmrsObject>>)")
	public void startAuditing_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsAll() throws Exception {
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		int originalAuditedCount = auditedClasses.size();
		assertEquals(5, auditedClasses.size());
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		
		Set<Class<? extends OpenmrsObject>> unAuditedClasses = service.getUnAuditedClasses();
		int originalUnAuditedCount = unAuditedClasses.size();
		assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		gp.setPropertyValue(AuditingStrategy.ALL.name());
		as.saveGlobalProperty(gp);
		try {
			service.startAuditing(EncounterType.class);
			
			//Should not have changed
			auditedClasses = service.getAuditedClasses();
			unAuditedClasses = service.getUnAuditedClasses();
			assertEquals(originalAuditedCount, auditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
			
			assertEquals(originalUnAuditedCount, unAuditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		}
		finally {
			//reset
			service.stopAuditing(EncounterType.class);
			//gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	//@Test
	@Verifies(value = "should not update any global property if the strategy is none", method = "startAuditing(Set<Class<OpenmrsObject>>)")
	public void startAuditing_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsNone() throws Exception {
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		int originalAuditedCount = auditedClasses.size();
		assertEquals(5, auditedClasses.size());
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		
		Set<Class<? extends OpenmrsObject>> unAuditedClasses = service.getUnAuditedClasses();
		int originalUnAuditedCount = unAuditedClasses.size();
		assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		gp.setPropertyValue(AuditingStrategy.NONE.name());
		as.saveGlobalProperty(gp);
		try {
			service.startAuditing(EncounterType.class);
			
			//Should not have changed
			auditedClasses = service.getAuditedClasses();
			unAuditedClasses = service.getUnAuditedClasses();
			assertEquals(originalAuditedCount, auditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
			
			assertEquals(originalUnAuditedCount, unAuditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		}
		finally {
			service.stopAuditing(EncounterType.class);
			//gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the un audited class names global property if the strategy is all_except", method = "startAuditing(Set<Class<OpenmrsObject>>)")
	public void startAuditing_shouldUpdateTheUnAuditedClassNamesGlobalPropertyIfTheStrategyIsAll_except() throws Exception {
		Set<Class<? extends OpenmrsObject>> unAuditedClasses = service.getUnAuditedClasses();
		int originalCount = unAuditedClasses.size();
		assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		assertFalse(OpenmrsUtil.collectionContains(unAuditedClasses, Concept.class));
		assertFalse(OpenmrsUtil.collectionContains(unAuditedClasses, ConceptNumeric.class));
		assertFalse(OpenmrsUtil.collectionContains(unAuditedClasses, ConceptComplex.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		gp.setPropertyValue(AuditingStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		try {
			service.stopAuditing(Concept.class);
			unAuditedClasses = service.getUnAuditedClasses();
			assertEquals(originalCount += 3, unAuditedClasses.size());
			//Should have removed it and maintained the existing ones
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, Concept.class));
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, ConceptNumeric.class));
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, ConceptComplex.class));
		}
		finally {
			//reset
			service.startAuditing(Concept.class);
			//reset
			//gp.setPropertyValue(originalStrategy);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as audited", method = "startAuditing(Set<Class<OpenmrsObject>>)")
	public void startAuditing_shouldMarkAClassAndItsKnownSubclassesAsAudited() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		as.purgeGlobalProperty(as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITED_CLASSES));
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		assertFalse(auditedClasses.contains(Order.class));
		assertFalse(auditedClasses.contains(DrugOrder.class));
		
		service.startAuditing(Order.class);
		auditedClasses = service.getAuditedClasses();
		assertTrue(auditedClasses.contains(Order.class));
		assertTrue(auditedClasses.contains(DrugOrder.class));
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the audited class names global property if the strategy is none_except", method = "stopAuditing(Set<Class<OpenmrsObject>>)")
	public void stopAuditing_shouldUpdateTheAuditedClassNamesGlobalPropertyIfTheStrategyIsNone_except() throws Exception {
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		int originalCount = auditedClasses.size();
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		
		try {
			service.stopAuditing(Concept.class);
			
			auditedClasses = service.getAuditedClasses();
			assertEquals(originalCount -= 3, auditedClasses.size());
			//Should have added it and maintained the existing ones
			assertFalse(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
			assertFalse(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
			assertFalse(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		}
		finally {
			//reset
			service.startAuditing(Concept.class);
		}
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should not update any global property if the strategy is all", method = "stopAuditing(Set<Class<OpenmrsObject>>)")
	public void stopAuditing_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsAll() throws Exception {
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		int originalAuditedCount = auditedClasses.size();
		assertEquals(5, auditedClasses.size());
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		
		Set<Class<? extends OpenmrsObject>> unAuditedClasses = service.getUnAuditedClasses();
		int originalUnAuditedCount = unAuditedClasses.size();
		assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		gp.setPropertyValue(AuditingStrategy.ALL.name());
		as.saveGlobalProperty(gp);
		try {
			service.stopAuditing(Concept.class);
			
			//Should not have changed
			auditedClasses = service.getAuditedClasses();
			unAuditedClasses = service.getUnAuditedClasses();
			assertEquals(originalAuditedCount, auditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
			
			assertEquals(originalUnAuditedCount, unAuditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		}
		finally {
			//reset
			service.startAuditing(Concept.class);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	//@Test
	@Verifies(value = "should not update any global property if the strategy is none", method = "stopAuditing(Set<Class<OpenmrsObject>>)")
	public void stopAuditing_shouldNotUpdateAnyGlobalPropertyIfTheStrategyIsNone() throws Exception {
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		int originalAuditedCount = auditedClasses.size();
		assertEquals(5, auditedClasses.size());
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
		
		Set<Class<? extends OpenmrsObject>> unAuditedClasses = service.getUnAuditedClasses();
		int originalUnAuditedCount = unAuditedClasses.size();
		assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		gp.setPropertyValue(AuditingStrategy.NONE.name());
		as.saveGlobalProperty(gp);
		try {
			service.stopAuditing(Concept.class);
			
			//Should not have changed
			auditedClasses = service.getAuditedClasses();
			unAuditedClasses = service.getUnAuditedClasses();
			assertEquals(originalAuditedCount, auditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, Concept.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptNumeric.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, ConceptComplex.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, EncounterType.class));
			assertTrue(OpenmrsUtil.collectionContains(auditedClasses, PatientIdentifierType.class));
			
			assertEquals(originalUnAuditedCount, unAuditedClasses.size());
			assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		}
		finally {
			service.startAuditing(Concept.class);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the un audited class names global property if the strategy is all_except", method = "stopAuditing(Set<Class<OpenmrsObject>>)")
	public void stopAuditing_shouldUpdateTheUnAuditedClassNamesGlobalPropertyIfTheStrategyIsAll_except() throws Exception {
		Set<Class<? extends OpenmrsObject>> unAuditedClasses = service.getUnAuditedClasses();
		int originalCount = unAuditedClasses.size();
		assertTrue(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		gp.setPropertyValue(AuditingStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		try {
			service.startAuditing(EncounterType.class);
			unAuditedClasses = service.getUnAuditedClasses();
			assertEquals(--originalCount, unAuditedClasses.size());
			//Should have removed it and maintained the existing ones
			assertFalse(OpenmrsUtil.collectionContains(unAuditedClasses, EncounterType.class));
		}
		finally {
			//reset
			service.stopAuditing(EncounterType.class);
			as.saveGlobalProperty(gp);
		}
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as un audited", method = "stopAuditing(Set<Class<OpenmrsObject>>)")
	public void stopAuditing_shouldMarkAClassAndItsKnownSubclassesAsUnAudited() throws Exception {
		service.startAuditing(Concept.class);
		Set<Class<? extends OpenmrsObject>> auditedClasses = service.getAuditedClasses();
		assertTrue(auditedClasses.contains(Concept.class));
		assertTrue(auditedClasses.contains(ConceptNumeric.class));
		assertTrue(auditedClasses.contains(ConceptComplex.class));
		
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(Concept.class);
		service.stopAuditing(classes);
		assertFalse(auditedClasses.contains(Concept.class));
		assertFalse(auditedClasses.contains(ConceptNumeric.class));
		assertFalse(auditedClasses.contains(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should get all logs for the object matching the specified uuid", method = "getAuditLogs(String,Class<OpenmrsObject>,List<Action>,Date,Date)")
	public void getAuditLogs_shouldGetAllLogsForTheObjectMatchingTheSpecifiedUuid() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(2,
		    service.getAuditLogs("c607c80f-1ea9-4da3-bb88-6276ce8868dd", ConceptNumeric.class, null, null, null, false)
		            .size());
	}
	
	/**
	 * @see {@link AuditLogService#isAudited(Class)}
	 */
	@Test
	@Verifies(value = "should true if the class is audited", method = "isAudited(Class<*>)")
	public void isAudited_shouldTrueIfTheClassIsAudited() throws Exception {
		assertTrue(service.isAudited(Concept.class));
		assertTrue(service.isAudited(ConceptNumeric.class));
		assertTrue(service.isAudited(EncounterType.class));
		assertTrue(service.isAudited(PatientIdentifierType.class));
		
		AuditingStrategy newStrategy = AuditingStrategy.ALL_EXCEPT;
		setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, newStrategy.name());
		assertEquals(newStrategy, service.getAuditingStrategy());
		
		assertTrue(service.isAudited(Concept.class));
		assertTrue(service.isAudited(ConceptNumeric.class));
		assertTrue(service.isAudited(PatientIdentifierType.class));
		assertTrue(service.isAudited(Cohort.class));
	}
	
	/**
	 * @see {@link AuditLogService#isAudited(Class)}
	 */
	@Test
	@Verifies(value = "should false if the class is not audited", method = "isAudited(Class<*>)")
	public void isAudited_shouldFalseIfTheClassIsNotAudited() throws Exception {
		assertFalse(service.isAudited(Cohort.class));
		
		AuditingStrategy newStrategy = AuditingStrategy.ALL_EXCEPT;
		setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, newStrategy.name());
		assertEquals(newStrategy, service.getAuditingStrategy());
		
		assertFalse(service.isAudited(EncounterType.class));
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as audited for all_except strategy", method = "startAuditing(Set<Class<OpenmrsObject>>)")
	public void startAuditing_shouldMarkAClassAndItsKnownSubclassesAsAuditedForAll_exceptStrategy() throws Exception {
		AuditingStrategy newStrategy = AuditingStrategy.ALL_EXCEPT;
		setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, newStrategy.name());
		assertEquals(newStrategy, service.getAuditingStrategy());
		assertTrue(service.isAudited(Order.class));
		assertTrue(service.isAudited(DrugOrder.class));
		//mark orders as un audited for test purposes
		service.stopAuditing(Order.class);
		assertFalse(service.isAudited(Order.class));
		assertFalse(service.isAudited(DrugOrder.class));
		
		service.startAuditing(Order.class);
		assertTrue(service.isAudited(Order.class));
		assertTrue(service.isAudited(DrugOrder.class));
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(Class)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as un audited for all_except strategy", method = "stopAuditing(Set<Class<OpenmrsObject>>)")
	public void stopAuditing_shouldMarkAClassAndItsKnownSubclassesAsUnAuditedForAll_exceptStrategy() throws Exception {
		AuditingStrategy newStrategy = AuditingStrategy.ALL_EXCEPT;
		setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, newStrategy.name());
		assertEquals(newStrategy, service.getAuditingStrategy());
		assertTrue(service.isAudited(Order.class));
		assertTrue(service.isAudited(DrugOrder.class));
		
		service.stopAuditing(Order.class);
		assertFalse(service.isAudited(Order.class));
		assertFalse(service.isAudited(DrugOrder.class));
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(String, Class, java.util.List, java.util.Date, java.util.Date, boolean)}
	 */
	@Test
	@Verifies(value = "should include logs for subclasses when getting by type", method = "getAuditLogs(String,Class<OpenmrsObject>,List<Action>,Date,Date)")
	public void getAuditLogs_shouldIncludeLogsForSubclassesWhenGettingByType() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(2, service.getAuditLogs("c607c80f-1ea9-4da3-bb88-6276ce8868dd", Concept.class, null, null, null, false)
		        .size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should exclude child logs if excludeChildAuditLogsis set to true", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,null,Integer,Integer)")
	public void getAuditLogs_shouldExcludeChildLogsIfExcludeChildAuditLogsisSetToTrue() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(4, service.getAuditLogs(null, null, null, null, true, null, null).size());
	}
	
	/**
	 * @verifies exclude child logs for object if excludeChildAuditLogs is set to true
	 * @see AuditLogService#getAuditLogs(String, Class, java.util.List, java.util.Date,
	 *      java.util.Date, boolean)
	 */
	@Test
	public void getAuditLogs_shouldExcludeChildLogsForObjectIfExcludeChildAuditLogsIsSetToTrue() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(0,
		    service.getAuditLogs("d607c80f-1ea9-4da3-bb88-6276ce8868de", ConceptDescription.class, null, null, null, true)
		            .size());
	}
	
	/**
	 * @verifies get all logs for the specified object
	 * @see AuditLogService#getAuditLogs(org.openmrs.OpenmrsObject, java.util.List, java.util.Date,
	 *      java.util.Date, boolean)
	 */
	@Test
	public void getAuditLogs_shouldGetAllLogsForTheSpecifiedObject() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		OpenmrsObject obj = service.getObjectByUuid(ConceptNumeric.class, "c607c80f-1ea9-4da3-bb88-6276ce8868dd");
		assertEquals(2, service.getAuditLogs(obj, null, null, null, false).size());
	}
	
	/**
	 * @verifies also mark association types as audited
	 * @see AuditLogService#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldAlsoMarkAssociationTypesAsAudited() throws Exception {
		assertFalse(service.isAudited(Person.class));
		assertFalse(service.isAudited(PersonName.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(PersonName.class));
		
		service.startAuditing(Person.class);
		
		assertTrue(service.isAudited(Person.class));
		assertFalse(service.isAudited(PersonName.class));
		assertTrue(getAuditLogDAO().isImplicitlyAudited(PersonName.class));
	}
	
	/**
	 * @verifies not mark association types for many to many collections as audited
	 * @see AuditLogService#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldNotMarkAssociationTypesForManyToManyCollectionsAsAudited() throws Exception {
		assertFalse(service.isAudited(Location.class));
		assertFalse(service.isAudited(LocationTag.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(LocationTag.class));
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("tags", Location.class, null);
		Assert.assertTrue(cp.isManyToMany());
		
		service.startAuditing(Location.class);
		
		assertTrue(service.isAudited(Location.class));
		assertFalse(service.isAudited(LocationTag.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(LocationTag.class));
	}
	
	/**
	 * @verifies remove association types from audited classes
	 * @see AuditLogService#stopAuditing(java.util.Set>)
	 */
	@Test
	public void stopAuditing_shouldRemoveAssociationTypesFromAuditedClasses() throws Exception {
		assertTrue(service.isAudited(Concept.class));
		assertFalse(service.isAudited(ConceptName.class));
		assertTrue(getAuditLogDAO().isImplicitlyAudited(ConceptName.class));
		
		service.stopAuditing(Concept.class);
		
		assertFalse(service.isAudited(Concept.class));
		assertFalse(service.isAudited(ConceptName.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(ConceptName.class));
	}
}
