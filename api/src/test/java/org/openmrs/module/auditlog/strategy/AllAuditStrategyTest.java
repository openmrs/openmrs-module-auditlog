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
import org.openmrs.Location;
import org.openmrs.module.auditlog.BaseAuditLogTest;

@Ignore
public class AllAuditStrategyTest extends BaseAuditLogTest {
	
	/**
	 * @verifies always return true
	 * @see AllAuditStrategy#isAudited(Class)
	 */
	@Test
	public void isAudited_shouldAlwaysReturnTrue() throws Exception {
		assertEquals(false, auditLogService.isAudited(Location.class));
		setAuditConfiguration(AuditStrategy.ALL, null, false);
		assertEquals(AuditStrategy.ALL, auditLogService.getAuditingStrategy());
		assertEquals(true, auditLogService.isAudited(Location.class));
	}
}
