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
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.test.Verifies;

public class AuditLogUtilTest {
	
	/**
	 * @see {@link AuditLogUtil#getCollectionElementType(Class<*>,String)}
	 */
	@Test
	@Verifies(value = "should return the class of the property", method = "getCollectionElementType(Class<*>,String)")
	public void getCollectionElementType_shouldReturnTheClassOfTheProperty() throws Exception {
		assertEquals(AuditLogUtil.getCollectionElementType(Concept.class, "names"), ConceptName.class);
		//should pass if the property is defined in a super class
		assertEquals(ConceptName.class, AuditLogUtil.getCollectionElementType(ConceptNumeric.class, "names"));
		assertNull(AuditLogUtil.getCollectionElementType(ConceptNumeric.class, "random"));
	}
}
