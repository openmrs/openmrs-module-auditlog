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
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.auditlog.AuditLog.Action.CREATED;
import static org.openmrs.module.auditlog.AuditLog.Action.DELETED;
import static org.openmrs.module.auditlog.AuditLog.Action.UPDATED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Order;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.annotation.NotTransactional;

/**
 * Contains tests for testing the core functionality of the module
 */
@SuppressWarnings("deprecation")
public class AuditLogBehaviorTest extends BaseBehaviorTest {
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogEntryWhenANewObjectIsCreated() {
		Concept concept = new Concept();
		ConceptName cn = new ConceptName("new", Locale.ENGLISH);
		cn.setConcept(concept);
		concept.addName(cn);
		concept.setDatatype(conceptService.getConceptDatatype(4));
		concept.setConceptClass(conceptService.getConceptClass(4));
		conceptService.saveConcept(concept);
		List<AuditLog> logs = getAllLogs();
		Assert.assertNotNull(concept.getConceptId());
		//Should have created an entry for the concept and concept name
		Assert.assertEquals(2, logs.size());
		//The latest logs come first
		Assert.assertEquals(CREATED, logs.get(0).getAction());
		Assert.assertEquals(CREATED, logs.get(1).getAction());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogEntryWhenAnObjectIsDeleted() throws Exception {
		EncounterType encounterType = encounterService.getEncounterType(6);
		encounterService.purgeEncounterType(encounterType);
		List<AuditLog> logs = getAllLogs(encounterType.getUuid(), EncounterType.class, null);
		//Should have created a log entry for deleted Encounter type
		Assert.assertEquals(1, logs.size());
		Assert.assertEquals(DELETED, logs.get(0).getAction());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogEntryWhenAnObjectIsEdited() throws Exception {
		Concept concept = conceptService.getConcept(3);
		String oldConceptClassUuid = concept.getConceptClass().getUuid();
		String oldDatatypeUuid = concept.getDatatype().getUuid();
		ConceptClass cc = conceptService.getConceptClass(2);
		ConceptDatatype dt = conceptService.getConceptDatatype(3);
		String oldVersion = concept.getVersion();
		String newVersion = "1.11";
		Assert.assertFalse(cc.equals(concept.getConceptClass()));
		Assert.assertFalse(dt.equals(concept.getDatatype()));
		Assert.assertFalse(newVersion.equalsIgnoreCase(oldVersion));
		
		concept.setConceptClass(cc);
		concept.setDatatype(dt);
		concept.setVersion(newVersion);
		conceptService.saveConcept(concept);
		
		List<AuditLog> logs = getAllLogs();
		//Should have created a log entry for edited concept
		Assert.assertEquals(1, logs.size());
		AuditLog auditLog = logs.get(0);
		
		//Should have created entries for the changes properties and their old values
		Assert.assertEquals(UPDATED, auditLog.getAction());
		//Check that there 3 property tag entries
		Map<String, String[]> changes = auditLog.getChanges();
		Assert.assertEquals(3, changes.size());
		Assert.assertEquals(AuditLogConstants.UUID_LABEL + oldConceptClassUuid, changes.get("conceptClass")[1]);
		Assert.assertEquals(AuditLogConstants.UUID_LABEL + oldDatatypeUuid, changes.get("datatype")[1]);
		Assert.assertEquals(oldVersion, changes.get("version")[1]);
		
		Assert.assertEquals(AuditLogConstants.UUID_LABEL + cc.getUuid(), changes.get("conceptClass")[0]);
		Assert.assertEquals(AuditLogConstants.UUID_LABEL + dt.getUuid(), changes.get("datatype")[0]);
		Assert.assertEquals(newVersion, changes.get("version")[0]);
	}
	
	@Test
	@NotTransactional
	public void shouldCreateNoLogEntryIfNoChangesAreMadeToAnExistingObject() throws Exception {
		EncounterType encounterType = encounterService.getEncounterType(2);
		encounterService.saveEncounterType(encounterType);
		Assert.assertTrue(getAllLogs().isEmpty());
	}
	
	@Test
	@NotTransactional
	public void shouldIgnoreDateChangedAndCreatedFields() throws Exception {
		Concept concept = conceptService.getConcept(3);
		//sanity checks
		Assert.assertNull(concept.getDateChanged());
		Assert.assertNull(concept.getChangedBy());
		concept.setDateChanged(new Date());
		concept.setChangedBy(Context.getAuthenticatedUser());
		conceptService.saveConcept(concept);
		Assert.assertTrue(getAllLogs().isEmpty());
	}
	
	@Test
	@NotTransactional
	public void shouldHandleInsertsOrUpdatesOrDeletesInEachTransactionIndependently() throws InterruptedException {
		final int N = 50;
		final Set<Thread> threads = new LinkedHashSet<Thread>();
		
		for (int i = 0; i < N; i++) {
			threads.add(new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						Context.openSession();
						Context.authenticate("admin", "test");
						Integer index = new Integer(Thread.currentThread().getName());
						EncounterService es = Context.getEncounterService();
						if (index == 0) {
							//Let's have a delete
							EncounterType existingEncounterType = es.getEncounterType(6);
							Assert.assertNotNull(existingEncounterType);
							es.purgeEncounterType(existingEncounterType);
						} else {
							EncounterType encounterType = null;
							if (index % 2 == 0) {
								//And some updates
								encounterType = es.getEncounterType(2);
								encounterType.setDescription("New Description-" + index);
							} else {
								//And some new rows inserted
								encounterType = new EncounterType("Encounter Type-" + index, "Description-" + index);
							}
							es.saveEncounterType(encounterType);
						}
					}
					finally {
						Context.closeSession();
					}
				}
			}, new Integer(i).toString()));
		}
		
		for (Thread thread : threads) {
			thread.start();
		}
		
		for (Thread thread : threads) {
			thread.join();
		}
		
		Assert.assertEquals(N, getAllLogs().size());
		
		List<Action> actions = new ArrayList<Action>();
		actions.add(CREATED);//should match expected count of created log entries
		Assert.assertEquals(25, auditLogService.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.clear();
		actions.add(UPDATED);//should match expected count of updated log entries
		Assert.assertEquals(24, auditLogService.getAuditLogs(null, actions, null, null, false, null, null).size());
		
		actions.clear();
		actions.add(DELETED);//should match expected count of deleted log entries
		Assert.assertEquals(1, auditLogService.getAuditLogs(null, actions, null, null, false, null, null).size());
	}
	
	@Test
	@NotTransactional
	public void shouldNotCreateAuditLogsForUnMonitoredObjects() {
		Assert.assertFalse(auditLogService.isMonitored(Location.class));
		Location location = new Location();
		location.setName("najja");
		location.setAddress1("test address");
		Location savedLocation = Context.getLocationService().saveLocation(location);
		Assert.assertNotNull(savedLocation.getLocationId());//sanity check that it was actually created
		//Should not have created any logs
		Assert.assertTrue(getAllLogs().isEmpty());
	}
	
	@Test
	@NotTransactional
	public void shouldIgnoreChangesForStringFieldsFromNullToBlank() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifierType idType = ps.getPatientIdentifierType(1);
		idType.setFormat(null);
		ps.savePatientIdentifierType(idType);
		
		int originalLogCount = getAllLogs().size();
		idType.setFormat("");
		ps.savePatientIdentifierType(idType);
		Assert.assertEquals(originalLogCount, getAllLogs().size());
	}
	
	@Test
	@NotTransactional
	public void shouldIgnoreChangesForStringFieldsFromBlankToNull() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifierType idType = ps.getPatientIdentifierType(1);
		idType.setFormat("");
		idType = ps.savePatientIdentifierType(idType);
		//this will fail when required version is 1.9 since it converts blanks to null
		Assert.assertEquals("", idType.getFormat());
		
		int originalLogCount = getAllLogs().size();
		idType.setFormat(null);
		ps.savePatientIdentifierType(idType);
		Assert.assertEquals(originalLogCount, getAllLogs().size());
	}
	
	@Test
	@NotTransactional
	public void shouldBeCaseInsensitiveForChangesInStringFields() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifierType idType = ps.getPatientIdentifierType(1);
		idType.setFormat("test");
		idType = ps.savePatientIdentifierType(idType);
		
		int originalLogCount = getAllLogs().size();
		idType.setFormat("TEST");
		ps.savePatientIdentifierType(idType);
		Assert.assertEquals(originalLogCount, getAllLogs().size());
	}
	
	@Test
	@NotTransactional
	public void shouldMonitorAnyOpenmrsObjectWhenStrateyIsSetToAll() throws Exception {
		Assert.assertFalse(auditLogService.isMonitored(Location.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		gp.setPropertyValue(MonitoringStrategy.ALL.name());
		as.saveGlobalProperty(gp);
		Location location = new Location();
		location.setName("new location");
		Context.getLocationService().saveLocation(location);
		Assert.assertEquals(1, getAllLogs(location.getUuid(), Location.class, Collections.singletonList(CREATED)).size());
	}
	
	@Test
	@NotTransactional
	public void shouldNotMonitorAnyObjectWhenStrateyIsSetToNone() throws Exception {
		Assert.assertTrue(auditLogService.isMonitored(EncounterType.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		gp.setPropertyValue(MonitoringStrategy.NONE.name());
		as.saveGlobalProperty(gp);
		EncounterType encounterType = encounterService.getEncounterType(6);
		encounterService.purgeEncounterType(encounterType);
		Assert.assertEquals(0, getAllLogs().size());
	}
	
	@Test
	@NotTransactional
	public void shouldNotCreateLogWhenStrateyIsSetToAllExceptAndObjectTypeIsListedAsExcluded() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		//sanity check
		GlobalProperty monitoredGP = as.getGlobalPropertyObject(AuditLogConstants.GP_UN_MONITORED_CLASSES);
		Assert.assertTrue(monitoredGP.getPropertyValue().indexOf(EncounterType.class.getName()) > -1);
		GlobalProperty strategyGP = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		strategyGP.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(strategyGP);
		
		EncounterType encounterType = encounterService.getEncounterType(6);
		encounterService.purgeEncounterType(encounterType);
		Assert.assertEquals(0, getAllLogs(encounterType.getUuid(), EncounterType.class, Collections.singletonList(DELETED))
		        .size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateLogWhenStrateyIsSetToAllExceptAndObjectTypeIsNotListedAsIncluded() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		//sanity check
		GlobalProperty monitoredGP = as.getGlobalPropertyObject(AuditLogConstants.GP_UN_MONITORED_CLASSES);
		Assert.assertTrue(monitoredGP.getPropertyValue().indexOf(EncounterType.class.getName()) > -1);
		GlobalProperty strategyGP = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		strategyGP.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(strategyGP);
		
		Location location = new Location();
		location.setName("new location");
		Context.getLocationService().saveLocation(location);
		Assert.assertEquals(1, getAllLogs(location.getUuid(), Location.class, Collections.singletonList(CREATED)).size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateLogForUnMonitoredTypeIfTheOwningTypeIsMonitoredAndStrategyIsAllExcept() throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		Assert.assertEquals(0, getAllLogs().size());
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		gp.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		
		auditLogService.stopMonitoring(LocationTag.class);
		Location loc = ls.getLocation(2);
		LocationTag tag = loc.getTags().iterator().next();
		tag.setDescription("new");
		ls.saveLocation(loc);
		Assert.assertEquals(1, getAllLogs(loc.getUuid(), Location.class, Collections.singletonList(UPDATED)).size());
		Assert.assertEquals(1, getAllLogs(tag.getUuid(), LocationTag.class, Collections.singletonList(UPDATED)).size());
	}
	
	@Test
	public void shouldUpdateTheMonitoredClassCacheWhenTheMonitoredClassGlobalPropertyIsUpdatedWithAnAddition()
	    throws Exception {
		Assert.assertFalse(auditLogService.isMonitored(Order.class));
		Assert.assertFalse(auditLogService.isMonitored(DrugOrder.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORED_CLASSES);
		Set<Class<?>> monitoredClasses = new HashSet<Class<?>>();
		monitoredClasses.addAll(auditLogService.getMonitoredClasses());
		monitoredClasses.add(Order.class);
		gp.setPropertyValue(StringUtils.join(AuditLogUtil.getAsListOfClassnames(monitoredClasses), ","));
		as.saveGlobalProperty(gp);
		Assert.assertTrue(auditLogService.isMonitored(Order.class));
		Assert.assertTrue(auditLogService.isMonitored(DrugOrder.class));
	}
	
	@Test
	public void shouldUpdateTheMonitoredClassCacheWhenTheMonitoredClassGlobalPropertyIsUpdatedWithARemoval()
	    throws Exception {
		Assert.assertTrue(auditLogService.isMonitored(Concept.class));
		Assert.assertTrue(auditLogService.isMonitored(ConceptNumeric.class));
		Assert.assertTrue(auditLogService.isMonitored(ConceptComplex.class));
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORED_CLASSES);
		Set<Class<?>> monitoredClasses = new HashSet<Class<?>>();
		monitoredClasses.addAll(auditLogService.getMonitoredClasses());
		monitoredClasses.remove(Concept.class);
		gp.setPropertyValue(StringUtils.join(AuditLogUtil.getAsListOfClassnames(monitoredClasses), ","));
		as.saveGlobalProperty(gp);
		Assert.assertFalse(auditLogService.isMonitored(Concept.class));
		Assert.assertTrue(auditLogService.isMonitored(ConceptNumeric.class));
		Assert.assertTrue(auditLogService.isMonitored(ConceptComplex.class));
	}
	
	@Test
	@NotTransactional
	public void shouldNotCreateAnAuditLogWhenTheTransactionIsRolledBack() throws Exception {
		ConceptService cs = Context.getConceptService();
		
		int initialLogCount = getAllLogs().size();
		boolean exceptionThrown = false;
		try {
			ConceptClass cc = cs.getConceptClass(1);
			cc.setUuid("An invalid long uuid that for sure should result into an exception");
			cs.saveConceptClass(cc);
		}
		catch (UncategorizedSQLException e) {
			exceptionThrown = true;
		}
		
		assertTrue(exceptionThrown);
		
		//No sync record should have been created
		assertEquals(initialLogCount, getAllLogs().size());
	}
}
