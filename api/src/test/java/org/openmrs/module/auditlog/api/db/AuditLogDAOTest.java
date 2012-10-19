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

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

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
import org.openmrs.OpenmrsObject;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.MonitoringStrategy;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditLogDAOTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private AuditLogDAO dao;
	
	/**
	 * @see {@link AuditLogDAO#getPersistentConcreteSubclasses(Class<*>)}
	 */
	@Test
	@Verifies(value = "should return a list of subclasses for the specified type", method = "getPersistentConcreteSubclasses(Class<*>)")
	public void getPersistentConcreteSubclasses_shouldReturnAListOfSubclassesForTheSpecifiedType() throws Exception {
		Set<Class<?>> subclasses = dao.getPersistentConcreteSubclasses(Concept.class);
		Assert.assertEquals(2, subclasses.size());
		Assert.assertTrue(subclasses.contains(ConceptNumeric.class));
		Assert.assertTrue(subclasses.contains(ConceptComplex.class));
	}
	
	/**
	 * @see {@link AuditLogDAO#getPersistentConcreteSubclasses(Class<*>)}
	 */
	@Test
	@Verifies(value = "should exclude interfaces and abstract classes", method = "getPersistentConcreteSubclasses(Class<*>)")
	public void getPersistentConcreteSubclasses_shouldExcludeInterfacesAndAbstractClasses() throws Exception {
		Set<Class<?>> subclasses = dao.getPersistentConcreteSubclasses(OpenmrsObject.class);
		for (Class<?> clazz : subclasses) {
			Assert.assertFalse("Found interface:" + clazz.getName() + ", interfaces should be excluded",
			    Modifier.isInterface(clazz.getModifiers()));
			Assert.assertFalse("Found abstract class:" + clazz.getName() + ", abstract classes should be excluded",
			    Modifier.isAbstract(clazz.getModifiers()));
		}
	}
	
	/**
	 * @see {@link AuditLogDAO#getImplicitlyMonitoredClasses()}
	 */
	@Test
	@Verifies(value = "should return a set of implicitly monitored classes", method = "getImplicitlyMonitoredClasses()")
	public void getImplicitlyMonitoredClasses_shouldReturnASetOfImplicitlyMonitoredClasses() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		as.saveGlobalProperty(new GlobalProperty(AuditLogConstants.GP_MONITORING_STRATEGY, MonitoringStrategy.NONE_EXCEPT
		        .name()));
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(Concept.class);
		dao.startMonitoring(classes);
		Set<Class<?>> implicitlyMonitoredClasses = dao.getImplicitlyMonitoredClasses();
		Assert.assertEquals(6, implicitlyMonitoredClasses.size());
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptName.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptDescription.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptMap.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptSet.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptAnswer.class));
		Assert.assertTrue(implicitlyMonitoredClasses.contains(ConceptNameTag.class));
	}
	
}
