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
package org.openmrs.module.auditlog.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.Location;
import org.openmrs.module.auditlog.api.db.DAOUtils;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class AuditLogUtilTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see {@link AuditLogUtil#getCollectionElementType(Class, String)}
	 */
	@Test
	@Verifies(value = "should return the class of the property", method = "getCollectionElementType(Class<*>,String)")
	public void getCollectionElementType_shouldReturnTheClassOfTheProperty() throws Exception {
		assertEquals(AuditLogUtil.getCollectionElementType(Concept.class, "names"), ConceptName.class);
		//should pass if the property is defined in a super class
		assertEquals(ConceptName.class, AuditLogUtil.getCollectionElementType(ConceptNumeric.class, "names"));
		assertNull(AuditLogUtil.getCollectionElementType(ConceptNumeric.class, "random"));
	}
	
	/**
	 * @verifies return the collection persister
	 * @see AuditLogUtil#getCollectionPersister(String, Class,
	 *      org.hibernate.engine.SessionFactoryImplementor)
	 */
	@Test
	public void getCollectionPersister_shouldReturnTheCollectionPersister() throws Exception {
		assertNotNull(AuditLogUtil.getCollectionPersister("names", Concept.class, null));
	}
	
	/**
	 * @verifies return the collection persister if the property is declared in a super class
	 * @see AuditLogUtil#getCollectionPersister(String, Class,
	 *      org.hibernate.engine.SessionFactoryImplementor)
	 */
	@Test
	public void getCollectionPersister_shouldReturnTheCollectionPersisterIfThePropertyIsDeclaredInASuperClass()
	    throws Exception {
		assertEquals(Concept.class.getName() + ".names",
		    AuditLogUtil.getCollectionPersister("names", ConceptNumeric.class, null).getRole());
	}
	
	/**
	 * @verifies serialize an uninitialized proxy by its identifier without initializing it
	 * @see AuditLogUtil#serializeObject(Object)
	 */
	@Test
	public void serializeObject_shouldSerializeAnUninitializedProxyByItsIdentifierWithoutInitializingIt() throws Exception {
		Object location = getProxy(Location.class, 1);
		assertFalse(Hibernate.isInitialized(location));
		
		assertEquals("1", AuditLogUtil.serializeObject(location));
		assertFalse(Hibernate.isInitialized(location));
	}
	
	/**
	 * @verifies serialize an initialized proxy by its identifier
	 * @see AuditLogUtil#serializeObject(Object)
	 */
	@Test
	public void serializeObject_shouldSerializeAnInitializedProxyByItsIdentifier() throws Exception {
		Object location = getProxy(Location.class, 1);
		Hibernate.initialize(location);
		assertTrue(Hibernate.isInitialized(location));
		
		assertEquals("1", AuditLogUtil.serializeObject(location));
	}
	
	private Object getProxy(Class<?> clazz, Integer id) {
		Session session = DAOUtils.getSessionFactory().getCurrentSession();
		//Ensures that load returns a proxy and not an instance that is already in the session
		session.clear();
		Object proxy = session.load(clazz, id);
		assertTrue(proxy instanceof HibernateProxy);
		return proxy;
	}
}
