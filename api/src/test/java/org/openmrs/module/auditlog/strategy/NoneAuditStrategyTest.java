/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlog.strategy;

import static junit.framework.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.module.auditlog.BaseAuditLogTest;

@Ignore
public class NoneAuditStrategyTest extends BaseAuditLogTest {
	
	/**
	 * @verifies always return false
	 * @see NoneAuditStrategy#isAudited(Class)
	 */
	@Test
	public void isAudited_shouldAlwaysReturnFalse() throws Exception {
		assertEquals(true, auditLogService.isAudited(EncounterType.class));
		setAuditConfiguration(AuditStrategy.NONE, null, false);
		assertEquals(AuditStrategy.NONE, auditLogService.getAuditingStrategy());
		assertEquals(false, auditLogService.isAudited(EncounterType.class));
	}
}
