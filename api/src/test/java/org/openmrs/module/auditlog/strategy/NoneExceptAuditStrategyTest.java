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
package org.openmrs.module.auditlog.strategy;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.Set;

import org.hibernate.persister.collection.CollectionPersister;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptNumeric;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Order;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.module.auditlog.BaseAuditLogTest;
import org.openmrs.module.auditlog.util.AuditLogUtil;

public class NoneExceptAuditStrategyTest extends BaseAuditLogTest {
	
	/**
	 * @verifies update the exception class names global property
	 * @see NoneExceptAuditStrategy#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldUpdateTheExceptionClassNamesGlobalProperty() throws Exception {
		Set<Class<?>> exceptions = helper.getExceptions();
		int originalCount = exceptions.size();
		assertFalse(auditLogService.isAudited(ConceptDescription.class));
		assertTrue(auditLogService.isAudited(Concept.class));
		assertTrue(auditLogService.isAudited(ConceptNumeric.class));
		assertTrue(auditLogService.isAudited(ConceptComplex.class));
		assertTrue(auditLogService.isAudited(EncounterType.class));
		assertTrue(auditLogService.isAudited(PatientIdentifierType.class));
		startAuditing(ConceptDescription.class);
		exceptions = helper.getExceptions();
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
	 * @verifies mark a class and its known subclasses as audited
	 * @see NoneExceptAuditStrategy#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldMarkAClassAndItsKnownSubclassesAsAudited() throws Exception {
		Set<Class<?>> exceptions = helper.getExceptions();
		assertFalse(exceptions.contains(Order.class));
		assertFalse(exceptions.contains(DrugOrder.class));
		assertEquals(false, auditLogService.isAudited(Order.class));
		assertEquals(false, auditLogService.isAudited(DrugOrder.class));
		startAuditing(Order.class);
		exceptions = helper.getExceptions();
		assertTrue(exceptions.contains(Order.class));
		assertTrue(exceptions.contains(DrugOrder.class));
		assertEquals(true, auditLogService.isAudited(Order.class));
		assertEquals(true, auditLogService.isAudited(DrugOrder.class));
	}
	
	/**
	 * @verifies also mark association types as audited
	 * @see NoneExceptAuditStrategy#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldAlsoMarkAssociationTypesAsAudited() throws Exception {
		assertFalse(auditLogService.isAudited(Person.class));
		assertFalse(auditLogService.isAudited(PersonName.class));
		assertFalse(helper.isImplicitlyAudited(PersonName.class));
		startAuditing(Person.class);
		assertTrue(auditLogService.isAudited(Person.class));
		assertFalse(auditLogService.isAudited(PersonName.class));
		assertTrue(helper.isImplicitlyAudited(PersonName.class));
	}
	
	/**
	 * @verifies not mark association types for many to many collections as audited
	 * @see NoneExceptAuditStrategy#startAuditing(java.util.Set>)
	 */
	@Test
	public void startAuditing_shouldNotMarkAssociationTypesForManyToManyCollectionsAsAudited() throws Exception {
		assertFalse(auditLogService.isAudited(Location.class));
		assertFalse(auditLogService.isAudited(LocationTag.class));
		assertFalse(helper.isImplicitlyAudited(LocationTag.class));
		CollectionPersister cp = AuditLogUtil.getCollectionPersister("tags", Location.class, null);
		assertTrue(cp.isManyToMany());
		startAuditing(Location.class);
		assertTrue(auditLogService.isAudited(Location.class));
		assertFalse(auditLogService.isAudited(LocationTag.class));
		assertFalse(helper.isImplicitlyAudited(LocationTag.class));
	}
}
