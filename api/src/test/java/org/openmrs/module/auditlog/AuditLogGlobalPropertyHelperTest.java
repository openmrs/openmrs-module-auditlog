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
import static org.junit.Assert.assertTrue;

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
import org.openmrs.EncounterType;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.strategy.AuditStrategy;
import org.openmrs.module.auditlog.strategy.ExceptionBasedAuditStrategy;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditLogGlobalPropertyHelperTest extends BaseAuditLogTest {
	
	@Autowired
	private AuditLogHelper helper;
	
	/**
	 * @verifies return a set of exception classes
	 * @see AuditLogHelper#getExceptions()
	 */
	@Test
	public void getExceptions_shouldReturnASetOfExceptionClasses() throws Exception {
		Set<Class<?>> exceptions = auditLogService.getExceptions();
		junit.framework.Assert.assertEquals(5, exceptions.size());
		junit.framework.Assert.assertTrue(OpenmrsUtil.collectionContains(exceptions, Concept.class));
		junit.framework.Assert.assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptNumeric.class));
		junit.framework.Assert.assertTrue(OpenmrsUtil.collectionContains(exceptions, ConceptComplex.class));
		junit.framework.Assert.assertTrue(OpenmrsUtil.collectionContains(exceptions, EncounterType.class));
		junit.framework.Assert.assertTrue(OpenmrsUtil.collectionContains(exceptions, PatientIdentifierType.class));
	}
	
	/**
	 * @see {@link AuditLogHelper#getImplicitlyAuditedClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of implicitly audited classes for none except strategy", method = "getImplicitlyAuditedClasses()")
	public void getImplicitlyAuditedClasses_shouldReturnASetOfImplicitlyAuditedClassesForNoneExceptStrategy()
	    throws Exception {
		Set<Class<?>> implicitlyAuditedClasses = helper.getImplicitlyAuditedClasses();
		AuditLogUtil.setGlobalProperty(ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION, ConceptName.class.getName()
		        + "," + ConceptDescription.class.getName());
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
	 * @see {@link AuditLogHelper#getImplicitlyAuditedClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of implicitly audited classes for all except strategy", method = "getImplicitlyAuditedClasses()")
	public void getImplicitlyAuditedClasses_shouldReturnASetOfImplicitlyAuditedClassesForAllExceptStrategy()
	    throws Exception {
		AuditStrategy newStrategy = AuditStrategy.ALL_EXCEPT;
		String exceptions = ConceptName.class.getName() + "," + ConceptDescription.class.getName() + ","
		        + ConceptAnswer.class.getName();
		setAuditConfiguration(newStrategy, exceptions, false);
		Set<Class<?>> implicitlyAuditedClasses = helper.getImplicitlyAuditedClasses();
		assertEquals(3, implicitlyAuditedClasses.size());
		//ConceptName and Description should still be audited implicitly
		assertTrue(implicitlyAuditedClasses.contains(ConceptName.class));
		assertTrue(implicitlyAuditedClasses.contains(ConceptDescription.class));
		assertTrue(implicitlyAuditedClasses.contains(ConceptAnswer.class));
		assertFalse(implicitlyAuditedClasses.contains(ConceptMap.class));
		assertFalse(implicitlyAuditedClasses.contains(ConceptSet.class));
		//ConceptName.tags is mapped as many-to-many
		assertFalse(implicitlyAuditedClasses.contains(ConceptNameTag.class));
	}
	
	/**
	 * @verifies return an empty set for all strategy
	 * @see AuditLogHelper#getImplicitlyAuditedClasses()
	 */
	@Test
	public void getImplicitlyAuditedClasses_shouldReturnAnEmptySetForAllStrategy() throws Exception {
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, AuditStrategy.SHORT_NAME_ALL);
		assertEquals(0, helper.getImplicitlyAuditedClasses().size());
	}
	
	/**
	 * @verifies return an empty set for none strategy
	 * @see AuditLogHelper#getImplicitlyAuditedClasses()
	 */
	@Test
	public void getImplicitlyAuditedClasses_shouldReturnAnEmptySetForNoneStrategy() throws Exception {
		AuditLogUtil.setGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY, AuditStrategy.SHORT_NAME_NONE);
		assertEquals(0, helper.getImplicitlyAuditedClasses().size());
	}
	
	/**
	 * @see {@link AuditLogHelper#isImplicitlyAudited(Class)}
	 */
	@Test
	@Verifies(value = "should return true if a class is implicitly audited", method = "isImplicitlyAudited(Class<*>)")
	public void isImplicitlyAudited_shouldReturnTrueIfAClassIsImplicitlyAudited() throws Exception {
		assertFalse(helper.isAudited(ConceptName.class));//sanity check
		assertTrue(helper.isImplicitlyAudited(ConceptName.class));
	}
	
	/**
	 * @verifies return false if a class is already explicitly marked already as audited
	 * @see AuditLogHelper#isImplicitlyAudited(Class)
	 */
	@Test
	public void isImplicitlyAudited_shouldReturnFalseIfAClassIsAlreadyExplicitlyMarkedAlreadyAsAudited() throws Exception {
		assertTrue(helper.isAudited(Concept.class));//sanity checks
		assertTrue(helper.isImplicitlyAudited(ConceptName.class));
		Context.getService(AuditLogService.class).startAuditing(ConceptName.class);
		assertTrue(helper.isAudited(Concept.class));
		assertFalse(helper.isImplicitlyAudited(ConceptName.class));
	}
	
	/**
	 * @verifies return false if a class is not implicitly marked as audited
	 * @see AuditLogHelper#isImplicitlyAudited(Class)
	 */
	@Test
	public void isImplicitlyAudited_shouldReturnFalseIfAClassIsNotImplicitlyMarkedAsAudited() throws Exception {
		assertFalse(helper.isAudited(Person.class));
		assertFalse(helper.isAudited(PersonName.class));//sanity check
		assertFalse(helper.isImplicitlyAudited(PersonName.class));
	}
	
	/**
	 * @verifies return true if a class is implicitly audited and strategy is all except
	 * @see AuditLogHelper#isImplicitlyAudited(Class)
	 */
	@Test
	public void isImplicitlyAudited_shouldReturnTrueIfAClassIsImplicitlyAuditedAndStrategyIsAllExcept() throws Exception {
		setAuditConfiguration(AuditStrategy.ALL_EXCEPT, "org.openmrs.ConceptName", false);
		assertTrue(helper.isAudited(Concept.class));
		assertFalse(helper.isAudited(ConceptName.class));//sanity check
		assertTrue(helper.isImplicitlyAudited(ConceptName.class));
	}
	
	/**
	 * \
	 * 
	 * @verifies return false if a class is already explicitly audited and strategy is all except
	 * @see AuditLogHelper#isImplicitlyAudited(Class)
	 */
	@Test
	public void isImplicitlyAudited_shouldReturnFalseIfAClassIsAlreadyExplicitlyAuditedAndStrategyIsAllExcept()
	    throws Exception {
		setAuditConfiguration(AuditStrategy.ALL_EXCEPT, null, false);
		assertTrue(helper.isAudited(ConceptName.class));
		assertFalse(helper.isImplicitlyAudited(ConceptName.class));
	}
	
	/**
	 * @verifies return false if a class is not implicitly audited and strategy is all except
	 * @see AuditLogHelper#isImplicitlyAudited(Class)
	 */
	@Test
	public void isImplicitlyAudited_shouldReturnFalseIfAClassIsNotImplicitlyAuditedAndStrategyIsAllExcept() throws Exception {
		setAuditConfiguration(AuditStrategy.ALL_EXCEPT, "org.openmrs.Concept,org.openmrs.ConceptName", false);
		assertFalse(helper.isAudited(Concept.class));
		assertFalse(helper.isAudited(ConceptName.class));
		assertFalse(helper.isImplicitlyAudited(ConceptName.class));
	}
}
