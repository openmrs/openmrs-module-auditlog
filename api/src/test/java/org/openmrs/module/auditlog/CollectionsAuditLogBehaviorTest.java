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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
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
import org.springframework.test.annotation.NotTransactional;

/**
 * Contains tests for testing the core functionality of the module
 */
@SuppressWarnings("deprecation")
public class CollectionsAuditLogBehaviorTest extends BaseBehaviorTest {
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnUnMonitoredElementIsRemovedFromAChildCollection() throws Exception {
		assertFalse(auditLogService.isMonitored(PersonName.class));
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		ps.savePatient(patient);
		List<AuditLog> existingUpdateLogs = getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED));
		assertEquals(0, existingUpdateLogs.size());
		int originalCount = patient.getNames().size();
		assertTrue(originalCount > 1);
		
		auditLogService.startMonitoring(Patient.class);
		try {
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
			assertEquals(al.getObjectUuid(), patient.getUuid());
			assertEquals(originalCount - 1, patient.getNames().size());
			assertEquals(-1, al.getNewValue("names").indexOf(nameToRemove.getUuid()));
			for (PersonName name : patient.getNames()) {
				assertTrue(al.getPreviousValue("names").indexOf(name.getUuid()) > -1);
			}
			List<AuditLog> nameLogs = getAllLogs(nameUuid, PersonName.class, Collections.singletonList(DELETED));
			assertEquals(1, nameLogs.size());
		}
		finally {
			auditLogService.stopMonitoring(Patient.class);
		}
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForAParentWhenAnUnMonitoredElementIsAddedToAChildCollection() throws Exception {
		assertFalse(auditLogService.isMonitored(ConceptDescription.class));
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
		assertEquals(al.getObjectUuid(), concept.getUuid());
		assertEquals(al.getNewValue("descriptions"), previousDescriptionUuids + AuditLogConstants.SEPARATOR
		        + AuditLogConstants.UUID_LABEL + cd1.getUuid());
		assertEquals(al.getPreviousValue("descriptions"), previousDescriptionUuids);
		
		List<AuditLog> descriptionLogs = getAllLogs(cd1.getUuid(), ConceptDescription.class,
		    Collections.singletonList(CREATED));
		assertEquals(1, descriptionLogs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentObjectWhenAnUnMonitoredElementInAChildCollectionIsUpdated()
	    throws Exception {
		assertFalse(auditLogService.isMonitored(ConceptDescription.class));
		Concept concept = conceptService.getConcept(7);
		assertEquals(0, getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED)).size());
		
		int originalDescriptionCount = concept.getDescriptions().size();
		assertTrue(originalDescriptionCount > 0);
		
		concept.getDescription().setDescription("another descr");
		concept = conceptService.saveConcept(concept);
		assertEquals(originalDescriptionCount, concept.getDescriptions().size());
		
		assertEquals(1, getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED)).size());
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
		assertNull(log.getNewValue("descriptions"));
		assertNotNull(log.getPreviousValue("descriptions").indexOf(descriptionUuid1) > -1);
		assertNotNull(log.getPreviousValue("descriptions").indexOf(descriptionUuid2) > -1);
		assertNotNull(log.getPreviousValue("descriptions").indexOf(descriptionUuid3) > -1);
		assertNotNull(log.getPreviousValue("descriptions").indexOf(descriptionUuid4) > -1);
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
			assertTrue(auditLogService.isMonitored(Cohort.class));
			assertFalse(c.contains(memberId));
			c.addMember(memberId);
			cs.saveCohort(c);
			
			List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
			int newCount = logs.size();
			assertEquals(++count, newCount);
			assertTrue(logs.get(0).getNewValue("memberIds").indexOf(memberId.toString()) > -1);
			assertEquals(-1, logs.get(0).getPreviousValue("memberIds").indexOf(memberId.toString()));
		}
		finally {
			auditLogService.stopMonitoring(Cohort.class);
		}
		assertFalse(auditLogService.isMonitored(Cohort.class));
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
			assertTrue(auditLogService.isMonitored(Cohort.class));
			assertTrue(c.contains(memberId));
			c.removeMember(memberId);
			cs.saveCohort(c);
			
			List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
			int newCount = logs.size();
			assertEquals(++count, newCount);
			assertEquals(-1, logs.get(0).getNewValue("memberIds").indexOf(memberId.toString()));
			assertTrue(logs.get(0).getPreviousValue("memberIds").indexOf(memberId.toString()) > -1);
		}
		finally {
			auditLogService.stopMonitoring(Cohort.class);
		}
		assertFalse(auditLogService.isMonitored(Cohort.class));
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
			assertTrue(auditLogService.isMonitored(Cohort.class));
			assertEquals(2, c.getMemberIds().size());
			assertTrue(c.contains(memberId2));
			assertTrue(c.contains(memberId3));
			c.getMemberIds().clear();
			cs.saveCohort(c);
			
			List<AuditLog> logs = getAllLogs(c.getUuid(), Cohort.class, actions);
			int newCount = logs.size();
			assertEquals(++count, newCount);
			AuditLog log = logs.get(0);
			assertNull(log.getNewValue("memberIds"));
			assertTrue(log.getPreviousValue("memberIds").indexOf(memberId2.toString()) > -1);
			assertTrue(log.getPreviousValue("memberIds").indexOf(memberId3.toString()) > -1);
		}
		finally {
			auditLogService.stopMonitoring(Cohort.class);
		}
		assertFalse(auditLogService.isMonitored(Cohort.class));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateLogForUnMonitoredTypeIfTheOwningTypeIsMonitoredAndStrategyIsAllExcept() throws Exception {
		executeDataSet("org/openmrs/api/include/LocationServiceTest-initialData.xml");
		LocationService ls = Context.getLocationService();
		assertEquals(0, getAllLogs().size());
		
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.GP_MONITORING_STRATEGY);
		gp.setPropertyValue(MonitoringStrategy.ALL_EXCEPT.name());
		as.saveGlobalProperty(gp);
		assertEquals(MonitoringStrategy.ALL_EXCEPT, auditLogService.getMonitoringStrategy());
		assertEquals(true, auditLogService.isMonitored(Location.class));
		assertEquals(true, auditLogService.isMonitored(LocationTag.class));
		
		auditLogService.stopMonitoring(LocationTag.class);
		assertEquals(false, auditLogService.isMonitored(LocationTag.class));
		Location loc = ls.getLocation(2);
		LocationTag tag = loc.getTags().iterator().next();
		tag.setDescription("new");
		ls.saveLocation(loc);
		assertEquals(1, getAllLogs(tag.getUuid(), LocationTag.class, Collections.singletonList(UPDATED)).size());
		assertEquals(1, getAllLogs(loc.getUuid(), Location.class, Collections.singletonList(UPDATED)).size());
	}
	
	@Test
	@NotTransactional
	public void shouldLinkTheLogsOfCollectionItemsToThatOfTheUpdatedParent() throws Exception {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(2);
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(PersonName.class);
		classes.add(Patient.class);
		
		auditLogService.startMonitoring(classes);
		try {
			patient = ps.savePatient(patient);
			//Ensure that no log will be created unless we actually perform an update
			assertEquals(0, getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED)).size());
			
			int originalDescriptionCount = patient.getNames().size();
			assertTrue(originalDescriptionCount > 3);
			
			assertTrue(auditLogService.isMonitored(PersonName.class));
			assertTrue(auditLogService.isMonitored(Patient.class));
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
			
			List<AuditLog> patientAuditLogs = getAllLogs(patient.getUuid(), Patient.class,
			    Collections.singletonList(UPDATED));
			assertEquals(1, patientAuditLogs.size());
			assertEquals(6, patientAuditLogs.get(0).getChildAuditLogs().size());
			
			assertEquals(patientAuditLogs.get(0), personNameAuditLog1.getParentAuditLog());
			assertEquals(patientAuditLogs.get(0), personNameAuditLog2.getParentAuditLog());
			assertEquals(patientAuditLogs.get(0), personNameAuditLog3.getParentAuditLog());
			assertEquals(patientAuditLogs.get(0), personNameAuditLog4.getParentAuditLog());
			assertEquals(patientAuditLogs.get(0), personNameAuditLog5.getParentAuditLog());
			assertEquals(patientAuditLogs.get(0), personNameAuditLog6.getParentAuditLog());
		}
		finally {
			auditLogService.stopMonitoring(classes);
		}
		assertFalse(auditLogService.isMonitored(PersonName.class));
		assertFalse(auditLogService.isMonitored(Patient.class));
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
		
		auditLogService.startMonitoring(ConceptDescription.class);
		try {
			concept = conceptService.saveConcept(concept);
			//Ensure that no log will be created unless we actually perform an update
			assertEquals(0, getAllLogs(concept.getUuid(), Concept.class, Collections.singletonList(UPDATED)).size());
			assertTrue(auditLogService.isMonitored(ConceptDescription.class));
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
			
			List<AuditLog> conceptAuditLogs = getAllLogs(concept.getUuid(), Concept.class,
			    Collections.singletonList(UPDATED));
			assertEquals(1, conceptAuditLogs.size());
			assertEquals(4, conceptAuditLogs.get(0).getChildAuditLogs().size());
			
			assertEquals(conceptAuditLogs.get(0), descriptionAuditLog1.getParentAuditLog());
			assertEquals(conceptAuditLogs.get(0), descriptionAuditLog2.getParentAuditLog());
		}
		finally {
			auditLogService.stopMonitoring(ConceptDescription.class);
		}
		assertFalse(auditLogService.isMonitored(ConceptDescription.class));
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
		
		auditLogService.startMonitoring(classes);
		try {
			patient = ps.savePatient(patient);
			//Ensure that no log will be created unless we actually perform an update
			assertEquals(0, getAllLogs(patient.getUuid(), Patient.class, Collections.singletonList(UPDATED)).size());
			assertTrue(auditLogService.isMonitored(PersonName.class));
			assertTrue(auditLogService.isMonitored(Patient.class));
			
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
			
			List<AuditLog> patientAuditLogs = getAllLogs(patient.getUuid(), Patient.class,
			    Collections.singletonList(DELETED));
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
		finally {
			auditLogService.stopMonitoring(classes);
		}
		assertFalse(auditLogService.isMonitored(PersonName.class));
		assertFalse(auditLogService.isMonitored(Patient.class));
		assertFalse(auditLogService.isMonitored(PersonAddress.class));
		assertFalse(auditLogService.isMonitored(PatientIdentifier.class));
		assertFalse(auditLogService.isMonitored(PersonAttribute.class));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentWhenAnEntryIsAddedToAMapProperty() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		Map.Entry<String, String> entry = user.getUserProperties().entrySet().iterator().next();
		String previousUserProperties = entry.getKey() + MAP_KEY_VALUE_SEPARATOR + entry.getValue();
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startMonitoring(User.class);
		try {
			assertEquals(true, auditLogService.isMonitored(User.class));
			final String newPropKey1 = "locale";
			final String newPropValue1 = "fr";
			final String newPropKey2 = "loginAttempts";
			final String newPropValue2 = "2";
			user.setUserProperty(newPropKey1, newPropValue1);
			user.setUserProperty(newPropKey2, newPropValue2);
			us.saveUser(user, null);
			List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, null);
			assertEquals(1, logs.size());
			AuditLog al = logs.get(0);
			assertEquals(previousUserProperties, al.getPreviousValue("userProperties"), previousUserProperties);
			String expectedNewUserProperties = previousUserProperties + SEPARATOR + newPropKey1 + MAP_KEY_VALUE_SEPARATOR
			        + newPropValue1 + SEPARATOR + newPropKey2 + MAP_KEY_VALUE_SEPARATOR + newPropValue2;
			assertEquals(expectedNewUserProperties, al.getNewValue("userProperties"));
		}
		finally {
			auditLogService.stopMonitoring(User.class);
		}
		assertEquals(false, auditLogService.isMonitored(User.class));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogForTheParentWhenAnEntryIsRemovedFromAMapProperty() throws Exception {
		executeDataSet("org/openmrs/api/include/UserServiceTest.xml");
		UserService us = Context.getUserService();
		User user = us.getUser(505);
		assertEquals(1, user.getUserProperties().size());
		Map.Entry<String, String> entry = user.getUserProperties().entrySet().iterator().next();
		String previousUserProperties = entry.getKey() + MAP_KEY_VALUE_SEPARATOR + entry.getValue();
		assertEquals(0, getAllLogs(user.getUuid(), User.class, null).size());
		auditLogService.startMonitoring(User.class);
		try {
			assertEquals(true, auditLogService.isMonitored(User.class));
			user.getUserProperties().clear();//since it is 1, just clear
			us.saveUser(user, null);
			List<AuditLog> logs = getAllLogs(user.getUuid(), User.class, null);
			assertEquals(1, logs.size());
			AuditLog al = logs.get(0);
			assertEquals(previousUserProperties, al.getPreviousValue("userProperties"), previousUserProperties);
			assertNull(al.getNewValue("userProperties"));
		}
		finally {
			auditLogService.stopMonitoring(User.class);
		}
		assertEquals(false, auditLogService.isMonitored(User.class));
	}
}
