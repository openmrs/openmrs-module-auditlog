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

import static org.openmrs.module.auditlog.AuditLog.Action.CREATED;
import static org.openmrs.module.auditlog.AuditLog.Action.DELETED;
import static org.openmrs.module.auditlog.AuditLog.Action.UPDATED;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

import org.junit.Test;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptDescription;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.api.CohortService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.springframework.test.annotation.NotTransactional;

/**
 * Contains tests for testing the core functionality of the module
 */
@SuppressWarnings("deprecation")
public class CollectionsAuditLogBehaviorTest extends BaseBehaviorTest {
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnUnMonitoredElementIsRemovedFromAChildCollection() throws Exception {
		Assert.assertFalse(auditLogService.isMonitored(PersonName.class));
		PatientService patientService = Context.getPatientService();
		Patient patient = patientService.getPatient(2);
		patientService.savePatient(patient);
		String nameUuid = patient.getPersonName().getUuid();
		List<AuditLog> existingUpdateLogs = auditLogService.getAuditLogs(patient.getUuid(), Patient.class,
		    Collections.singletonList(UPDATED), null, null);
		Assert.assertEquals(0, existingUpdateLogs.size());
		int originalCount = patient.getNames().size();
		Assert.assertTrue(originalCount > 0);
		String previousNameUuids = "";
		for (PersonName name : patient.getNames()) {
			previousNameUuids += (AuditLogConstants.UUID_LABEL + name.getUuid());
		}
		
		auditLogService.startMonitoring(Patient.class);
		patient.removeName(patient.getPersonName());
		patientService.savePatient(patient);
		Assert.assertEquals(originalCount - 1, patient.getNames().size());
		List<AuditLog> patientLogs = auditLogService.getAuditLogs(patient.getUuid(), Patient.class,
		    Collections.singletonList(UPDATED), null, null);
		Assert.assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		Assert.assertEquals(al.getObjectUuid(), patient.getUuid());
		Assert.assertNull(al.getChanges().get("names")[0]);
		Assert.assertEquals(al.getChanges().get("names")[1], previousNameUuids);
		List<AuditLog> nameLogs = auditLogService.getAuditLogs(nameUuid, PersonName.class,
		    Collections.singletonList(DELETED), null, null);
		Assert.assertEquals(1, nameLogs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnUnMonitoredElementIsAddedToAChildCollection() throws Exception {
		Assert.assertFalse(auditLogService.isMonitored(ConceptDescription.class));
		Concept concept = conceptService.getConcept(5089);
		//something with ConceptMaps having blank uuids and now getting set, this should have been
		//fixed in later versions
		conceptService.saveConcept(concept);
		List<AuditLog> existingUpdateLogs = auditLogService.getAuditLogs(concept.getUuid(), Concept.class,
		    Collections.singletonList(UPDATED), null, null);
		int originalCount = concept.getDescriptions().size();
		Assert.assertTrue(originalCount == 1);
		String previousDescriptionUuids = AuditLogConstants.UUID_LABEL + concept.getDescription().getUuid();
		
		ConceptDescription cd1 = new ConceptDescription("desc1", Locale.ENGLISH);
		cd1.setDateCreated(new Date());
		cd1.setCreator(Context.getAuthenticatedUser());
		concept.addDescription(cd1);
		conceptService.saveConcept(concept);
		
		List<AuditLog> conceptLogs = auditLogService.getAuditLogs(concept.getUuid(), Concept.class,
		    Collections.singletonList(UPDATED), null, null);
		Assert.assertEquals(existingUpdateLogs.size() + 1, conceptLogs.size());
		conceptLogs.removeAll(existingUpdateLogs);
		Assert.assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		Assert.assertEquals(al.getObjectUuid(), concept.getUuid());
		Assert.assertEquals(al.getChanges().get("descriptions")[0], previousDescriptionUuids + ","
		        + AuditLogConstants.UUID_LABEL + cd1.getUuid());
		Assert.assertEquals(al.getChanges().get("descriptions")[1], previousDescriptionUuids);
		
		List<AuditLog> descriptionLogs = auditLogService.getAuditLogs(cd1.getUuid(), ConceptDescription.class,
		    Collections.singletonList(CREATED), null, null);
		Assert.assertEquals(1, descriptionLogs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentObjectWhenAnUnMonitoredElementInAChildCollectionIsUpdated()
	    throws Exception {
		Assert.assertFalse(auditLogService.isMonitored(ConceptDescription.class));
		Concept concept = conceptService.getConcept(7);
		Assert.assertEquals(0,
		    auditLogService.getAuditLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED), null, null)
		            .size());
		
		int originalDescriptionCount = concept.getDescriptions().size();
		Assert.assertTrue(originalDescriptionCount > 0);
		
		concept.getDescription().setDescription("another descr");
		concept = conceptService.saveConcept(concept);
		Assert.assertEquals(originalDescriptionCount, concept.getDescriptions().size());
		
		Assert.assertEquals(1,
		    auditLogService.getAuditLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED), null, null)
		            .size());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAllItemsAreRemovedFromACollection() throws Exception {
		Concept c = conceptService.getConcept(7);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Concept.class, actions).size();
		Assert.assertEquals(4, c.getDescriptions().size());
		Iterator<ConceptDescription> it = c.getDescriptions().iterator();
		String descriptionUuid1 = it.next().getUuid();
		String descriptionUuid2 = it.next().getUuid();
		String descriptionUuid3 = it.next().getUuid();
		String descriptionUuid4 = it.next().getUuid();
		c.getDescriptions().clear();
		conceptService.saveConcept(c);
		
		List<AuditLog> logs = getAllLogs(c.getUuid(), Concept.class, actions);
		int newCount = logs.size();
		Assert.assertEquals(++count, newCount);
		Assert.assertNull(logs.get(0).getChanges().get("descriptions")[0]);
		Assert.assertNotNull(logs.get(0).getChanges().get("descriptions")[1].indexOf(descriptionUuid1) > -1);
		Assert.assertNotNull(logs.get(0).getChanges().get("descriptions")[1].indexOf(descriptionUuid2) > -1);
		Assert.assertNotNull(logs.get(0).getChanges().get("descriptions")[1].indexOf(descriptionUuid3) > -1);
		Assert.assertNotNull(logs.get(0).getChanges().get("descriptions")[1].indexOf(descriptionUuid4) > -1);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnItemIsAddedToCollectionOfNoneOpenmrsObjects() throws Exception {
		executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
		CohortService cs = Context.getCohortService();
		final Integer memberId = 5;
		Cohort c = cs.getCohort(1);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Cohort.class, actions).size();
		auditLogService.startMonitoring(Cohort.class);
		try {
			Assert.assertTrue(auditLogService.isMonitored(Cohort.class));
			Assert.assertFalse(c.contains(memberId));
			c.addMember(memberId);
			cs.saveCohort(c);
			
			List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
			int newCount = logs.size();
			Assert.assertEquals(++count, newCount);
			Assert.assertTrue(logs.get(0).getChanges().get("memberIds")[0].indexOf(memberId.toString()) > -1);
			Assert.assertEquals(-1, logs.get(0).getChanges().get("memberIds")[1].indexOf(memberId.toString()));
		}
		finally {
			auditLogService.stopMonitoring(Cohort.class);
		}
		Assert.assertFalse(auditLogService.isMonitored(Cohort.class));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnItemIsRemovedFromCollectionOfNoneOpenmrsObjects() throws Exception {
		executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
		CohortService cs = Context.getCohortService();
		final Integer memberId = 2;
		Cohort c = cs.getCohort(1);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Cohort.class, actions).size();
		auditLogService.startMonitoring(Cohort.class);
		try {
			Assert.assertTrue(auditLogService.isMonitored(Cohort.class));
			Assert.assertTrue(c.contains(memberId));
			c.removeMember(memberId);
			cs.saveCohort(c);
			
			List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
			int newCount = logs.size();
			Assert.assertEquals(++count, newCount);
			Assert.assertEquals(-1, logs.get(0).getChanges().get("memberIds")[0].indexOf(memberId.toString()));
			Assert.assertTrue(logs.get(0).getChanges().get("memberIds")[1].indexOf(memberId.toString()) > -1);
		}
		finally {
			auditLogService.stopMonitoring(Cohort.class);
		}
		Assert.assertFalse(auditLogService.isMonitored(Cohort.class));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAllItemsAreRemovedFromCollectionOfNoneOpenmrsObjects() throws Exception {
		executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
		CohortService cs = Context.getCohortService();
		final Integer memberId2 = 2;
		final Integer memberId3 = 3;
		Cohort c = cs.getCohort(1);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Cohort.class, actions).size();
		auditLogService.startMonitoring(Cohort.class);
		try {
			Assert.assertTrue(auditLogService.isMonitored(Cohort.class));
			Assert.assertEquals(2, c.getMemberIds().size());
			Assert.assertTrue(c.contains(memberId2));
			Assert.assertTrue(c.contains(memberId3));
			c.getMemberIds().clear();
			cs.saveCohort(c);
			
			List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
			int newCount = logs.size();
			Assert.assertEquals(++count, newCount);
			Assert.assertNull(logs.get(0).getChanges().get("memberIds")[0]);
			Assert.assertTrue(logs.get(0).getChanges().get("memberIds")[1].indexOf(memberId2.toString()) > -1);
			Assert.assertTrue(logs.get(0).getChanges().get("memberIds")[1].indexOf(memberId3.toString()) > -1);
		}
		finally {
			auditLogService.stopMonitoring(Cohort.class);
		}
		Assert.assertFalse(auditLogService.isMonitored(Cohort.class));
	}
	
