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
import java.util.Set;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptNumeric;
import org.openmrs.OpenmrsObject;

public class DAOUtilsTest {
	
	/**
	 * @verifies exclude interfaces and abstract classes
	 * @see DAOUtils#getPersistentConcreteSubclasses(Class)
	 */
	@Test
	public void getPersistentConcreteSubclasses_shouldExcludeInterfacesAndAbstractClasses() throws Exception {
		Set<Class<?>> subclasses = DAOUtils.getPersistentConcreteSubclasses(OpenmrsObject.class);
		for (Class<?> clazz : subclasses) {
			assertFalse("Found interface:" + clazz.getName() + ", interfaces should be excluded",
			    Modifier.isInterface(clazz.getModifiers()));
			assertFalse("Found abstract class:" + clazz.getName() + ", abstract classes should be excluded",
			    Modifier.isAbstract(clazz.getModifiers()));
		}
	}
	
	/**
	 * @verifies return a list of subclasses for the specified type
	 * @see DAOUtils#getPersistentConcreteSubclasses(Class)
	 */
	@Test
	public void getPersistentConcreteSubclasses_shouldReturnAListOfSubclassesForTheSpecifiedType() throws Exception {
		Set<Class<?>> subclasses = DAOUtils.getPersistentConcreteSubclasses(Concept.class);
		assertEquals(2, subclasses.size());
		assertTrue(subclasses.contains(ConceptNumeric.class));
		assertTrue(subclasses.contains(ConceptComplex.class));
	}
}
