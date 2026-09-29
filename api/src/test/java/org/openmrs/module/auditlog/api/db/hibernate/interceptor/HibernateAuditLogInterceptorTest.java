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
package org.openmrs.module.auditlog.api.db.hibernate.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.StaleStateException;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.BaseAuditLogTest;
import org.openmrs.module.auditlog.api.db.DAOUtils;

public class HibernateAuditLogInterceptorTest extends BaseAuditLogTest {
	
	/**
	 * @verifies not fail for a detached object whose previous state has lazy associations
	 * @see HibernateAuditLogInterceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[],
	 *      String[], org.hibernate.type.Type[])
	 */
	@Test
	public void onFlushDirty_shouldNotFailForADetachedObjectWhosePreviousStateHasLazyAssociations() throws Exception {
		assertTrue(auditLogService.isAudited(Concept.class));
		Session session = DAOUtils.getSessionFactory().getCurrentSession();
		Concept concept = Context.getConceptService().getConcept(3);
		Hibernate.initialize(concept.getConceptClass());
		Hibernate.initialize(concept.getDatatype());
		session.evict(concept);
		
		//The previous state is loaded in a separate session where the unchanged concept class and
		//datatype are uninitialized proxies, comparing them with the current values initializes them
		concept.setVersion("new version");
		session.update(concept);
		session.flush();
		
		session.clear();
		assertEquals("new version", Context.getConceptService().getConcept(3).getVersion());
	}
	
	/**
	 * @verifies not fail when an existing person is saved as a patient
	 * @see HibernateAuditLogInterceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[],
	 *      String[], org.hibernate.type.Type[])
	 */
	@Test
	public void onFlushDirty_shouldNotFailWhenAnExistingPersonIsSavedAsAPatient() throws Exception {
		Person person = Context.getPersonService().getPerson(501);
		Context.clearSession();
		DAOUtils.getSessionFactory().getCache().evictAllRegions();
		
		savePersonAsPatient(person);
	}
	
	/**
	 * @verifies not fail when an existing person that is still cached is saved as a patient
	 * @see HibernateAuditLogInterceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[],
	 *      String[], org.hibernate.type.Type[])
	 */
	@Test
	public void onFlushDirty_shouldNotFailWhenAnExistingPersonThatIsStillCachedIsSavedAsAPatient() throws Exception {
		Person person = Context.getPersonService().getPerson(501);
		Context.clearSession();
		
		//Without evicting the second level cache, loading the previous state of the patient in a
		//separate session can find the cached person, which is not a patient
		savePersonAsPatient(person);
	}
	
	/**
	 * @verifies not fail with a NullPointerException for a detached object that has no stored row
	 * @see HibernateAuditLogInterceptor#onFlushDirty(Object, java.io.Serializable, Object[], Object[],
	 *      String[], org.hibernate.type.Type[])
	 */
	@Test
	public void onFlushDirty_shouldNotFailWithANullPointerExceptionForADetachedObjectThatHasNoStoredRow() throws Exception {
		assertTrue(auditLogService.isAudited(EncounterType.class));
		EncounterType type = new EncounterType("some name", "some description");
		type.setEncounterTypeId(9999);
		Session session = DAOUtils.getSessionFactory().getCurrentSession();
		session.update(type);
		try {
			session.flush();
			fail("Hibernate should have failed to update a row that does not exist");
		}
		catch (Exception e) {
			//The update itself should fail and not the interceptor
			Throwable rootCause = ExceptionUtils.getRootCause(e) != null ? ExceptionUtils.getRootCause(e) : e;
			assertTrue("Unexpected failure: " + rootCause, rootCause instanceof StaleStateException);
		}
	}
	
	private void savePersonAsPatient(Person person) throws Exception {
		startAuditing(Patient.class);
		assertTrue(auditLogService.isAudited(Patient.class));
		PatientService ps = Context.getPatientService();
		
		//Core inserts the patient row in the current transaction and then saves the patient as a
		//detached object whose previous state is loaded in a separate session
		Patient patient = new Patient(person);
		PatientIdentifier identifier = new PatientIdentifier("some identifier", new PatientIdentifierType(2),
		        new Location(1));
		identifier.setPreferred(true);
		patient.addIdentifier(identifier);
		ps.savePatient(patient);
		Context.flushSession();
		
		Context.clearSession();
		assertNotNull(ps.getPatient(501));
	}
}