	@Test
	@NotTransactional
	public void shouldLinkTheLogsOfCollectionItemsToThatOfTheUpdatedParent() throws Exception {
		Concept concept = conceptService.getConcept(7);
		Assert.assertEquals(0,
		    auditLogService.getAuditLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED), null, null)
		            .size());
		int originalDescriptionCount = concept.getDescriptions().size();
		Assert.assertTrue(originalDescriptionCount > 3);
		
		auditLogService.startMonitoring(ConceptDescription.class);
		try {
			Assert.assertTrue(auditLogService.isMonitored(ConceptDescription.class));
			Iterator<ConceptDescription> it = concept.getDescriptions().iterator();
			//update some existing descriptions
			ConceptDescription cd1 = it.next();
			cd1.setDescription("another descr1");
			ConceptDescription cd2 = it.next();
			cd2.setDescription("another descr2");
			//remove the next 2
			ConceptDescription cd3 = it.next();
			it.remove();
			ConceptDescription cd4 = it.next();
			it.remove();
			ConceptDescription cd5 = new ConceptDescription("yes in japanese", Locale.JAPANESE);
			cd5.setUuid("6e9226f4-999d-11e2-a6ac-b499bae1ce4e");
			cd5.setDateCreated(new Date());
			cd5.setCreator(Context.getAuthenticatedUser());
			ConceptDescription cd6 = new ConceptDescription("yes in chinese", Locale.CHINESE);
			cd6.setUuid("781f01b0-999d-11e2-a6ac-b499bae1ce4e");
			cd6.setDateCreated(new Date());
			cd6.setCreator(Context.getAuthenticatedUser());
			concept.addDescription(cd5);
			concept.addDescription(cd6);
			concept = conceptService.saveConcept(concept);
			Assert.assertEquals(originalDescriptionCount, concept.getDescriptions().size());
			List<AuditLog> descriptionAuditLogs1 = getAllLogs(cd1.getUuid(), ConceptDescription.class,
			    Collections.singletonList(UPDATED));
			Assert.assertEquals(1, descriptionAuditLogs1.size());
			AuditLog descriptionAuditLog1 = descriptionAuditLogs1.get(0);
			Assert.assertNotNull(descriptionAuditLog1.getParentAuditLog());
			
			List<AuditLog> descriptionAuditLogs2 = getAllLogs(cd2.getUuid(), ConceptDescription.class,
			    Collections.singletonList(UPDATED));
			Assert.assertEquals(1, descriptionAuditLogs2.size());
			AuditLog descriptionAuditLog2 = descriptionAuditLogs2.get(0);
			Assert.assertNotNull(descriptionAuditLog2.getParentAuditLog());
			
			List<AuditLog> descriptionAuditLogs3 = getAllLogs(cd3.getUuid(), ConceptDescription.class,
			    Collections.singletonList(DELETED));
			Assert.assertEquals(1, descriptionAuditLogs3.size());
			AuditLog descriptionAuditLog3 = descriptionAuditLogs3.get(0);
			Assert.assertNotNull(descriptionAuditLog3.getParentAuditLog());
			
			List<AuditLog> descriptionAuditLogs4 = getAllLogs(cd4.getUuid(), ConceptDescription.class,
			    Collections.singletonList(DELETED));
			Assert.assertEquals(1, descriptionAuditLogs4.size());
			AuditLog descriptionAuditLog4 = descriptionAuditLogs4.get(0);
			Assert.assertNotNull(descriptionAuditLog4.getParentAuditLog());
			
			List<AuditLog> descriptionAuditLogs5 = getAllLogs(cd5.getUuid(), ConceptDescription.class,
			    Collections.singletonList(CREATED));
			Assert.assertEquals(1, descriptionAuditLogs5.size());
			AuditLog descriptionAuditLog5 = descriptionAuditLogs5.get(0);
			Assert.assertNotNull(descriptionAuditLog5.getParentAuditLog());
			
			List<AuditLog> descriptionAuditLogs6 = getAllLogs(cd6.getUuid(), ConceptDescription.class,
			    Collections.singletonList(CREATED));
			Assert.assertEquals(1, descriptionAuditLogs6.size());
			AuditLog descriptionAuditLog6 = descriptionAuditLogs6.get(0);
			Assert.assertNotNull(descriptionAuditLog6.getParentAuditLog());
			
			List<AuditLog> conceptAuditLogs = getAllLogs(concept.getUuid(), Concept.class,
			    Collections.singletonList(UPDATED));
			Assert.assertEquals(1, conceptAuditLogs.size());
			Assert.assertEquals(6, conceptAuditLogs.get(0).getChildAuditLogs().size());
			
			Assert.assertEquals(conceptAuditLogs.get(0), descriptionAuditLog1.getParentAuditLog());
			Assert.assertEquals(conceptAuditLogs.get(0), descriptionAuditLog2.getParentAuditLog());
		}
		finally {
			auditLogService.stopMonitoring(ConceptDescription.class);
		}
		Assert.assertFalse(auditLogService.isMonitored(ConceptDescription.class));
	}
}
