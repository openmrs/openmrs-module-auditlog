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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterType;
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
import org.openmrs.module.auditlog.BaseAuditLogTest;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsUtil;

/**
 * Contains tests for methods in {@link AuditLogService}
 */
public class AuditLogServiceTest extends BaseAuditLogTest {
	
	private static final String MODULE_TEST_DATA_AUDIT_LOGS = "moduleTestData-initialAuditLogs.xml";
	
	private static final String EXCEPTIONS_FOR_ALL_EXCEPT = "org.openmrs.Concept, org.openmrs.EncounterType";
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	private List<AuditLog> getAllAuditLogs() {
		return auditLogService.getAuditLogs(null, null, null, null, false, null, null);
	}
	
	private AuditLogDAO getAuditLogDAO() {
		return Context.getRegisteredComponents(AuditLogDAO.class).get(0);
	}
	
	private void setAuditConfiguration(AuditingStrategy strategy) throws Exception {
		setAuditConfiguration(strategy, null, false);
	}
	
	/**
	 * @see {@link AuditLogService#getObjectById(Class, Integer)}
	 */
	@Test
	@Verifies(value = "should get the saved object matching the specified arguments", method = "get(Class<T>,Integer)")
	public void getObjectById_shouldGetTheSavedObjectMatchingTheSpecifiedArguments() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		AuditLog al = auditLogService.getObjectById(AuditLog.class, 1);
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
		assertEquals(3, auditLogService.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.add(Action.UPDATED);//get both insert and update logs
		assertEquals(5, auditLogService.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.clear();
		actions.add(Action.UPDATED);//get only updates
		assertEquals(2, auditLogService.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.clear();
		actions.add(Action.DELETED);//get only deletes
		assertEquals(1, auditLogService.getAuditLogs(null, actions, null, null, false, null, null).size());
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
	@Verifies(value = "should match on the specified classes", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldMatchOnTheSpecifiedClasses() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Class<?>> clazzes = new ArrayList<Class<?>>();
		clazzes.add(Concept.class);
		assertEquals(3, auditLogService.getAuditLogs(clazzes, null, null, null, false, null, null).size());
		clazzes.add(ConceptName.class);
		assertEquals(4, auditLogService.getAuditLogs(clazzes, null, null, null, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created on or after the specified startDate", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedOnOrAfterTheSpecifiedStartDate() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(2012, Calendar.APRIL, 1, 0, 1, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date startDate = cal.getTime();
		assertEquals(3, auditLogService.getAuditLogs(null, null, startDate, null, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created on or before the specified endDate", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedOnOrBeforeTheSpecifiedEndDate() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(2012, Calendar.APRIL, 1, 0, 3, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date endDate = cal.getTime();
		assertEquals(5, auditLogService.getAuditLogs(null, null, null, endDate, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created within the specified start and end dates", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedWithinTheSpecifiedStartAndEndDates() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(2012, Calendar.APRIL, 1, 0, 0, 1);
		Date startDate = cal.getTime();
		cal.set(2012, Calendar.APRIL, 1, 0, 3, 1);
		Date endDate = cal.getTime();
		assertEquals(2, auditLogService.getAuditLogs(null, null, startDate, endDate, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test(expected = APIException.class)
	@Verifies(value = "should reject a start date that is in the future", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldRejectAStartDateThatIsInTheFuture() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 1);
		Date startDate = cal.getTime();
		auditLogService.getAuditLogs(null, null, startDate, null, false, null, null);
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should ignore end date it it is in the future", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldIgnoreEndDateItItIsInTheFuture() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 1);
		Date endDate = cal.getTime();
		assertEquals(6, auditLogService.getAuditLogs(null, null, null, endDate, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should sort the logs by date of creation starting with the latest", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
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
		assertNull(auditLogService.getObjectByUuid(Location.class, "Unknown uuid"));
		Location description = auditLogService.getObjectByUuid(Location.class, "dc5c1fcc-0459-4201-bf70-0b90535ba362");
		assertNotNull(description);
		assertEquals(1, description.getId().intValue());
		
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should include logs for subclasses when getting logs by type", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldIncludeLogsForSubclassesWhenGettingLogsByType() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Class<?>> clazzes = new ArrayList<Class<?>>();
		clazzes.add(OpenmrsObject.class);
		assertEquals(6, auditLogService.getAuditLogs(clazzes, null, null, null, false, null, null).size());
		clazzes.clear();
		clazzes.add(Concept.class);
		assertEquals(3, auditLogService.getAuditLogs(clazzes, null, null, null, false, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getExceptions()}
	 */
	@Test
	@Verifies(value = "should return a set of exception classes", method = "getAuditedClasses()")
	public void getExceptions_shouldReturnASetOfExceptionClasses() throws Exception {
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		assertEquals(5, exceptions.size());
		assertTrue(OpenmrsUtil.collectionContains(exceptions, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptComplex.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, PatientIdentifierType.class));
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the exception class names global property if the strategy is none_except", method = "startAuditing(Set<Class<?>>)")
	public void startAuditing_shouldUpdateTheExceptionClassNamesGlobalPropertyIfTheStrategyIsNone_except() throws Exception {
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		int originalCount = exceptions.size();
		assertFalse(auditLogService.isAudited(ConceptDescription.class));
		assertTrue(auditLogService.isAudited(Concept.class));
		assertTrue(auditLogService.isAudited(ConceptNumeric.class));
		assertTrue(auditLogService.isAudited(ConceptComplex.class));
		assertTrue(auditLogService.isAudited(EncounterType.class));
		assertTrue(auditLogService.isAudited(PatientIdentifierType.class));
		
		auditLogService.startAuditing(ConceptDescription.class);
		
		exceptions = auditLogService.getExceptions();
		assertEquals(++originalCount, exceptions.size());
		//Should have added it and maintained the existing ones
		assertTrue(auditLogService.isAudited(ConceptDescription.class));
		assertTrue(auditLogService.isAudited(Concept.class));
		assertTrue(auditLogService.isAudited(ConceptNumeric.class));
		assertTrue(auditLogService.isAudited(ConceptComplex.class));
		assertTrue(auditLogService.isAudited(EncounterType.class));
		assertTrue(auditLogService.isAudited(PatientIdentifierType.class));
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should fail if the strategy is set to all", method = "startAuditing(Set<Class<?>>)")
	public void startAuditing_shouldFailIfTheStrategyIsSetToAll() throws Exception {
		setAuditConfiguration(AuditingStrategy.ALL);
		expectedException.expect(APIException.class);
		expectedException.expectMessage("Can't call AuditLogService.startAuditing when the Audit strategy is set to "
		        + AuditingStrategy.NONE + " or " + AuditingStrategy.ALL);
		auditLogService.startAuditing(EncounterType.class);
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should fail if the strategy is set to none", method = "startAuditing(Set<Class<?>>)")
	public void startAuditing_shouldFailIfTheStrategyIsSetToNone() throws Exception {
		setAuditConfiguration(AuditingStrategy.NONE);
		expectedException.expect(APIException.class);
		expectedException.expectMessage("Can't call AuditLogService.startAuditing when the Audit strategy is set to "
		        + AuditingStrategy.NONE + " or " + AuditingStrategy.ALL);
		auditLogService.startAuditing(EncounterType.class);
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the exception class names global property if the strategy is all_except", method = "startAuditing(Set<Class<?>>)")
	public void startAuditing_shouldUpdateTheExceptionClassNamesGlobalPropertyIfTheStrategyIsAll_except() throws Exception {
		setAuditConfiguration(AuditingStrategy.ALL_EXCEPT, EXCEPTIONS_FOR_ALL_EXCEPT, false);
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		int originalCount = exceptions.size();
		assertTrue(OpenmrsUtil.collectionContains(exceptions, EncounterType.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, Concept.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptNumeric.class));
		assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptComplex.class));
		assertEquals(false, auditLogService.isAudited(EncounterType.class));
		assertEquals(false, auditLogService.isAudited(Concept.class));
		assertEquals(false, auditLogService.isAudited(ConceptNumeric.class));
		assertEquals(false, auditLogService.isAudited(ConceptComplex.class));
		auditLogService.startAuditing(Concept.class);
		exceptions = auditLogService.getExceptions();
		assertEquals(originalCount -= 3, exceptions.size());
		//Should have removed it and maintained the existing ones
		assertTrue(OpenmrsUtil.collectionContains(exceptions, EncounterType.class));
		assertFalse(OpenmrsUtil.collectionContains(exceptions, Concept.class));
		assertFalse(OpenmrsUtil.collectionContains(exceptions, ConceptNumeric.class));
		assertFalse(OpenmrsUtil.collectionContains(exceptions, ConceptComplex.class));
		assertEquals(false, auditLogService.isAudited(EncounterType.class));
		assertEquals(true, auditLogService.isAudited(Concept.class));
		assertEquals(true, auditLogService.isAudited(ConceptNumeric.class));
		assertEquals(true, auditLogService.isAudited(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as audited", method = "startAuditing(Set<Class<?>>)")
	public void startAuditing_shouldMarkAClassAndItsKnownSubclassesAsAudited() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		as.purgeGlobalProperty(as.getGlobalPropertyObject(AuditLogConstants.GP_EXCEPTIONS));
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		assertFalse(exceptions.contains(Order.class));
		assertFalse(exceptions.contains(DrugOrder.class));
		assertEquals(false, auditLogService.isAudited(Order.class));
		assertEquals(false, auditLogService.isAudited(DrugOrder.class));
		
		auditLogService.startAuditing(Order.class);
		exceptions = auditLogService.getExceptions();
		assertTrue(exceptions.contains(Order.class));
		assertTrue(exceptions.contains(DrugOrder.class));
		assertEquals(true, auditLogService.isAudited(Order.class));
		assertEquals(true, auditLogService.isAudited(DrugOrder.class));
	}
	
	/**
	 * @see {@link AuditLogService#startAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as audited for all_except strategy", method = "startAuditing(Set<Class<?>>)")
	public void startAuditing_shouldMarkAClassAndItsKnownSubclassesAsAuditedForAll_exceptStrategy() throws Exception {
		setAuditConfiguration(AuditingStrategy.ALL_EXCEPT, EXCEPTIONS_FOR_ALL_EXCEPT, false);
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		assertTrue(exceptions.contains(Concept.class));
		assertTrue(exceptions.contains(ConceptNumeric.class));
		assertTrue(exceptions.contains(ConceptComplex.class));
		assertEquals(false, auditLogService.isAudited(Concept.class));
		assertEquals(false, auditLogService.isAudited(ConceptNumeric.class));
		assertEquals(false, auditLogService.isAudited(ConceptComplex.class));
		
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(Concept.class);
		auditLogService.startAuditing(classes);
		exceptions = auditLogService.getExceptions();
		assertFalse(exceptions.contains(Concept.class));
		assertFalse(exceptions.contains(ConceptNumeric.class));
		assertFalse(exceptions.contains(ConceptComplex.class));
		assertEquals(true, auditLogService.isAudited(Concept.class));
		assertEquals(true, auditLogService.isAudited(ConceptNumeric.class));
		assertEquals(true, auditLogService.isAudited(ConceptComplex.class));
	}
	
	/**
	 * @verifies also mark association types as audited
	 * @see AuditLogService#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldAlsoMarkAssociationTypesAsAudited() throws Exception {
		assertFalse(auditLogService.isAudited(Person.class));
		assertFalse(auditLogService.isAudited(PersonName.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(PersonName.class));
		
		auditLogService.startAuditing(Person.class);
		
		assertTrue(auditLogService.isAudited(Person.class));
		assertFalse(auditLogService.isAudited(PersonName.class));
		assertTrue(getAuditLogDAO().isImplicitlyAudited(PersonName.class));
	}
	
	/**
	 * @verifies not mark association types for many to many collections as audited
	 * @see AuditLogService#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldNotMarkAssociationTypesForManyToManyCollectionsAsAudited() throws Exception {
		assertFalse(auditLogService.isAudited(Location.class));
		assertFalse(auditLogService.isAudited(LocationTag.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(LocationTag.class));
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("tags", Location.class, null);
		Assert.assertTrue(cp.isManyToMany());
		
		auditLogService.startAuditing(Location.class);
		
		assertTrue(auditLogService.isAudited(Location.class));
		assertFalse(auditLogService.isAudited(LocationTag.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(LocationTag.class));
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the exception class names global property if the strategy is none_except", method = "stopAuditing(Set<Class<?>>)")
	public void stopAuditing_shouldUpdateTheExceptionClassNamesGlobalPropertyIfTheStrategyIsNone_except() throws Exception {
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		int originalCount = exceptions.size();
		assertTrue(OpenmrsUtil.collectionContains(exceptions, Concept.class));
		assertTrue(auditLogService.isAudited(Concept.class));
		assertTrue(auditLogService.isAudited(ConceptNumeric.class));
		assertTrue(auditLogService.isAudited(ConceptComplex.class));
		assertTrue(auditLogService.isAudited(EncounterType.class));
		assertTrue(auditLogService.isAudited(PatientIdentifierType.class));
		
		auditLogService.stopAuditing(Concept.class);
		
		exceptions = auditLogService.getExceptions();
		assertEquals(originalCount -= 3, exceptions.size());
		//Should have added it and maintained the existing ones
		assertFalse(auditLogService.isAudited(Concept.class));
		assertFalse(auditLogService.isAudited(ConceptNumeric.class));
		assertFalse(auditLogService.isAudited(ConceptComplex.class));
		assertTrue(auditLogService.isAudited(EncounterType.class));
		assertTrue(auditLogService.isAudited(PatientIdentifierType.class));
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should fail if the strategy is set to all", method = "stopAuditing(Set<Class<?>>)")
	public void stopAuditing_shouldFailIfTheStrategyIsSetToAll() throws Exception {
		setAuditConfiguration(AuditingStrategy.ALL);
		expectedException.expect(APIException.class);
		expectedException.expectMessage("Can't call AuditLogService.stopAuditing when the Audit strategy is set to "
		        + AuditingStrategy.NONE + " or " + AuditingStrategy.ALL);
		auditLogService.stopAuditing(Concept.class);
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should fail if the strategy is set to none", method = "stopAuditing(Set<Class<?>>)")
	public void stopAuditing_shouldFailIfTheStrategyIsSetToNone() throws Exception {
		setAuditConfiguration(AuditingStrategy.NONE);
		expectedException.expect(APIException.class);
		expectedException.expectMessage("Can't call AuditLogService.stopAuditing when the Audit strategy is set to "
		        + AuditingStrategy.NONE + " or " + AuditingStrategy.ALL);
		auditLogService.stopAuditing(Concept.class);
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should update the exception class names global property if the strategy is all_except", method = "stopAuditing(Set<Class<?>>)")
	public void stopAuditing_shouldUpdateTheExceptionClassNamesGlobalPropertyIfTheStrategyIsAll_except() throws Exception {
		setAuditConfiguration(AuditingStrategy.ALL_EXCEPT, EXCEPTIONS_FOR_ALL_EXCEPT, false);
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		int originalCount = exceptions.size();
		assertFalse(OpenmrsUtil.collectionContains(exceptions, Location.class));
		assertEquals(true, auditLogService.isAudited(Location.class));
		
		auditLogService.stopAuditing(Location.class);
		exceptions = auditLogService.getExceptions();
		assertEquals(++originalCount, exceptions.size());
		assertTrue(OpenmrsUtil.collectionContains(exceptions, Location.class));
		assertEquals(false, auditLogService.isAudited(Location.class));
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(java.util.Set)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as un audited", method = "stopAuditing(Set<Class<?>>)")
	public void stopAuditing_shouldMarkAClassAndItsKnownSubclassesAsUnAudited() throws Exception {
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		assertTrue(exceptions.contains(Concept.class));
		assertTrue(exceptions.contains(ConceptNumeric.class));
		assertTrue(exceptions.contains(ConceptComplex.class));
		assertEquals(true, auditLogService.isAudited(Concept.class));
		assertEquals(true, auditLogService.isAudited(ConceptNumeric.class));
		assertEquals(true, auditLogService.isAudited(ConceptComplex.class));
		
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(Concept.class);
		
		auditLogService.stopAuditing(classes);
		assertFalse(exceptions.contains(Concept.class));
		assertFalse(exceptions.contains(ConceptNumeric.class));
		assertFalse(exceptions.contains(ConceptComplex.class));
		assertEquals(false, auditLogService.isAudited(Concept.class));
		assertEquals(false, auditLogService.isAudited(ConceptNumeric.class));
		assertEquals(false, auditLogService.isAudited(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogService#stopAuditing(Class)}
	 */
	@Test
	@Verifies(value = "should mark a class and its known subclasses as un audited for all_except strategy", method = "stopAuditing(Set<Class<?>>)")
	public void stopAuditing_shouldMarkAClassAndItsKnownSubclassesAsUnAuditedForAll_exceptStrategy() throws Exception {
		setAuditConfiguration(AuditingStrategy.ALL_EXCEPT, EXCEPTIONS_FOR_ALL_EXCEPT, false);
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		assertFalse(exceptions.contains(Order.class));
		assertFalse(exceptions.contains(DrugOrder.class));
		assertEquals(true, auditLogService.isAudited(Order.class));
		assertEquals(true, auditLogService.isAudited(DrugOrder.class));
		
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(Order.class);
		auditLogService.stopAuditing(classes);
		exceptions = auditLogService.getExceptions();
		assertTrue(exceptions.contains(Order.class));
		assertTrue(exceptions.contains(DrugOrder.class));
		assertEquals(false, auditLogService.isAudited(Order.class));
		assertEquals(false, auditLogService.isAudited(DrugOrder.class));
	}
	
	/**
	 * @verifies remove association types from exception classes
	 * @see AuditLogService#stopAuditing(java.util.Set>)
	 */
	@Test
	public void stopAuditing_shouldRemoveAssociationTypesFromAuditedClasses() throws Exception {
		assertTrue(auditLogService.isAudited(Concept.class));
		assertFalse(auditLogService.isAudited(ConceptName.class));
		assertTrue(getAuditLogDAO().isImplicitlyAudited(ConceptName.class));
		
		auditLogService.stopAuditing(Concept.class);
		
		assertFalse(auditLogService.isAudited(Concept.class));
		assertFalse(auditLogService.isAudited(ConceptName.class));
		assertFalse(getAuditLogDAO().isImplicitlyAudited(ConceptName.class));
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should get all logs for the object matching the specified uuid", method = "getAuditLogs(String,Class<?>,List<Action>,Date,Date)")
	public void getAuditLogs_shouldGetAllLogsForTheObjectMatchingTheSpecifiedUuid() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(
		    2,
		    auditLogService.getAuditLogs("c607c80f-1ea9-4da3-bb88-6276ce8868dd", ConceptNumeric.class, null, null, null,
		        false).size());
	}
	
	/**
	 * @see {@link AuditLogService#isAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return true if the class is audited for none except strategy", method = "isAudited(Class<*>)")
	public void isAudited_shouldReturnTrueIfTheClassIsAuditedForNoneExceptStrategy() throws Exception {
		assertTrue(auditLogService.isAudited(Concept.class));
		assertTrue(auditLogService.isAudited(ConceptNumeric.class));
		assertTrue(auditLogService.isAudited(ConceptComplex.class));
		assertTrue(auditLogService.isAudited(EncounterType.class));
		assertTrue(auditLogService.isAudited(PatientIdentifierType.class));
	}
	
	/**
	 * @see {@link AuditLogService#isAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return false if the class is not audited for none except strategy", method = "isAudited(Class<*>)")
	public void isAudited_shouldReturnFalseIfTheClassIsNotAuditedForNoneExceptStrategy() throws Exception {
		assertFalse(auditLogService.isAudited(Cohort.class));
	}
	
	/**
	 * @see {@link AuditLogService#isAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return true if the class is audited for all except strategy", method = "isAudited(Class<*>)")
	public void isAudited_shouldReturnTrueIfTheClassIsAuditedForAllExceptStrategy() throws Exception {
		assertFalse(auditLogService.isAudited(Cohort.class));
		AuditingStrategy newStrategy = AuditingStrategy.ALL_EXCEPT;
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, newStrategy.name());
		assertTrue(auditLogService.isAudited(Cohort.class));
		assertTrue(auditLogService.isAudited(Concept.class));
		assertTrue(auditLogService.isAudited(ConceptNumeric.class));
		assertTrue(auditLogService.isAudited(ConceptComplex.class));
		assertTrue(auditLogService.isAudited(EncounterType.class));
		assertTrue(auditLogService.isAudited(PatientIdentifierType.class));
		assertEquals(newStrategy, auditLogService.getAuditingStrategy());
	}
	
	/**
	 * @see {@link AuditLogService#isAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return false if the class is not audited for all except strategy", method = "isAudited(Class<*>)")
	public void isAudited_shouldReturnFalseIfTheClassIsNotAuditedForAllExceptStrategy() throws Exception {
		assertFalse(auditLogService.isAudited(Cohort.class));
		AuditingStrategy newStrategy = AuditingStrategy.ALL_EXCEPT;
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, newStrategy.name());
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_EXCEPTIONS,
		    EncounterType.class.getName() + "," + Location.class.getName());
		assertEquals(newStrategy, auditLogService.getAuditingStrategy());
		assertFalse(auditLogService.isAudited(EncounterType.class));
		assertFalse(auditLogService.isAudited(Location.class));
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(String, Class, java.util.List, java.util.Date, java.util.Date, boolean)}
	 */
	@Test
	@Verifies(value = "should include logs for subclasses when getting by type", method = "getAuditLogs(String,Class<?>,List<Action>,Date,Date)")
	public void getAuditLogs_shouldIncludeLogsForSubclassesWhenGettingByType() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(2,
		    auditLogService.getAuditLogs("c607c80f-1ea9-4da3-bb88-6276ce8868dd", Concept.class, null, null, null, false)
		            .size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(java.util.List, java.util.List, java.util.Date, java.util.Date, boolean, Integer, Integer)}
	 */
	@Test
	@Verifies(value = "should exclude child logs if excludeChildAuditLogsis set to true", method = "getAuditLogs(List<Class<?>>,List<Action>,Date,Date,null,Integer,Integer)")
	public void getAuditLogs_shouldExcludeChildLogsIfExcludeChildAuditLogsisSetToTrue() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(4, auditLogService.getAuditLogs(null, null, null, null, true, null, null).size());
	}
	
	/**
	 * @verifies exclude child logs for object if excludeChildAuditLogs is set to true
	 * @see AuditLogService#getAuditLogs(String, Class, java.util.List, java.util.Date,
	 *      java.util.Date, boolean)
	 */
	@Test
	public void getAuditLogs_shouldExcludeChildLogsForObjectIfExcludeChildAuditLogsIsSetToTrue() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		assertEquals(
		    0,
		    auditLogService.getAuditLogs("d607c80f-1ea9-4da3-bb88-6276ce8868de", ConceptDescription.class, null, null, null,
		        true).size());
	}
	
	/**
	 * @verifies get all logs for the specified object
	 * @see AuditLogService#getAuditLogs(org.openmrs.OpenmrsObject, java.util.List, java.util.Date,
	 *      java.util.Date, boolean)
	 */
	@Test
	public void getAuditLogs_shouldGetAllLogsForTheSpecifiedObject() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		OpenmrsObject obj = auditLogService.getObjectByUuid(ConceptNumeric.class, "c607c80f-1ea9-4da3-bb88-6276ce8868dd");
		assertEquals(2, auditLogService.getAuditLogs(obj, null, null, null, false).size());
	}
}
