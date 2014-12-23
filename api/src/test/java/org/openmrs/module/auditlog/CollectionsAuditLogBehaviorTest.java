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

import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptDescription;
import org.openmrs.Location;
import org.openmrs.LocationTag;
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
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.strategy.AuditStrategy;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.springframework.test.annotation.NotTransactional;

/**
 * Contains tests for testing the core functionality of the module
 */
@SuppressWarnings("deprecation")
public class CollectionsAuditLogBehaviorTest extends BaseBehaviorTest {
	
	private List<String> getAsList(String str) throws Exception {
		return new ObjectMapper().readValue(str, List.class);
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnAuditedElementIsRemovedFromAChildCollection() throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		ps.savePatient(patient);
		List<AuditLog> existingUpdateLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED));
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
		Integer nameId = nameToRemove.getId();
		patient.removeName(nameToRemove);
		ps.savePatient(patient);
		assertEquals(originalCount - 1, patient.getNames().size());
		List<AuditLog> patientLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(originalCount - 1, patient.getNames().size());
		assertFalse(getAsList(AuditLogUtil.getNewValueOfUpdatedItem("names", al)).contains(nameToRemove.getId().toString()));
		List<String> previousNameIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("names", al));
		for (PersonName name : patient.getNames()) {
			assertTrue(previousNameIds.contains(name.getId().toString()));
		}
		
		List<AuditLog> nameLogs = getAllLogs(nameId, PersonName.class, Collections.singletonList(DELETED));
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
		List<AuditLog> existingUpdateLogs = getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED));
		int originalCount = concept.getDescriptions().size();
		assertTrue(originalCount == 1);
		Integer previousDescriptionId = concept.getDescription().getId();
		
		auditLogService.startAuditing(ConceptDescription.class);
		assertTrue(auditLogService.isAudited(ConceptDescription.class));
		
		ConceptDescription cd1 = new ConceptDescription("desc1", Locale.ENGLISH);
		cd1.setDateCreated(new Date());
		cd1.setCreator(Context.getAuthenticatedUser());
		concept.addDescription(cd1);
		conceptService.saveConcept(concept);
		
		List<AuditLog> conceptLogs = getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(existingUpdateLogs.size() + 1, conceptLogs.size());
		conceptLogs.removeAll(existingUpdateLogs);
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		assertEquals(AuditLogUtil.getNewValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionId + "\""
		        + AuditLogConstants.SEPARATOR + "\"" + cd1.getId() + "\"]");
		assertEquals(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionId + "\"]");
		
		List<AuditLog> descriptionLogs = getAllLogs(cd1.getId(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionLogs.size());
		assertEquals(al, descriptionLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentObjectWhenAnAuditedElementInAChildCollectionIsUpdated() throws Exception {
		Concept concept = conceptService.getConcept(7);
		assertEquals(0, getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED)).size());
		
		int originalDescriptionCount = concept.getDescriptions().size();
		assertTrue(originalDescriptionCount > 0);
		
		auditLogService.startAuditing(ConceptDescription.class);
		assertTrue(auditLogService.isAudited(ConceptDescription.class));
		
		ConceptDescription description = concept.getDescription();
		description.setDescription("another descr");
		concept = conceptService.saveConcept(concept);
		assertEquals(originalDescriptionCount, concept.getDescriptions().size());
		List<AuditLog> conceptLogs = getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		
		List<AuditLog> descriptionLogs = getAllLogs(description.getId(), ConceptDescription.class,
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
		List<AuditLog> existingUpdateLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED));
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
		Integer nameId = nameToRemove.getId();
		patient.removeName(nameToRemove);
		ps.savePatient(patient);
		assertEquals(originalCount - 1, patient.getNames().size());
		List<AuditLog> patientLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(originalCount - 1, patient.getNames().size());
		assertFalse(getAsList(AuditLogUtil.getNewValueOfUpdatedItem("names", al)).contains(nameToRemove.getId().toString()));
		List<String> previousNameIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("names", al));
		for (PersonName name : patient.getNames()) {
			assertTrue(previousNameIds.contains(name.getId().toString()));
		}
		
		List<AuditLog> nameLogs = getAllLogs(nameId, PersonName.class, Collections.singletonList(DELETED));
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
		List<AuditLog> existingUpdateLogs = getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED));
		int originalCount = concept.getDescriptions().size();
		assertTrue(originalCount == 1);
		Integer previousDescriptionId = concept.getDescription().getId();
		
		ConceptDescription cd1 = new ConceptDescription("desc1", Locale.ENGLISH);
		cd1.setDateCreated(new Date());
		cd1.setCreator(Context.getAuthenticatedUser());
		concept.addDescription(cd1);
		conceptService.saveConcept(concept);
		
		List<AuditLog> conceptLogs = getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(existingUpdateLogs.size() + 1, conceptLogs.size());
		conceptLogs.removeAll(existingUpdateLogs);
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		assertEquals(AuditLogUtil.getNewValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionId + "\""
		        + AuditLogConstants.SEPARATOR + "\"" + cd1.getId() + "\"]");
		assertEquals(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", al), "[\"" + previousDescriptionId + "\"]");
		
		List<AuditLog> descriptionLogs = getAllLogs(cd1.getId(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionLogs.size());
		assertEquals(al, descriptionLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentObjectWhenAnUnAuditedElementInAChildCollectionIsUpdated() throws Exception {
		assertFalse(auditLogService.isAudited(ConceptDescription.class));
		Concept concept = conceptService.getConcept(7);
		assertEquals(0, getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED)).size());
		
		int originalDescriptionCount = concept.getDescriptions().size();
		assertTrue(originalDescriptionCount > 0);
		
		ConceptDescription description = concept.getDescription();
		description.setDescription("another descr");
		concept = conceptService.saveConcept(concept);
		assertEquals(originalDescriptionCount, concept.getDescriptions().size());
		List<AuditLog> conceptLogs = getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED));
		assertEquals(1, conceptLogs.size());
		AuditLog al = conceptLogs.get(0);
		
		List<AuditLog> descriptionLogs = getAllLogs(description.getId(), ConceptDescription.class,
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
		int count = getAllLogs(c.getId(), Concept.class, actions).size();
		assertEquals(4, c.getDescriptions().size());
		Iterator<ConceptDescription> it = c.getDescriptions().iterator();
		Integer descriptionId1 = it.next().getId();
		Integer descriptionId2 = it.next().getId();
		Integer descriptionId3 = it.next().getId();
		Integer descriptionId4 = it.next().getId();
		c.getDescriptions().clear();
		conceptService.saveConcept(c);
		
		List<AuditLog> logs = getAllLogs(c.getId(), Concept.class, actions);
		int newCount = logs.size();
		assertEquals(++count, newCount);
		AuditLog log = logs.get(0);
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("descriptions", log));
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionId1) > -1);
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionId2) > -1);
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionId3) > -1);
		assertNotNull(AuditLogUtil.getPreviousValueOfUpdatedItem("descriptions", log).indexOf(descriptionId4) > -1);
	}
	
	@Test
	@NotTransactional
	public void shouldCreateLogForUnAuditedTypeIfTheOwningTypeIsAuditedAndStrategyIsAllExcept() throws Exception {
		setAuditConfiguration(AuditStrategy.ALL_EXCEPT, null, false);
		assertEquals(AuditStrategy.ALL_EXCEPT, auditLogService.getAuditingStrategy());
		assertEquals(true, auditLogService.isAudited(Person.class));
		assertEquals(true, auditLogService.isAudited(PersonAddress.class));
		
		auditLogService.stopAuditing(PersonAddress.class);
		assertEquals(false, auditLogService.isAudited(PersonAddress.class));
		PersonService ps = Context.getPersonService();
		Person person = ps.getPerson(2);
		PersonAddress address = person.getPersonAddress();
		address.setAddress1("new");
		ps.savePerson(person);
		List<AuditLog> addressLogs = getAllLogs(address.getId(), PersonAddress.class, Collections.singletonList(UPDATED));
		assertEquals(1, addressLogs.size());
		List<AuditLog> personLogs = getAllLogs(person.getId(), Person.class, Collections.singletonList(UPDATED));
		assertEquals(1, personLogs.size());
		assertEquals(personLogs.get(0), addressLogs.get(0).getParentAuditLog());
	}
	
	@Test
	@NotTransactional
	public void shouldLinkTheLogsOfCollectionItemsToThatOfTheUpdatedParent() throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(PersonName.class);
		classes.add(Patient.class);
		
		startAuditing(classes);
		patient = ps.savePatient(patient);
		//Ensure that no log will be created unless we actually perform an update
		assertEquals(0, getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED)).size());
		
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
		
		List<AuditLog> personNameAuditLogs1 = getAllLogs(name1.getId(), PersonName.class, Collections.singletonList(UPDATED));
		assertEquals(1, personNameAuditLogs1.size());
		AuditLog personNameAuditLog1 = personNameAuditLogs1.get(0);
		assertNotNull(personNameAuditLog1.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs2 = getAllLogs(name2.getId(), PersonName.class, Collections.singletonList(UPDATED));
		assertEquals(1, personNameAuditLogs2.size());
		AuditLog personNameAuditLog2 = personNameAuditLogs2.get(0);
		assertNotNull(personNameAuditLog2.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs3 = getAllLogs(name3.getId(), PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, personNameAuditLogs3.size());
		AuditLog personNameAuditLog3 = personNameAuditLogs3.get(0);
		assertNotNull(personNameAuditLog3.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs4 = getAllLogs(name4.getId(), PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, personNameAuditLogs4.size());
		AuditLog personNameAuditLog4 = personNameAuditLogs4.get(0);
		assertNotNull(personNameAuditLog4.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs5 = getAllLogs(name5.getId(), PersonName.class, Collections.singletonList(CREATED));
		assertEquals(1, personNameAuditLogs5.size());
		AuditLog personNameAuditLog5 = personNameAuditLogs5.get(0);
		assertNotNull(personNameAuditLog5.getParentAuditLog());
		
		List<AuditLog> personNameAuditLogs6 = getAllLogs(name6.getId(), PersonName.class, Collections.singletonList(CREATED));
		assertEquals(1, personNameAuditLogs6.size());
		AuditLog personNameAuditLog6 = personNameAuditLogs6.get(0);
		assertNotNull(personNameAuditLog6.getParentAuditLog());
		
		List<AuditLog> patientAuditLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED)).size());
		int originalDescriptionCount = concept.getDescriptions().size();
		assertTrue(originalDescriptionCount > 3);
		
		auditLogService.startAuditing(ConceptDescription.class);
		concept = conceptService.saveConcept(concept);
		//Ensure that no log will be created unless we actually perform an update
		assertEquals(0, getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED)).size());
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
		List<AuditLog> descriptionAuditLogs1 = getAllLogs(cd1.getId(), ConceptDescription.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, descriptionAuditLogs1.size());
		AuditLog descriptionAuditLog1 = descriptionAuditLogs1.get(0);
		assertNotNull(descriptionAuditLog1.getParentAuditLog());
		
		List<AuditLog> descriptionAuditLogs2 = getAllLogs(cd2.getId(), ConceptDescription.class,
		    Collections.singletonList(UPDATED));
		assertEquals(1, descriptionAuditLogs2.size());
		AuditLog descriptionAuditLog2 = descriptionAuditLogs2.get(0);
		assertNotNull(descriptionAuditLog2.getParentAuditLog());
		
		List<AuditLog> descriptionAuditLogs3 = getAllLogs(cd3.getId(), ConceptDescription.class,
		    Collections.singletonList(DELETED));
		//This is because concept.descriptions cascade option doesn't say delete-orphan
		assertEquals(0, descriptionAuditLogs3.size());
		
		List<AuditLog> descriptionAuditLogs4 = getAllLogs(cd4.getId(), ConceptDescription.class,
		    Collections.singletonList(DELETED));
		assertEquals(0, descriptionAuditLogs4.size());//same here
		
		List<AuditLog> descriptionAuditLogs5 = getAllLogs(cd5.getId(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionAuditLogs5.size());
		AuditLog descriptionAuditLog5 = descriptionAuditLogs5.get(0);
		assertNotNull(descriptionAuditLog5.getParentAuditLog());
		
		List<AuditLog> descriptionAuditLogs6 = getAllLogs(cd6.getId(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionAuditLogs6.size());
		AuditLog descriptionAuditLog6 = descriptionAuditLogs6.get(0);
		assertNotNull(descriptionAuditLog6.getParentAuditLog());
		
		List<AuditLog> conceptAuditLogs = getAllLogs(concept.getId(), Concept.class, Collections.singletonList(UPDATED));
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
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(PersonName.class);
		classes.add(PersonAttribute.class);
		classes.add(PersonAddress.class);
		classes.add(PatientIdentifier.class);
		classes.add(Patient.class);
		
		startAuditing(classes);
		patient = ps.savePatient(patient);
		//Ensure that no log will be created unless we actually perform an update
		assertEquals(0, getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED)).size());
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
		
		List<AuditLog> personName1AuditLogs = getAllLogs(name1.getId(), PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, personName1AuditLogs.size());
		AuditLog personName1AuditLog = personName1AuditLogs.get(0);
		assertNotNull(personName1AuditLog.getParentAuditLog());
		
		List<AuditLog> personName2AuditLogs = getAllLogs(name2.getId(), PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, personName2AuditLogs.size());
		AuditLog personName2AuditLog = personName2AuditLogs.get(0);
		assertNotNull(personName2AuditLog.getParentAuditLog());
		
		List<AuditLog> personName3AuditLogs = getAllLogs(name3.getId(), PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, personName3AuditLogs.size());
		AuditLog personName3AuditLog = personName3AuditLogs.get(0);
		assertNotNull(personName3AuditLog.getParentAuditLog());
		
		List<AuditLog> personName4AuditLogs = getAllLogs(name4.getId(), PersonName.class, Collections.singletonList(DELETED));
		assertEquals(1, personName4AuditLogs.size());
		AuditLog personName4AuditLog = personName4AuditLogs.get(0);
		assertNotNull(personName4AuditLog.getParentAuditLog());
		
		List<AuditLog> addressAuditLogs = getAllLogs(address.getId(), PersonAddress.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, addressAuditLogs.size());
		AuditLog addressAuditLog = addressAuditLogs.get(0);
		assertNotNull(addressAuditLog.getParentAuditLog());
		
		List<AuditLog> id1AuditLogs = getAllLogs(identifier1.getId(), PatientIdentifier.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, id1AuditLogs.size());
		AuditLog id1AuditLog = id1AuditLogs.get(0);
		assertNotNull(id1AuditLog.getParentAuditLog());
		
		List<AuditLog> id2AuditLogs = getAllLogs(identifier2.getId(), PatientIdentifier.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, id2AuditLogs.size());
		AuditLog id2AuditLog = id2AuditLogs.get(0);
		assertNotNull(id2AuditLog.getParentAuditLog());
		
		List<AuditLog> attribute1AuditLogs = getAllLogs(attribute1.getId(), PersonAttribute.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, attribute1AuditLogs.size());
		AuditLog attribute1AuditLog = attribute1AuditLogs.get(0);
		assertNotNull(attribute1AuditLog.getParentAuditLog());
		
		List<AuditLog> attribute2AuditLogs = getAllLogs(attribute2.getId(), PersonAttribute.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, attribute2AuditLogs.size());
		AuditLog attribute2AuditLog = attribute2AuditLogs.get(0);
		assertNotNull(attribute2AuditLog.getParentAuditLog());
		
		List<AuditLog> attribute3AuditLogs = getAllLogs(attribute3.getId(), PersonAttribute.class,
		    Collections.singletonList(DELETED));
		assertEquals(1, attribute3AuditLogs.size());
		AuditLog attribute3AuditLog = attribute3AuditLogs.get(0);
		assertNotNull(attribute3AuditLog.getParentAuditLog());
		
		List<AuditLog> patientAuditLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(DELETED));
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
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
		List<AuditLog> logs = getAllLogs(user.getId(), User.class, null);
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		user.getUserProperties().clear();//since it is 1, just clear
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getId(), User.class, null);
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
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
		List<AuditLog> logs = getAllLogs(user.getId(), User.class, null);
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		user.setUserProperties(null);
		//Should work even for detached owners
		Context.evictFromSession(user);
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getId(), User.class, null);
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		//We are setting the same original value but the string are new objects
		user.setUserProperty(key, value);
		us.saveUser(user, null);
		List<AuditLog> logs = getAllLogs(user.getId(), User.class, null);
		assertEquals(0, logs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldSerializeMapEntriesAsSerializedDataForADeletedItem() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_STORE_LAST_STATE_OF_DELETED_ITEMS, "true");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		auditLogService.startAuditing(User.class);
		Map.Entry<String, String> entry = user.getUserProperties().entrySet().iterator().next();
		String userProperties = "{\"" + entry.getKey() + "\"" + MAP_KEY_VALUE_SEPARATOR + "\"" + entry.getValue() + "\"}";
		assertEquals(true, auditLogService.isAudited(User.class));
		us.purgeUser(user);
		List<AuditLog> logs = getAllLogs(user.getId(), User.class, Collections.singletonList(DELETED));
		assertEquals(1, logs.size());
		AuditLog al = logs.get(0);
		String serializedData = AuditLogUtil.getAsString(al.getSerializedData());
		Map<String, Object> propertyNameValueMap = new ObjectMapper().readValue(serializedData, Map.class);
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
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
		
		List<AuditLog> userLogs = getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED));
		assertEquals(1, userLogs.size());
		assertEquals(0, userLogs.get(0).getChildAuditLogs().size());
		List<AuditLog> roleLogs = getAllLogs(role.getRole(), Role.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
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
		
		assertEquals(0, getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED)).size());
		List<AuditLog> roleLogs = getAllLogs(role.getRole(), Role.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.removeRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
		//But should create an audit log for the owner
		List<AuditLog> userLogs = getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.setUsername("New");
		user.removeRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
		//But should create an audit log for the owner
		List<AuditLog> userLogs = getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
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
		
		List<AuditLog> roleLogs = getAllLogs(role.getRole(), Role.class, Collections.singletonList(CREATED));
		assertEquals(1, roleLogs.size());
		List<AuditLog> userLogs = getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.addRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
		List<AuditLog> userLogs = getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
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
		
		List<AuditLog> roleLogs = getAllLogs(role.getRole(), Role.class, Collections.singletonList(CREATED));
		assertEquals(1, roleLogs.size());
		List<AuditLog> userLogs = getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED));
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
		assertEquals(0, getAllLogs(user.getId(), User.class, null).size());
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("roles", User.class, null);
		assertTrue(cp.isManyToMany());
		assertEquals(false, auditLogService.isAudited(User.class));
		
		auditLogService.startAuditing(User.class);
		assertEquals(true, auditLogService.isAudited(User.class));
		
		user.setUsername("New");
		user.addRole(role);
		us.saveUser(user, null);
		
		assertEquals(0, getAllLogs(role.getRole(), Role.class, null).size());
		List<AuditLog> userLogs = getAllLogs(user.getId(), User.class, Collections.singletonList(UPDATED));
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
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		assertEquals(2, location.getTags().size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		Iterator<LocationTag> i = location.getTags().iterator();
		LocationTag tagToRemove = i.next();
		Integer tagId = tagToRemove.getId();
		Collection<LocationTag> originalColl = location.getTags();
		LocationTag keptTag = i.next();
		location.setTags(Collections.singleton(keptTag));
		
		ls.saveLocation(location);
		
		assertTrue(originalColl != location.getTags());
		assertEquals(1, location.getTags().size());
		assertFalse(location.getTags().contains(tagToRemove));
		List<AuditLog> patientLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertFalse(getAsList(AuditLogUtil.getNewValueOfUpdatedItem("tags", al)).contains(tagToRemove.getId().toString()));
		assertTrue(getAsList(AuditLogUtil.getNewValueOfUpdatedItem("tags", al)).contains(keptTag.getId().toString()));
		List<String> previousTagIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al));
		assertTrue(previousTagIds.contains(tagToRemove.getId().toString()));
		assertTrue(previousTagIds.contains(keptTag.getId().toString()));
		List<AuditLog> tagLogs = getAllLogs(tagId, LocationTag.class, null);
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
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = new HashSet<LocationTag>();
		previousTags.addAll(location.getTags());
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		LocationTag tagToAdd = ls.getLocationTag(2);
		assertFalse(location.getTags().contains(tagToAdd));
		Integer tagId = tagToAdd.getId();
		Collection<LocationTag> originalColl = location.getTags();
		location.setTags(Collections.singleton(tagToAdd));
		
		ls.saveLocation(location);
		
		assertTrue(originalColl != location.getTags());
		assertEquals(1, location.getTags().size());
		assertTrue(location.getTags().contains(tagToAdd));
		List<AuditLog> patientLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		List<String> newTagIds = getAsList(AuditLogUtil.getNewValueOfUpdatedItem("tags", al));
		List<String> previousTagIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al));
		assertTrue(newTagIds.contains(tagToAdd.getId().toString()));
		assertFalse(previousTagIds.contains(tagToAdd.getId().toString()));
		for (LocationTag tag : previousTags) {
			assertFalse(newTagIds.contains(tag.getId().toString()));
			assertTrue(previousTagIds.contains(tag.getId().toString()));
		}
		List<AuditLog> tagLogs = getAllLogs(tagId, LocationTag.class, null);
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
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = new HashSet<LocationTag>();
		previousTags.addAll(location.getTags());
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		Iterator<LocationTag> i = location.getTags().iterator();
		LocationTag tagToRemove = i.next();
		Integer tagToRemoveId = tagToRemove.getId();
		LocationTag keptTag = i.next();
		LocationTag tagToAdd = ls.getLocationTag(2);
		assertFalse(location.getTags().contains(tagToAdd));
		Integer tagToAddId = tagToAdd.getId();
		Set<LocationTag> newTags = new HashSet<LocationTag>();
		newTags.add(keptTag);
		newTags.add(tagToAdd);
		location.setTags(newTags);
		
		ls.saveLocation(location);
		
		assertTrue(previousTags != location.getTags());
		assertEquals(2, location.getTags().size());
		assertTrue(location.getTags().contains(keptTag));
		assertTrue(location.getTags().contains(tagToAdd));
		List<AuditLog> patientLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		List<String> previousTagIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al));
		List<String> newTagIds = getAsList(AuditLogUtil.getNewValueOfUpdatedItem("tags", al));
		assertFalse(newTagIds.contains(tagToRemove.getId().toString()));
		assertTrue(previousTagIds.contains(tagToRemove.getId().toString()));
		assertTrue(newTagIds.contains(keptTag.getId().toString()));
		assertTrue(previousTagIds.contains(keptTag.getId().toString()));
		
		assertTrue(newTagIds.contains(tagToAdd.getId().toString()));
		assertFalse(previousTagIds.contains(tagToAdd.getId().toString()));
		
		List<AuditLog> tagLogs = getAllLogs(tagToRemoveId, LocationTag.class, null);
		assertEquals(0, tagLogs.size());
		tagLogs = getAllLogs(tagToAddId, LocationTag.class, null);
		assertEquals(0, tagLogs.size());
		tagLogs = getAllLogs(keptTag.getId(), LocationTag.class, null);
		assertEquals(0, tagLogs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForADetachedParentWhenACollectionNotMappedAsAllDeleteOrphanIsUpdated()
	    throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		ps.savePatient(patient);
		List<AuditLog> existingUpdateLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED));
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
		Integer nameId = nameToRemove.getId();
		patient.removeName(nameToRemove);
		Context.evictFromSession(patient);
		ps.savePatient(patient);
		assertEquals(originalCount - 1, patient.getNames().size());
		List<AuditLog> patientLogs = getAllLogs(patient.getId(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(originalCount - 1, patient.getNames().size());
		assertFalse(getAsList(AuditLogUtil.getNewValueOfUpdatedItem("names", al)).contains(nameToRemove.getId().toString()));
		List<String> previousNameIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("names", al));
		for (PersonName name : patient.getNames()) {
			assertTrue(previousNameIds.contains(name.getId().toString()));
		}
		
		List<AuditLog> nameLogs = getAllLogs(nameId, PersonName.class, Collections.singletonList(DELETED));
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
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = new HashSet<LocationTag>();
		previousTags.addAll(location.getTags());
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		location.getTags().clear();
		Context.evictFromSession(location);
		ls.saveLocation(location);
		
		List<AuditLog> locationLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, locationLogs.size());
		AuditLog al = locationLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("tags", al));
		List<String> previousTagIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al));
		for (LocationTag tag : previousTags) {
			assertTrue(previousTagIds.contains(tag.getId().toString()));
		}
		List<Class<?>> classes = new ArrayList<Class<?>>();
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
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = new HashSet<LocationTag>();
		previousTags.addAll(location.getTags());
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		location.setTags(new HashSet<LocationTag>());
		Context.evictFromSession(location);
		ls.saveLocation(location);
		
		List<AuditLog> patientLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("tags", al));
		List<String> previousTagIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al));
		for (LocationTag tag : previousTags) {
			assertTrue(previousTagIds.contains(tag.getId().toString()));
		}
		List<Class<?>> classes = new ArrayList<Class<?>>();
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
		List<AuditLog> existingUpdateLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		Set<LocationTag> previousTags = new HashSet<LocationTag>();
		previousTags.addAll(location.getTags());
		assertEquals(2, previousTags.size());
		
		auditLogService.startAuditing(Location.class);
		auditLogService.startAuditing(LocationTag.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertTrue(auditLogService.isAudited(LocationTag.class));
		location.setTags(null);
		Context.evictFromSession(location);
		ls.saveLocation(location);
		
		List<AuditLog> patientLogs = getAllLogs(location.getId(), Location.class, Collections.singletonList(UPDATED));
		assertEquals(1, patientLogs.size());
		AuditLog al = patientLogs.get(0);
		assertEquals(0, al.getChildAuditLogs().size());
		assertNull(AuditLogUtil.getNewValueOfUpdatedItem("tags", al));
		List<String> previousTagIds = getAsList(AuditLogUtil.getPreviousValueOfUpdatedItem("tags", al));
		for (LocationTag tag : previousTags) {
			assertTrue(previousTagIds.contains(tag.getId().toString()));
		}
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add(LocationTag.class);
		List<AuditLog.Action> actions = new ArrayList<AuditLog.Action>();
		actions.add(DELETED);
		List<AuditLog> logs = auditLogService.getAuditLogs(classes, actions, null, null, false, null, null);
		assertEquals(0, logs.size());
	}
}
