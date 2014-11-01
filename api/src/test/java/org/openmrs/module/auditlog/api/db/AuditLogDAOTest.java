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
package org.openmrs.module.auditlog.api.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.ConceptNumeric;
import org.openmrs.ConceptSet;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.OpenmrsObject;
import org.openmrs.PersonName;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditingStrategy;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditLogDAOTest extends BaseModuleContextSensitiveTest {
	
	private static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	@Autowired
	private AuditLogDAO dao;
	
	/**
	 * @see {@link AuditLogDAO#getPersistentConcreteSubclasses(Class)}
	 */
	@Test
	@Verifies(value = "should return a list of subclasses for the specified type", method = "getPersistentConcreteSubclasses(Class<*>)")
	public void getPersistentConcreteSubclasses_shouldReturnAListOfSubclassesForTheSpecifiedType() throws Exception {
		Set<Class<? extends OpenmrsObject>> subclasses = dao.getPersistentConcreteSubclasses(Concept.class);
		assertEquals(2, subclasses.size());
		assertTrue(subclasses.contains(ConceptNumeric.class));
		assertTrue(subclasses.contains(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogDAO#getPersistentConcreteSubclasses(Class)}
	 */
	@Test
	@Verifies(value = "should exclude interfaces and abstract classes", method = "getPersistentConcreteSubclasses(Class<*>)")
	public void getPersistentConcreteSubclasses_shouldExcludeInterfacesAndAbstractClasses() throws Exception {
		Set<Class<? extends OpenmrsObject>> subclasses = dao.getPersistentConcreteSubclasses(OpenmrsObject.class);
		for (Class<?> clazz : subclasses) {
			assertFalse("Found interface:" + clazz.getName() + ", interfaces should be excluded",
			    Modifier.isInterface(clazz.getModifiers()));
			assertFalse("Found abstract class:" + clazz.getName() + ", abstract classes should be excluded",
			    Modifier.isAbstract(clazz.getModifiers()));
		}
	}
	
	/**
	 * @see {@link AuditLogDAO#getImplicitlyAuditedClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of implicitly audited classes", method = "getImplicitlyAuditedClasses()")
	public void getImplicitlyAuditedClasses_shouldReturnASetOfImplicitlyAuditedClasses() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		as.saveGlobalProperty(new GlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, AuditingStrategy.NONE_EXCEPT.name()));
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(Concept.class);
		dao.startAuditing(classes);
		Set<Class<? extends OpenmrsObject>> implicitlyAuditedClasses = dao.getImplicitlyAuditedClasses();
		assertEquals(5, implicitlyAuditedClasses.size());
		assertTrue(implicitlyAuditedClasses.contains(ConceptName.class));
		assertTrue(implicitlyAuditedClasses.contains(ConceptDescription.class));
		assertTrue(implicitlyAuditedClasses.contains(ConceptMap.class));
		assertTrue(implicitlyAuditedClasses.contains(ConceptSet.class));
		assertTrue(implicitlyAuditedClasses.contains(ConceptAnswer.class));
		//ConceptName.tags is mapped as many-to-many
		assertFalse(implicitlyAuditedClasses.contains(ConceptNameTag.class));
	}
	
	/**
	 * @see {@link AuditLogDAO#isImplicitlyAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return true if a class is implicitly audited", method = "isImplicitlyAudited(Class<*>)")
	public void isImplicitlyAudited_shouldReturnTrueIfAClassIsImplicitlyAudited() throws Exception {
		assertFalse(dao.isAudited(ConceptName.class));//sanity check
		assertTrue(dao.isImplicitlyAudited(ConceptName.class));
	}
	
	/**
	 * @see {@link AuditLogDAO#isImplicitlyAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return false if a class is also marked as audited", method = "isImplicitlyAudited(Class<*>)")
	public void isImplicitlyAudited_shouldReturnFalseIfAClassIsAlsoMarkedAsAudited() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty strategyGP = as.getGlobalPropertyObject(AuditLogConstants.GP_AUDITING_STRATEGY);
		strategyGP.setPropertyValue(AuditingStrategy.NONE_EXCEPT.name());
		as.saveGlobalProperty(strategyGP);
		
		Context.getService(AuditLogService.class).startAuditing(Location.class);
		assertTrue(dao.isAudited(Location.class));//sanity check
		assertFalse(dao.isImplicitlyAudited(Location.class));
	}
	
	/**
	 * @see {@link AuditLogDAO#isImplicitlyAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return false if a class is not implicitly audited", method = "isImplicitlyAudited(Class<*>)")
	public void isImplicitlyAudited_shouldReturnFalseIfAClassIsNotImplicitlyAudited() throws Exception {
		assertFalse(dao.isAudited(PersonName.class));//sanity check
		assertFalse(dao.isImplicitlyAudited(PersonName.class));
	}
	
}
