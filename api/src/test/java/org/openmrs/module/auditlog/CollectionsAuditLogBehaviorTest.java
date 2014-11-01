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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openmrs.module.auditlog.AuditLog.Action.CREATED;
import static org.openmrs.module.auditlog.AuditLog.Action.DELETED;
import static org.openmrs.module.auditlog.AuditLog.Action.UPDATED;
import static org.openmrs.module.auditlog.util.AuditLogConstants.MAP_KEY_VALUE_SEPARATOR;
import static org.openmrs.module.auditlog.util.AuditLogConstants.SEPARATOR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.Test;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptDescription;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.CohortService;
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.springframework.test.annotation.NotTransactional;

/**
 * Contains tests for testing the core functionality of the module
 */
@SuppressWarnings("deprecation")
public class CollectionsAuditLogBehaviorTest extends BaseBehaviorTest {
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnAuditedElementIsRemovedFromAChildCollection() throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		ps.savePatient(patient);
		List<AuditLog> existingUpdateLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		int originalCount = patient.getNames().size();
		assertTrue(originalCount > 1);
		
		auditLogService.startAuditing(Patient.class);
		auditLogService.startAuditing(PersonName.class);
		assertTrue(auditLogService.isAudited(PersonName.class));
		PersonName nameToRemove = null;
		for (PersonName name : patient.getNames()) {
			if (!name.isPreferred()) {
				nameToRemove = name;
				break;
			}
		}
		assertNotNull(nameToRemove);
		String nameUuid = nameToRemove.getUuid();
		patient.removeName(nameToRemove);
		ps.savePatient(patient);
		assertEquals(originalCount - 1, patient.getNames().size());
		List<AuditLog> patientLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(originalCount - 1, patient.getNames().size());
		assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("names", al).indexOf(nameToRemove.getUuid()));
		for (PersonName name : patient.getNames()) {
			assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("names", al).indexOf(name.getUuid()) > -1);
		}
		
		List<AuditLog> nameLogs = getAllLogs(nameUuid, PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, nameLogs.size());
		assertEquals(al, nameLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnAuditedElementIsAddedToAChildCollection() throws Exception {
		Concept concept = conceptService.getConcept(5089);
		//something with ConceptMaps having blank uuids and now getting set, this should have been
		//fixed in later versions
		conceptService.saveConcept(concept);
		List<AuditLog> existingUpdateLogs = getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED));
		int originalCount = concept.getDescriptions().size();
		assertTrue(originalCount == 1);
		String previousDescriptionUuids = AuditLogConstants.UUID_LABEL + concept.getDescription().getUuid();
		
		auditLogService.startAuditing(ConceptDescription.class);
		assertTrue(auditLogService.isAudited(ConceptDescription.class));
		
		ConceptDescription cd1 = new ConceptDescription("desc1", Locale.ENGLISH);
		cd1.setDateCreated(new Date());
		cd1.setCreator(Context.getAuthenticatedUser());
		concept.addDescription(cd1);
		conceptService.saveConcept(concept);
		
		List<AuditLog> conceptLogs = getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(existingUpdateLogs.size() + 1, conceptLogs.size());
		conceptLogs.removeAll(existingUpdateLogs);
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		assertEquals(AuditLogUtil.getNewValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionUuids + "\""
		        + AuditLogConstants.SEPARATOR + "\"" + AuditLogConstants.UUID_LABEL + cd1.getUuid() + "\"]");
		assertEquals(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionUuids
		        + "\"]");
		
		List<AuditLog> descriptionLogs = getAllLogs(cd1.getUuid(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionLogs.size());
		assertEquals(al, descriptionLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentObjectWhenAnAuditedElementInAChildCollectionIsUpdated() throws Exception {
		Concept concept = conceptService.getConcept(7);
		assertEquals(0, getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED)).size());
		
		int originalDescriptionCount = concept.getDescriptions().size();
		assertTrue(originalDescriptionCount > 0);
		
		auditLogService.startAuditing(ConceptDescription.class);
		assertTrue(auditLogService.isAudited(ConceptDescription.class));
		
		ConceptDescription description = concept.getDescription();
		description.setDescription("another descr");
		concept = conceptService.saveConcept(concept);
		assertEquals(originalDescriptionCount, concept.getDescriptions().size());
		List<AuditLog> conceptLogs = getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		
		List<AuditLog> descriptionLogs = getAllLogs(description.getUuid(), ConceptDescription.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, descriptionLogs.size());
		assertEquals(al, descriptionLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnUnAuditedElementIsRemovedFromAChildCollection() throws Exception {
		assertFalse(auditLogService.isAudited(PersonName.class));
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		ps.savePatient(patient);
		List<AuditLog> existingUpdateLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		int originalCount = patient.getNames().size();
		assertTrue(originalCount > 1);
		
		auditLogService.startAuditing(Patient.class);
		PersonName nameToRemove = null;
		for (PersonName name : patient.getNames()) {
			if (!name.isPreferred()) {
				nameToRemove = name;
				break;
			}
		}
		assertNotNull(nameToRemove);
		String nameUuid = nameToRemove.getUuid();
		patient.removeName(nameToRemove);
		ps.savePatient(patient);
		assertEquals(originalCount - 1, patient.getNames().size());
		List<AuditLog> patientLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(originalCount - 1, patient.getNames().size());
		assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("names", al).indexOf(nameToRemove.getUuid()));
		for (PersonName name : patient.getNames()) {
			assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("names", al).indexOf(name.getUuid()) > -1);
		}
		
		List<AuditLog> nameLogs = getAllLogs(nameUuid, PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, nameLogs.size());
		assertEquals(al, nameLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnUnAuditedElementIsAddedToAChildCollection() throws Exception {
		assertFalse(auditLogService.isAudited(ConceptDescription.class));
		Concept concept = conceptService.getConcept(5089);
		//something with ConceptMaps having blank uuids and now getting set, this should have been
		//fixed in later versions
		conceptService.saveConcept(concept);
		List<AuditLog> existingUpdateLogs = getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED));
		int originalCount = concept.getDescriptions().size();
		assertTrue(originalCount == 1);
		String previousDescriptionUuids = AuditLogConstants.UUID_LABEL + concept.getDescription().getUuid();
		
		ConceptDescription cd1 = new ConceptDescription("desc1", Locale.ENGLISH);
		cd1.setDateCreated(new Date());
		cd1.setCreator(Context.getAuthenticatedUser());
		concept.addDescription(cd1);
		conceptService.saveConcept(concept);
		
		List<AuditLog> conceptLogs = getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(existingUpdateLogs.size() + 1, conceptLogs.size());
		conceptLogs.removeAll(existingUpdateLogs);
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		assertEquals(AuditLogUtil.getNewValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionUuids + "\""
		        + AuditLogConstants.SEPARATOR + "\"" + AuditLogConstants.UUID_LABEL + cd1.getUuid() + "\"]");
		assertEquals(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionUuids
		        + "\"]");
		
		List<AuditLog> descriptionLogs = getAllLogs(cd1.getUuid(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionLogs.size());
		assertEquals(al, descriptionLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentObjectWhenAnUnAuditedElementInAChildCollectionIsUpdated() throws Exception {
		assertFalse(auditLogService.isAudited(ConceptDescription.class));
		Concept concept = conceptService.getConcept(7);
		assertEquals(0, getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED)).size());
		
		int originalDescriptionCount = concept.getDescriptions().size();
		assertTrue(originalDescriptionCount > 0);
		
		ConceptDescription description = concept.getDescription();
		description.setDescription("another descr");
		concept = conceptService.saveConcept(concept);
		assertEquals(originalDescriptionCount, concept.getDescriptions().size());
		List<AuditLog> conceptLogs = getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		
		List<AuditLog> descriptionLogs = getAllLogs(description.getUuid(), ConceptDescription.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, descriptionLogs.size());
		assertEquals(al, descriptionLogs.get(0).getParentAuditLog());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAllItemsAreRemovedFromACollection() throws Exception {
		Concept c = conceptService.getConcept(7);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Concept.class, actions).size();
		assertEquals(4, c.getDescriptions().size());
		Iterator<ConceptDescription> it = c.getDescriptions().iterator();
		String descriptionUuid1 = it.next().getUuid();
		String descriptionUuid2 = it.next().getUuid();
		String descriptionUuid3 = it.next().getUuid();
		String descriptionUuid4 = it.next().getUuid();
		c.getDescriptions().clear();
		conceptService.saveConcept(c);
		
		List<AuditLog> logs = getAllLogs(c.getUuid(), Concept.class, actions);
		int newCount = logs.size();
		assertEquals(++count, newCount);
		AuditLog log = logs.get(0);
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("descriptions", log));
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionUuid1) > -1);
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionUuid2) > -1);
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionUuid3) > -1);
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionUuid4) > -1);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnItemIsAddedToCollectionOfNonOpenmrsObjects() throws Exception {
		executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
		CohortService cs = Context.getCohortService();
		final Integer memberId = 5;
		Cohort c = cs.getCohort(1);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Cohort.class, actions).size();
		auditLogService.startAuditing(Cohort.class);
		assertTrue(auditLogService.isAudited(Cohort.class));
		assertFalse(c.contains(memberId));
		c.addMember(memberId);
		cs.saveCohort(c);
		
		List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
		int newCount = logs.size();
		assertEquals(++count, newCount);
		assertTrue(AuditLogUtil.getNewValueOfUpdatedItem("memberIds", logs.get(0)).indexOf(memberId.toString()) > -1);
		assertEquals(-1, AuditLogUtil.getPreviousValueOfUpdatedItem("memberIds", logs.get(0)).indexOf(memberId.toString()));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnItemIsRemovedFromCollectionOfNonOpenmrsObjects() throws Exception {
		executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
		CohortService cs = Context.getCohortService();
		final Integer memberId = 2;
		Cohort c = cs.getCohort(1);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Cohort.class, actions).size();
		auditLogService.startAuditing(Cohort.class);
		assertTrue(auditLogService.isAudited(Cohort.class));
		assertTrue(c.contains(memberId));
		c.removeMember(memberId);
		cs.saveCohort(c);
		
		List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
		int newCount = logs.size();
		assertEquals(++count, newCount);
		assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("memberIds", logs.get(0)).indexOf(memberId.toString()));
		assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("memberIds", logs.get(0)).indexOf(memberId.toString()) > -1);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAllItemsAreRemovedFromCollectionOfNonOpenmrsObjects() throws Exception {
		executeDataSet("org/openmrs/api/include/CohortServiceTest-cohort.xml");
		CohortService cs = Context.getCohortService();
		final Integer memberId2 = 2;
		final Integer memberId3 = 3;
		Cohort c = cs.getCohort(1);
		List actions = Collections.singletonList(UPDATED);
		int count = getAllLogs(c.getUuid(), Cohort.class, actions).size();
		auditLogService.startAuditing(Cohort.class);
		assertTrue(auditLogService.isAudited(Cohort.class));
		assertEquals(2, c.getMemberIds().size());
		assertTrue(c.contains(memberId2));
		assertTrue(c.contains(memberId3));
		c.getMemberIds().clear();
		cs.saveCohort(c);
		
		List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
		int newCount = logs.size();
		assertEquals(++count, newCount);
		AuditLog log = logs.get(0);
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("memberIds", log));
		assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("memberIds", log).indexOf(memberId2.toString()) > -1);
		assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("memberIds", log).indexOf(memberId3.toString()) > -1);
	}
	
	@Test
	@NotTransactional
	public void shouldCreateLogForUnAuditedTypeIfTheOwningTypeIsAuditedAndStrategyIsAllExcept() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		gp.setPropertyValue(AuditingStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		assertEquals(AuditingStrategy.ALL_EXCEPT, auditLogService.getAuditingStrategy());
		assertEquals(true, auditLogService.isAudited(Person.class));
		assertEquals(true, auditLogService.isAudited(PersonAddress.class));
		
		auditLogService.stopAuditing(PersonAddress.class);
		assertEquals(false, auditLogService.isAudited(PersonAddress.class));
		PersonService ps = Context.getPersonService();
		Person person = ps.getPerson(2);
		PersonAddress address = person.getPersonAddress();
		address.setAddress1("new");
		ps.savePerson(person);
		List<AuditLog> addressLogs = getAllLogs(address.getUuid(), PersonAddress.class, Collections.singletonList(UPDATED));
		assertEquals(1, addressLogs.size());
		List<AuditLog> personLogs = getAllLogs(person.getUuid(), Person.class, Collections.singletonList(UPDATED));
		assertEquals(1, personLogs.size());
		assertEquals(personLogs.get(0), addressLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldLinkTheLogsOfCollectionItemsToThatOfTheUpdatedParent() throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(PersonName.class);
		classes.add(Patient.class);
		
		auditLogService.startAuditing(classes);
		patient = ps.savePatient(patient);
		//Ensure that no log will be created unless we actually perform an update
		assertEquals(0, getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED)).size());
		
		int originalNameCount = patient.getNames().size();
		assertTrue(originalNameCount > 3);
		
		assertTrue(auditLogService.isAudited(PersonName.class));
		assertTrue(auditLogService.isAudited(Patient.class));
		Iterator<PersonName> it = patient.getNames().iterator();
		//update some existing names
		PersonName name1 = it.next();
		name1.setGivenName("another given name1");
		PersonName name2 = it.next();
		name2.setGivenName("another given name2");
		//remove the next 2
		PersonName name3 = it.next();
		it.remove();
		PersonName name4 = it.next();
		it.remove();
		PersonName name5 = new PersonName("another given name5", null, "another family name5");
		name5.setUuid("6e9226f4-999d-11e2-a6ac-b499bae1ce4e");
		name5.setDateCreated(new Date());
		name5.setCreator(Context.getAuthenticatedUser());
		PersonName name6 = new PersonName("another given name6", null, "another family name6");
		name6.setUuid("781f01b0-999d-11e2-a6ac-b499bae1ce4e");
		name6.setDateCreated(new Date());
		name6.setCreator(Context.getAuthenticatedUser());
		patient.addName(name5);
		patient.addName(name6);
		patient = ps.savePatient(patient);
		
		List<AuditLog> personNameAuditLogs1 = getAllLogs(name1.getUuid(), PersonName.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, personNameAuditLogs1.size());
		AuditLog personNameAuditLog1 = personNameAuditLogs1.get(0);
		assertNotNull(personNameAuditLog1.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs2 = getAllLogs(name2.getUuid(), PersonName.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, personNameAuditLogs2.size());
		AuditLog personNameAuditLog2 = personNameAuditLogs2.get(0);
		assertNotNull(personNameAuditLog2.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs3 = getAllLogs(name3.getUuid(), PersonName.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, personNameAuditLogs3.size());
		AuditLog personNameAuditLog3 = personNameAuditLogs3.get(0);
		assertNotNull(personNameAuditLog3.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs4 = getAllLogs(name4.getUuid(), PersonName.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, personNameAuditLogs4.size());
		AuditLog personNameAuditLog4 = personNameAuditLogs4.get(0);
		assertNotNull(personNameAuditLog4.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs5 = getAllLogs(name5.getUuid(), PersonName.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, personNameAuditLogs5.size());
		AuditLog personNameAuditLog5 = personNameAuditLogs5.get(0);
		assertNotNull(personNameAuditLog5.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs6 = getAllLogs(name6.getUuid(), PersonName.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, personNameAuditLogs6.size());
		AuditLog personNameAuditLog6 = personNameAuditLogs6.get(0);
		assertNotNull(personNameAuditLog6.getParentAuditLog());
		
		List<AuditLog> patientAuditLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientAuditLogs.size());
		assertEquals(6, patientAuditLogs.get(0).getChildAuditLogs().size());
		
		assertEquals(patientAuditLogs.get(0), personNameAuditLog1.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personNameAuditLog2.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personNameAuditLog3.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personNameAuditLog4.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personNameAuditLog5.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personNameAuditLog6.getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldNotLinkTheLogsOfCollectionItemsToThatOfTheUpdatedParentIfCascadeOptionIsNotDeleteOrphan()
	    throws Exception {
		//TODO Add check that the cascade options is not delete option
		Concept concept = conceptService.getConcept(7);
		assertEquals(0, getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED)).size());
		int originalDescriptionCount = concept.getDescriptions().size();
		assertTrue(originalDescriptionCount > 3);
		
		auditLogService.startAuditing(ConceptDescription.class);
		concept = conceptService.saveConcept(concept);
		//Ensure that no log will be created unless we actually perform an update
		assertEquals(0, getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED)).size());
		assertTrue(auditLogService.isAudited(ConceptDescription.class));
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
		assertEquals(originalDescriptionCount, concept.getDescriptions().size());
		List<AuditLog> descriptionAuditLogs1 = getAllLogs(cd1.getUuid(), ConceptDescription.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, descriptionAuditLogs1.size());
		AuditLog descriptionAuditLog1 = descriptionAuditLogs1.get(0);
		assertNotNull(descriptionAuditLog1.getParentAuditLog());
		
		List<AuditLog> descriptionAuditLogs2 = getAllLogs(cd2.getUuid(), ConceptDescription.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, descriptionAuditLogs2.size());
		AuditLog descriptionAuditLog2 = descriptionAuditLogs2.get(0);
		assertNotNull(descriptionAuditLog2.getParentAuditLog());
		
		List<AuditLog> descriptionAuditLogs3 = getAllLogs(cd3.getUuid(), ConceptDescription.class,
		    Collections.singletonList(DELETED));
		//This is because concept.descriptions cascade option doesn't say delete-orphan
		assertEquals(0, descriptionAuditLogs3.size());
		
		List<AuditLog> descriptionAuditLogs4 = getAllLogs(cd4.getUuid(), ConceptDescription.class,
		    Collections.singletonList(DELETED));
		assertEquals(0, descriptionAuditLogs4.size());//same here
		
		List<AuditLog> descriptionAuditLogs5 = getAllLogs(cd5.getUuid(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionAuditLogs5.size());
		AuditLog descriptionAuditLog5 = descriptionAuditLogs5.get(0);
		assertNotNull(descriptionAuditLog5.getParentAuditLog());
		
		List<AuditLog> descriptionAuditLogs6 = getAllLogs(cd6.getUuid(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionAuditLogs6.size());
		AuditLog descriptionAuditLog6 = descriptionAuditLogs6.get(0);
		assertNotNull(descriptionAuditLog6.getParentAuditLog());
		
		List<AuditLog> conceptAuditLogs = getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(1, conceptAuditLogs.size());
		assertEquals(4, conceptAuditLogs.get(0).getChildAuditLogs().size());
		
		assertEquals(conceptAuditLogs.get(0), descriptionAuditLog1.getParentAuditLog());
		assertEquals(conceptAuditLogs.get(0), descriptionAuditLog2.getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldLinkTheLogsOfCollectionItemsToThatOfTheDeletedParent() throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(PersonName.class);
		classes.add(PersonAttribute.class);
		classes.add(PersonAddress.class);
		classes.add(PatientIdentifier.class);
		classes.add(Patient.class);
		
		auditLogService.startAuditing(classes);
		patient = ps.savePatient(patient);
		//Ensure that no log will be created unless we actually perform an update
		assertEquals(0, getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED)).size());
		assertTrue(auditLogService.isAudited(PersonName.class));
		assertTrue(auditLogService.isAudited(Patient.class));
		
		assertEquals(4, patient.getNames().size());
		Iterator<PersonName> nameIt = patient.getNames().iterator();
		PersonName name1 = nameIt.next();
		PersonName name2 = nameIt.next();
		PersonName name3 = nameIt.next();
		PersonName name4 = nameIt.next();
		
		assertEquals(1, patient.getAddresses().size());
		PersonAddress address = patient.getAddresses().iterator().next();
		
		assertEquals(2, patient.getIdentifiers().size());
		Iterator<PatientIdentifier> idIt = patient.getIdentifiers().iterator();
		PatientIdentifier identifier1 = idIt.next();
		PatientIdentifier identifier2 = idIt.next();
		
		assertEquals(3, patient.getAttributes().size());
		Iterator<PersonAttribute> attributeIt = patient.getAttributes().iterator();
		PersonAttribute attribute1 = attributeIt.next();
		PersonAttribute attribute2 = attributeIt.next();
		PersonAttribute attribute3 = attributeIt.next();
		
		PersonService personService = Context.getPersonService();
		List<Relationship> relationships = personService.getRelationshipsByPerson(patient);
		for (Relationship r : relationships) {
			personService.purgeRelationship(r);
		}
		OrderService os = Context.getOrderService();
		List<Order> orders = os.getOrdersByPatient(patient);
		for (Order o : orders) {
			os.purgeOrder(o);
		}
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		List<PatientProgram> pps = pws.getPatientPrograms(patient, null, null, null, null, null, true);
		for (PatientProgram pp : pps) {
			pws.purgePatientProgram(pp);
		}
		ps.purgePatient(patient);
		
		List<AuditLog> personName1AuditLogs = getAllLogs(name1.getUuid(), PersonName.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, personName1AuditLogs.size());
		AuditLog personName1AuditLog = personName1AuditLogs.get(0);
		assertNotNull(personName1AuditLog.getParentAuditLog());
		
		List<AuditLog> personName2AuditLogs = getAllLogs(name2.getUuid(), PersonName.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, personName2AuditLogs.size());
		AuditLog personName2AuditLog = personName2AuditLogs.get(0);
		assertNotNull(personName2AuditLog.getParentAuditLog());
		
		List<AuditLog> personName3AuditLogs = getAllLogs(name3.getUuid(), PersonName.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, personName3AuditLogs.size());
		AuditLog personName3AuditLog = personName3AuditLogs.get(0);
		assertNotNull(personName3AuditLog.getParentAuditLog());
		
		List<AuditLog> personName4AuditLogs = getAllLogs(name4.getUuid(), PersonName.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, personName4AuditLogs.size());
		AuditLog personName4AuditLog = personName4AuditLogs.get(0);
		assertNotNull(personName4AuditLog.getParentAuditLog());
		
		List<AuditLog> addressAuditLogs = getAllLogs(address.getUuid(), PersonAddress.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, addressAuditLogs.size());
		AuditLog addressAuditLog = addressAuditLogs.get(0);
		assertNotNull(addressAuditLog.getParentAuditLog());
		
		List<AuditLog> id1AuditLogs = getAllLogs(identifier1.getUuid(), PatientIdentifier.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, id1AuditLogs.size());
		AuditLog id1AuditLog = id1AuditLogs.get(0);
		assertNotNull(id1AuditLog.getParentAuditLog());
		
		List<AuditLog> id2AuditLogs = getAllLogs(identifier2.getUuid(), PatientIdentifier.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, id2AuditLogs.size());
		AuditLog id2AuditLog = id2AuditLogs.get(0);
		assertNotNull(id2AuditLog.getParentAuditLog());
		
		List<AuditLog> attribute1AuditLogs = getAllLogs(attribute1.getUuid(), PersonAttribute.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, attribute1AuditLogs.size());
		AuditLog attribute1AuditLog = attribute1AuditLogs.get(0);
		assertNotNull(attribute1AuditLog.getParentAuditLog());
		
		List<AuditLog> attribute2AuditLogs = getAllLogs(attribute2.getUuid(), PersonAttribute.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, attribute2AuditLogs.size());
		AuditLog attribute2AuditLog = attribute2AuditLogs.get(0);
		assertNotNull(attribute2AuditLog.getParentAuditLog());
		
		List<AuditLog> attribute3AuditLogs = getAllLogs(attribute3.getUuid(), PersonAttribute.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, attribute3AuditLogs.size());
		AuditLog attribute3AuditLog = attribute3AuditLogs.get(0);
		assertNotNull(attribute3AuditLog.getParentAuditLog());
		
		List<AuditLog> patientAuditLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(DELETED));
		assertEquals(1, patientAuditLogs.size());
		assertEquals(10, patientAuditLogs.get(0).getChildAuditLogs().size());
		
		assertEquals(patientAuditLogs.get(0), personName1AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personName2AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personName3AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), personName4AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), addressAuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), id1AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), id2AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), attribute1AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), attribute2AuditLog.getParentAuditLog());
		assertEquals(patientAuditLogs.get(0), attribute3AuditLog.getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentWhenAnEntryIsAddedToAMapProperty() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		Map.Entry<String, String> entry = user.getUserProperties().entrySet().iterator().next();
		String previousUserProperties = "\"" + entry.getKey() + "\"" + MAP_KEY_VALUE_SEPARATOR + "\"" + entry.getValue()
		        + "\"";
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		final String newPropKey1 = "locale";
		final String newPropValue1 = "fr";
		final String newPropKey2 = "loginAttempts";
		final String newPropValue2 = "2";
		user.setUserProperty(newPropKey1, newPropValue1);
		user.setUserProperty(newPropKey2, newPropValue2);
		//Should work even for detached owners
		Context.evictFromSession(user);
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, null);
		assertEquals(1, logs.size());
		AuditLog al = logs.get(0);
		assertEquals("{" + previousUserProperties + "}", AuditLogUtil.getPreviousValueOfUpdatedItem("userProperties", al));
		String expectedNewUserProperties = "{" + previousUserProperties + SEPARATOR + "\"" + newPropKey2 + "\""
		        + MAP_KEY_VALUE_SEPARATOR + "\"" + newPropValue2 + "\"" + SEPARATOR + "\"" + newPropKey1 + "\""
		        + MAP_KEY_VALUE_SEPARATOR + "\"" + newPropValue1 + "\"}";
		assertEquals(expectedNewUserProperties, AuditLogUtil.getNewValueOfUpdatedItem("userProperties", al));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentWhenAnEntryIsRemovedFromAMapProperty() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		Map.Entry<String, String> entry = user.getUserProperties().entrySet().iterator().next();
		String previousUserProperties = "{\"" + entry.getKey() + "\"" + MAP_KEY_VALUE_SEPARATOR + "\"" + entry.getValue()
		        + "\"}";
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		user.getUserProperties().clear();//since it is 1, just clear
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, null);
		assertEquals(1, logs.size());
		AuditLog al = logs.get(0);
		assertEquals(previousUserProperties, AuditLogUtil.getPreviousValueOfUpdatedItem("userProperties", al));
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("userProperties", al));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentWhenAMapPropertyIsReplacedWithANewInstance() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		Map<String, String> originalProperties = user.getUserProperties();
		Map.Entry<String, String> entry = originalProperties.entrySet().iterator().next();
		String previousUserProperties = "{\"" + entry.getKey() + "\"" + MAP_KEY_VALUE_SEPARATOR + "\"" + entry.getValue()
		        + "\"}";
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		Map<String, String> newProperties = new HashMap<String, String>();
		final String newKey = "this is new";
		final String newValue = "this is the value";
		newProperties.put(newKey, newValue);
		user.setUserProperties(newProperties);
		//Should work even for detached owners
		Context.evictFromSession(user);
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, null);
		assertEquals(1, logs.size());
		AuditLog al = logs.get(0);
		assertEquals(previousUserProperties, AuditLogUtil.getPreviousValueOfUpdatedItem("userProperties", al));
		assertEquals("{\"" + newKey + "\":\"" + newValue + "\"}",
		    AuditLogUtil.getNewValueOfUpdatedItem("userProperties", al));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentWhenAMapPropertyIsReplacedWithANullValue() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		Map<String, String> originalProperties = user.getUserProperties();
		Map.Entry<String, String> entry = originalProperties.entrySet().iterator().next();
		String previousUserProperties = "{\"" + entry.getKey() + "\"" + MAP_KEY_VALUE_SEPARATOR + "\"" + entry.getValue()
		        + "\"}";
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		user.setUserProperties(null);
		//Should work even for detached owners
		Context.evictFromSession(user);
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, null);
		assertEquals(1, logs.size());
		AuditLog al = logs.get(0);
		assertEquals(previousUserProperties, AuditLogUtil.getPreviousValueOfUpdatedItem("userProperties", al));
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("userProperties", al));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogWhenNoChangesHaveBeenMadeToAUser() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		final String key = "some key";
		final String value = "some value";
		assertEquals(key, user.getUserProperties().keySet().iterator().next());
		assertEquals(value, user.getUserProperties().values().iterator().next());
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		//We are setting the same original value but the string are new objects
		user.setUserProperty(key, value);
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, null);
		assertEquals(0, logs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldSerializeMapEntriesAsSerializedDataForADeletedItem() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		Map.Entry<String, String> entry = user.getUserProperties().entrySet().iterator().next();
		String userProperties = "{\"" + entry.getKey() + "\"" + MAP_KEY_VALUE_SEPARATOR + "\"" + entry.getValue() + "\"}";
		assertEquals(true, auditLogService.isAudited(User.class));
		us.purgeUser(user);
		List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(DELETED));
		assertEquals(1, logs.size());
		AuditLog al = logs.get(0);
		Map<String, Object> propertyNameValueMap = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(al.getSerializedData())) {
			try {
				propertyNameValueMap = new ObjectMapper().readValue(al.getSerializedData(), Map.class);
			}
			catch (Exception e) {
				fail("Failed to convert serialized data to a map");
			}
		}
		assertEquals(userProperties, propertyNameValueMap.get("userProperties"));
	}
	
	/**
	 * This tests assumes that the owner has other changes
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotLinkChildLogsToThatOfAnEditedOwnerForUpdatedItemsInACollectionMappedAsManyToMany() throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		assertEquals(1, user.getRoles().size());
		Role role = user.getRoles().iterator().next();
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		assertEquals(false, auditLogService.isAudited(Role.class));
		
		auditLogService.startAuditing(User.class);
		auditLogService.startAuditing(Role.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		assertEquals(true, auditLogService.isAudited(Role.class));
		
		user.setUsername("new user name");
		role.setDescription("Testing");
		us.saveUser(user, null);
		
		List<AuditLog> userLogs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		assertEquals(0, userLogs.get(0).getChildAuditLogs().size());
		List<AuditLog> roleLogs = getAllLogs(role.getUuid(), Role.class, Collections.singletonList(UPDATED));
		assertEquals(1, roleLogs.size());
		assertNull(roleLogs.get(0).getParentAuditLog());
	}
	
	/**
	 * This tests assumes that the owner has no other changes
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotMarkOwnerAsUpdatedWhenItemIsUpdatedAndIsInACollectionMappedAsManyToMany() throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		assertEquals(1, user.getRoles().size());
		Role role = user.getRoles().iterator().next();
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		assertEquals(false, auditLogService.isAudited(Role.class));
		
		auditLogService.startAuditing(User.class);
		auditLogService.startAuditing(Role.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		assertEquals(true, auditLogService.isAudited(Role.class));
		
		role.setDescription("Testing");
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED)).size());
		List<AuditLog> roleLogs = getAllLogs(role.getUuid(), Role.class, Collections.singletonList(UPDATED));
		assertEquals(1, roleLogs.size());
		assertNull(roleLogs.get(0).getParentAuditLog());
	}
	
	/**
	 * This tests assumes that the owner has no other changes
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldCreateLogForTheOwnerAndNotTheItemThatIsRemovedFromACollectionMappedAsManyToMany() throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		assertEquals(1, user.getRoles().size());
		Role role = user.getRoles().iterator().next();
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.removeRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		//But should create an audit log for the owner
		List<AuditLog> userLogs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		AuditLog al = userLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
	}
	
	/**
	 * This tests assumes that the owner has other changes
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotCreateLogForAnItemRemovedFromACollectionMappedAsManyToManyAndOwnerIsEdited() throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		assertEquals(1, user.getRoles().size());
		Role role = user.getRoles().iterator().next();
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.setUsername("New");
		user.removeRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		//But should create an audit log for the owner
		List<AuditLog> userLogs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		AuditLog al = userLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
	}
	
	/**
	 * This tests assumes that the owner has no other changes and the collection item is new
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotLinkLogsToThatOfTheOwnerForANewItemAddedToACollectionMappedAsManyToMany() throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		assertEquals(1, user.getRoles().size());
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		assertEquals(false, auditLogService.isAudited(Role.class));
		
		auditLogService.startAuditing(User.class);
		auditLogService.startAuditing(Role.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		assertEquals(true, auditLogService.isAudited(Role.class));
		
		Role role = new Role("new role", "new desc");
		user.addRole(role);
		us.saveUser(user, null);
		
		List<AuditLog> roleLogs = getAllLogs(role.getUuid(), Role.class, Collections.singletonList(CREATED));
		assertEquals(1, roleLogs.size());
		List<AuditLog> userLogs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		AuditLog al = userLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertNull(roleLogs.get(0).getParentAuditLog());
	}
	
	/**
	 * This tests assumes that the owner has no other changes and collection item exists
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotLinkLogsToThatOfTheOwnerForAnExistingItemAddedToACollectionMappedAsManyToMany() throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		Role role = us.getRole("Anonymous");
		assertNotNull(role);
		assertFalse(user.getRoles().contains(role));
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.addRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		List<AuditLog> userLogs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		assertEquals(0, userLogs.get(0).getChildAuditLogs().size());
	}
	
	/**
	 * This tests assumes that the owner has other changes and collection item is new
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotLinkLogsToThatOfTheOwnerForANewItemAddedToACollectionMappedAsManyToManyAndOwnerIsEdited()
	    throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		assertEquals(1, user.getRoles().size());
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		assertEquals(false, auditLogService.isAudited(Role.class));
		
		auditLogService.startAuditing(User.class);
		auditLogService.startAuditing(Role.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		assertEquals(true, auditLogService.isAudited(Role.class));
		
		user.setUsername("New");
		Role role = new Role("new role", "new desc");
		user.addRole(role);
		us.saveUser(user, null);
		
		List<AuditLog> roleLogs = getAllLogs(role.getUuid(), Role.class, Collections.singletonList(CREATED));
		assertEquals(1, roleLogs.size());
		List<AuditLog> userLogs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		AuditLog al = userLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertNull(roleLogs.get(0).getParentAuditLog());
	}
	
	/**
	 * This tests assumes that the owner has other changes and collection item exists
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldNotLinkLogsToThatOfTheOwnerForAnExistingItemAddedToACollectionMappedAsManyToManyAndOwnerIsEdited()
	    throws Exception {
		UserService us = Context.getUserService();
		User user = us.getUser(501);
		Role role = us.getRole("Anonymous");
		assertNotNull(role);
		assertFalse(user.getRoles().contains(role));
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.setUsername("New");
		user.addRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getUuid(), Role.class, null).size());
		List<AuditLog> userLogs = getAllLogs(user.getUuid(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		assertEquals(0, userLogs.get(0).getChildAuditLogs().size());
	}
	
	/**
	 * For this test we remove the item by replacing the collection with a nw instance that doesn't
	 * contain it
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldMarkTheParentAsEditedWhenAnItemIsRemovedFromACollectionByReplacingTheCollectionInstance()
	    throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		Location location = ls.getLocation(2);
		ls.saveLocation(location);
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getUuid(), Location.class,
		    Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		assertEquals(2, location.getTags().size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		Iterator<LocationTag> i = location.getTags().iterator();
		LocationTag tagToRemove = i.next();
		String tagUuid = tagToRemove.getUuid();
		Collection<LocationTag> originalColl = location.getTags();
		LocationTag keptTag = i.next();
		location.setTags(Collections.singleton(keptTag));
		
		ls.saveLocation(location);
		
		assertTrue(originalColl != location.getTags());
		assertEquals(1, location.getTags().size());
		assertFalse(location.getTags().contains(tagToRemove));
		List<AuditLog> patientLogs = getAllLogs(location.getUuid(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(tagToRemove.getUuid()));
		assertTrue(AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(keptTag.getUuid()) > -1);
		assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tagToRemove.getUuid()) > -1);
		assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(keptTag.getUuid()) > -1);
		List<AuditLog> tagLogs = getAllLogs(tagUuid, LocationTag.class, null);
		assertEquals(0, tagLogs.size());
	}
	
	/**
	 * For this test we remove the item by replacing the collection with a nw instance that doesn't
	 * contain it
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldMarkTheParentAsEditedWhenAnItemIsAddedToACollectionByReplacingTheCollectionInstance() throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		Location location = ls.getLocation(2);
		ls.saveLocation(location);
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getUuid(), Location.class,
		    Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = location.getTags();
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		LocationTag tagToAdd = ls.getLocationTag(2);
		assertFalse(location.getTags().contains(tagToAdd));
		String tagUuid = tagToAdd.getUuid();
		Collection<LocationTag> originalColl = location.getTags();
		location.setTags(Collections.singleton(tagToAdd));
		
		ls.saveLocation(location);
		
		assertTrue(originalColl != location.getTags());
		assertEquals(1, location.getTags().size());
		assertTrue(location.getTags().contains(tagToAdd));
		List<AuditLog> patientLogs = getAllLogs(location.getUuid(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertTrue(AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(tagToAdd.getUuid()) > -1);
		assertEquals(-1, AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tagToAdd.getUuid()));
		for (LocationTag tag : previousTags) {
			assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(tag.getUuid()));
			assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tag.getUuid()) > -1);
		}
		List<AuditLog> tagLogs = getAllLogs(tagUuid, LocationTag.class, null);
		assertEquals(0, tagLogs.size());
	}
	
	/**
	 * For this test we remove the item by replacing the collection with a new instance that doesn't
	 * contain it
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldMarkTheParentAsEditedWhenItemsAreAddedAndRemovedToAndFromACollectionByReplacingTheCollectionInstance()
	    throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		Location location = ls.getLocation(2);
		ls.saveLocation(location);
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getUuid(), Location.class,
		    Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = location.getTags();
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		Iterator<LocationTag> i = location.getTags().iterator();
		LocationTag tagToRemove = i.next();
		String tagToRemoveUuid = tagToRemove.getUuid();
		LocationTag keptTag = i.next();
		LocationTag tagToAdd = ls.getLocationTag(2);
		assertFalse(location.getTags().contains(tagToAdd));
		String tagToAddUuid = tagToAdd.getUuid();
		Set<LocationTag> newTags = new HashSet<LocationTag>();
		newTags.add(keptTag);
		newTags.add(tagToAdd);
		location.setTags(newTags);
		
		ls.saveLocation(location);
		
		assertTrue(previousTags != location.getTags());
		assertEquals(2, location.getTags().size());
		assertTrue(location.getTags().contains(keptTag));
		assertTrue(location.getTags().contains(tagToAdd));
		List<AuditLog> patientLogs = getAllLogs(location.getUuid(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(tagToRemove.getUuid()));
		assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tagToRemove.getUuid()) > -1);
		assertTrue(AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(keptTag.getUuid()) > -1);
		assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(keptTag.getUuid()) > -1);
		
		assertTrue(AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(tagToAdd.getUuid()) > -1);
		assertEquals(-1, AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tagToAdd.getUuid()));
		
		List<AuditLog> tagLogs = getAllLogs(tagToRemoveUuid, LocationTag.class, null);
		assertEquals(0, tagLogs.size());
		tagLogs = getAllLogs(tagToAddUuid, LocationTag.class, null);
		assertEquals(0, tagLogs.size());
		tagLogs = getAllLogs(keptTag.getUuid(), LocationTag.class, null);
		assertEquals(0, tagLogs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForADetachedParentWhenACollectionNotMappedAsAllDeleteOrphanIsUpdated()
	    throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		ps.savePatient(patient);
		List<AuditLog> existingUpdateLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		int originalCount = patient.getNames().size();
		assertTrue(originalCount > 1);
		
		auditLogService.startAuditing(Patient.class);
		auditLogService.startAuditing(PersonName.class);
		assertTrue(auditLogService.isAudited(PersonName.class));
		PersonName nameToRemove = null;
		for (PersonName name : patient.getNames()) {
			if (!name.isPreferred()) {
				nameToRemove = name;
				break;
			}
		}
		assertNotNull(nameToRemove);
		String nameUuid = nameToRemove.getUuid();
		patient.removeName(nameToRemove);
		Context.evictFromSession(patient);
		ps.savePatient(patient);
		assertEquals(originalCount - 1, patient.getNames().size());
		List<AuditLog> patientLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(originalCount - 1, patient.getNames().size());
		assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("names", al).indexOf(nameToRemove.getUuid()));
		for (PersonName name : patient.getNames()) {
			assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("names", al).indexOf(name.getUuid()) > -1);
		}
		
		List<AuditLog> nameLogs = getAllLogs(nameUuid, PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, nameLogs.size());
		assertEquals(al, nameLogs.get(0).getParentAuditLog());
	}
	
	/**
	 * For this test we remove the item by replacing the collection with a new instance that doesn't
	 * contain it
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForADetachedParentWhenACollectionMappedAsAllDeleteOrphanIsCleared() throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		Location location = ls.getLocation(2);
		ls.saveLocation(location);
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getUuid(), Location.class,
		    Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = location.getTags();
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		location.getTags().clear();
		Context.evictFromSession(location);
		ls.saveLocation(location);
		
		List<AuditLog> patientLogs = getAllLogs(location.getUuid(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		for (LocationTag tag : previousTags) {
			assertEquals(-1, AuditLogUtil.getNewValueOfUpdatedItem("tags", al).indexOf(tag.getUuid()));
			assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tag.getUuid()) > -1);
		}
		List<Class<? extends OpenmrsObject>> classes = new ArrayList<Class<? extends OpenmrsObject>>();
		classes.add(LocationTag.class);
		List<AuditLog.Action> actions = new ArrayList<AuditLog.Action>();
		actions.add(DELETED);
		List<AuditLog> logs = auditLogService.getAuditLogs(classes, actions, null, null, false, null, null);
		assertEquals(0, logs.size());
	}
	
	/**
	 * For this test we remove the item by replacing the collection with a new instance that doesn't
	 * contain it
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForADetachedParentWhenACollectionMappedAsAllDeleteOrphanIsReplaceWithANewInstance()
	    throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		Location location = ls.getLocation(2);
		ls.saveLocation(location);
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getUuid(), Location.class,
		    Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = location.getTags();
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		location.setTags(new HashSet<LocationTag>());
		Context.evictFromSession(location);
		ls.saveLocation(location);
		
		List<AuditLog> patientLogs = getAllLogs(location.getUuid(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("tags", al));
		for (LocationTag tag : previousTags) {
			assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tag.getUuid()) > -1);
		}
		List<Class<? extends OpenmrsObject>> classes = new ArrayList<Class<? extends OpenmrsObject>>();
		classes.add(LocationTag.class);
		List<AuditLog.Action> actions = new ArrayList<AuditLog.Action>();
		actions.add(DELETED);
		List<AuditLog> logs = auditLogService.getAuditLogs(classes, actions, null, null, false, null, null);
		assertEquals(0, logs.size());
	}
	
	/**
	 * For this test we remove the item by replacing the collection with a new instance that doesn't
	 * contain it
	 * 
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForADetachedParentWhenACollectionMappedAsAllDeleteOrphanIsRemovedBySettingItToNull()
	    throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		Location location = ls.getLocation(2);
		ls.saveLocation(location);
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getUuid(), Location.class,
		    Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = location.getTags();
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		location.setTags(null);
		Context.evictFromSession(location);
		ls.saveLocation(location);
		
		List<AuditLog> patientLogs = getAllLogs(location.getUuid(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("tags", al));
		for (LocationTag tag : previousTags) {
			assertTrue(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al).indexOf(tag.getUuid()) > -1);
		}
		List<Class<? extends OpenmrsObject>> classes = new ArrayList<Class<? extends OpenmrsObject>>();
		classes.add(LocationTag.class);
		List<AuditLog.Action> actions = new ArrayList<AuditLog.Action>();
		actions.add(DELETED);
		List<AuditLog> logs = auditLogService.getAuditLogs(classes, actions, null, null, false, null, null);
		assertEquals(0, logs.size());
	}
}
