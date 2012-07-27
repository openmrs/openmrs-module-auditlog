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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.annotation.NotTransactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@SuppressWarnings("deprecation")
public class AuditLogBehaviorTest extends BaseModuleContextSensitiveTest {
	
	private static final String MODULE_TEST_DATA_MONITORED_OBJS = "moduleTestData.xml";
	
	private ConceptService conceptService;
	
	private AuditLogService auditLogService;
	
	private EncounterService encounterService;
	
	@Before
	public void before() throws Exception {
		executeDataSet(MODULE_TEST_DATA_MONITORED_OBJS);
		conceptService = Context.getConceptService();
		encounterService = Context.getEncounterService();
		auditLogService = Context.getService(AuditLogService.class);
		
		//No log entries should be existing
		Assert.assertTrue(auditLogService.getAuditLogs(null, null, null, null, null, null).isEmpty());
	}
	
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
		List<AuditLog> logs = auditLogService.getAuditLogs(null, null, null, null, null, null);
		Assert.assertNotNull(concept.getConceptId());
		//Should have created an entry for the concept and concept name
		Assert.assertEquals(2, logs.size());
		//The latest logs come first
		Assert.assertEquals(Action.CREATED, logs.get(0).getAction());
		Assert.assertEquals(Action.CREATED, logs.get(1).getAction());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogEntryWhenAnObjectIsDeleted() throws Exception {
		EncounterType encounterType = encounterService.getEncounterType(6);
		encounterService.purgeEncounterType(encounterType);
		List<AuditLog> logs = auditLogService.getAuditLogs(null, null, null, null, null, null);
		//Should have created a log entry for deleted Encounter type
		Assert.assertEquals(1, logs.size());
		Assert.assertEquals(Action.DELETED, logs.get(0).getAction());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogEntryWhenAnObjectIsEdited() throws Exception {
		Concept concept = conceptService.getConcept(3);
		Integer oldConceptClassId = concept.getConceptClass().getConceptClassId();
		Integer oldDatatypeId = concept.getDatatype().getConceptDatatypeId();
		ConceptClass cc = conceptService.getConceptClass(2);
		ConceptDatatype dt = conceptService.getConceptDatatype(3);
		String oldVersion = concept.getVersion();
		String newVersion = "1.11";
		Assert.assertNotSame(cc, concept.getConceptClass());
		Assert.assertNotSame(dt, concept.getDatatype());
		Assert.assertNotSame(newVersion, oldVersion);
		
		concept.setConceptClass(cc);
		concept.setDatatype(dt);
		concept.setVersion(newVersion);
		conceptService.saveConcept(concept);
		
		List<AuditLog> logs = auditLogService.getAuditLogs(null, null, null, null, null, null);
		//Should have created a log entry for edited concept
		Assert.assertEquals(1, logs.size());
		AuditLog auditLog = logs.get(0);
		
		//Should have created entries for the changes properties and their old values
		Assert.assertEquals(Action.UPDATED, auditLog.getAction());
		//Check that there 3 property tag entries
		String changesXml = auditLog.getChangesXml();
		Document doc = AuditLogUtil.createDocument(changesXml);
		Assert.assertFalse(StringUtils.isBlank(changesXml));
		Element changesElement = doc.getDocumentElement();
		Assert.assertEquals(3, getCountOfPropertyTags(changesElement));
		Assert.assertEquals(oldConceptClassId.toString(),
		    AuditLogUtil.getPreviousOrNewPropertyValue(changesElement, "conceptClass", AuditLogUtil.NODE_PREVIOUS));
		Assert.assertEquals(oldDatatypeId.toString(),
		    AuditLogUtil.getPreviousOrNewPropertyValue(changesElement, "datatype", AuditLogUtil.NODE_PREVIOUS));
		Assert.assertEquals(oldVersion,
		    AuditLogUtil.getPreviousOrNewPropertyValue(changesElement, "version", AuditLogUtil.NODE_PREVIOUS));
		
		Assert.assertEquals(cc.getConceptClassId().toString(),
		    AuditLogUtil.getPreviousOrNewPropertyValue(changesElement, "conceptClass", AuditLogUtil.NODE_NEW));
		Assert.assertEquals(dt.getConceptDatatypeId().toString(),
		    AuditLogUtil.getPreviousOrNewPropertyValue(changesElement, "datatype", AuditLogUtil.NODE_NEW));
		Assert.assertEquals(newVersion,
		    AuditLogUtil.getPreviousOrNewPropertyValue(changesElement, "version", AuditLogUtil.NODE_NEW));
	}
	
	@Test
	@NotTransactional
	public void shouldCreateNoLogEntryIfNoChangesAreMadeToAnExistingObject() throws Exception {
		EncounterType encounterType = encounterService.getEncounterType(2);
		encounterService.saveEncounterType(encounterType);
		Assert.assertTrue(auditLogService.getAuditLogs(null, null, null, null, null, null).isEmpty());
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
		Assert.assertTrue(auditLogService.getAuditLogs(null, null, null, null, null, null).isEmpty());
	}
	
	@Test
	public void shouldHandleInsertsOrUpdatesOrDeletesInEachTransactionIndependently() throws InterruptedException {
		final int N = 50;
		final List<Thread> threads = new Vector<Thread>();
		
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
		
		for (int i = 0; i < N; ++i)
			threads.get(i).start();
		
		for (int i = 0; i < N; ++i)
			threads.get(i).join();
		
		Assert.assertEquals(N, auditLogService.getAuditLogs(null, null, null, null, null, null).size());
		
		List<Action> actions = new ArrayList<Action>();
		actions.add(Action.CREATED);//should match expected count of created log entries
		Assert.assertEquals(25, auditLogService.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.clear();
		actions.add(Action.UPDATED);//should match expected count of updated log entries
		Assert.assertEquals(24, auditLogService.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.clear();
		actions.add(Action.DELETED);//should match expected count of deleted log entries
		Assert.assertEquals(1, auditLogService.getAuditLogs(null, actions, null, null, null, null).size());
	}
	
	@Test
	public void shouldNotCreateAuditLogsForUnMonitoredObjects() {
		Location location = new Location();
		location.setName("najja");
		location.setAddress1("test address");
		Location savedLocation = Context.getLocationService().saveLocation(location);
		Assert.assertNotNull(savedLocation.getLocationId());//sanity check that it was actually created
		//Should not have created any logs
		Assert.assertTrue(auditLogService.getAuditLogs(null, null, null, null, null, null).isEmpty());
	}
	
	@Test
	@NotTransactional
	public void shouldIgnoreChangesForStringFieldsFromNullToBlank() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifierType idType = ps.getPatientIdentifierType(1);
		idType.setFormat(null);
		ps.savePatientIdentifierType(idType);
		
		int originalLogCount = auditLogService.getAuditLogs(null, null, null, null, null, null).size();
		idType.setFormat("");
		ps.savePatientIdentifierType(idType);
		Assert.assertEquals(originalLogCount, auditLogService.getAuditLogs(null, null, null, null, null, null).size());
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
		
		int originalLogCount = auditLogService.getAuditLogs(null, null, null, null, null, null).size();
		idType.setFormat(null);
		ps.savePatientIdentifierType(idType);
		Assert.assertEquals(originalLogCount, auditLogService.getAuditLogs(null, null, null, null, null, null).size());
	}
	
	@Test
	@NotTransactional
	public void shouldBeCaseInsensitiveForChangesInStringFields() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifierType idType = ps.getPatientIdentifierType(1);
		idType.setFormat("test");
		idType = ps.savePatientIdentifierType(idType);
		
		int originalLogCount = auditLogService.getAuditLogs(null, null, null, null, null, null).size();
		idType.setFormat("TEST");
		ps.savePatientIdentifierType(idType);
		Assert.assertEquals(originalLogCount, auditLogService.getAuditLogs(null, null, null, null, null, null).size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogEntryWhenAnElementIsRemovedFormAChildCollection() throws Exception {
		Concept concept = conceptService.getConcept(5089);
		Assert.assertTrue(concept.isNumeric());
		//This is a ConceptNumeric, so we need to mark it as monitored
		auditLogService.markAsMonitoredObjects(ConceptNumeric.class, null);
		Assert.assertFalse(concept.getConceptMappings().isEmpty());
		
		concept.removeDescription(concept.getDescription());
		conceptService.saveConcept(concept);
		
		List<AuditLog> updateLogs = auditLogService.getAuditLogs(null, Collections.singletonList(Action.UPDATED), null,
		    null, null, null);
		Assert.assertEquals(1, updateLogs.size());
	}
	
	@Test
	@NotTransactional
	public void shouldCreateAnAuditLogEntryWhenAnElementIsAddedToAChildCollection() throws Exception {
		Concept concept = conceptService.getConcept(5089);
		Assert.assertTrue(concept.isNumeric());
		//This is a ConceptNumeric, so we need to mark it as monitored
		auditLogService.markAsMonitoredObjects(ConceptNumeric.class, null);
		ConceptDescription cd1 = new ConceptDescription("desc1", Locale.ENGLISH);
		cd1.setDateCreated(new Date());
		cd1.setCreator(Context.getAuthenticatedUser());
		concept.addDescription(cd1);
		conceptService.saveConcept(concept);
		
		List<AuditLog> updateLogs = auditLogService.getAuditLogs(null, Collections.singletonList(Action.UPDATED), null,
		    null, null, null);
		Assert.assertEquals(1, updateLogs.size());
	}
	
	/**
	 * Gets the count of property tags in the specified {@link Document}
	 * 
	 * @param doc
	 * @return the count
	 * @throws Exception
	 */
	private int getCountOfPropertyTags(Element rootElement) throws Exception {
		NodeList nodeList = rootElement.getElementsByTagName(AuditLogUtil.NODE_PROPERTY);
		if (nodeList != null)
			return nodeList.getLength();
		return 0;
	}
}
