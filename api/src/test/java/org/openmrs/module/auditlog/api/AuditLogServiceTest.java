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
package org.openmrs.module.auditlog.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsUtil;

/**
 * Contains tests for methods in {@link AuditLogService}
 */
public class AuditLogServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	private static final String MODULE_TEST_DATA_AUDIT_LOGS = "moduleTestData-initialAuditLogs.xml";
	
	private AuditLogService service;
	
	@Before
	public void before() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		service = Context.getService(AuditLogService.class);
	}
	
	/**
	 * @see {@link AuditLogService#get(Class<T>,Integer)}
	 */
	@Test
	@Verifies(value = "should get the saved object matching the specified arguments", method = "get(Class<T>,Integer)")
	public void get_shouldGetTheSavedObjectMatchingTheSpecifiedArguments() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Assert.assertEquals("4f7d57f0-9077-11e1-aaa4-00248140a5eb", service.get(AuditLog.class, 1).getUuid());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)}
	 */
	@Test
	@Verifies(value = "should match on the specified audit log actions", method = "getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldMatchOnTheSpecifiedAuditLogActions() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Action> actions = new ArrayList<Action>();
		actions.add(Action.CREATED);//get only inserts
		Assert.assertEquals(1, service.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.add(Action.UPDATED);//get both insert and update logs
		Assert.assertEquals(3, service.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.clear();
		actions.add(Action.UPDATED);//get only updates
		Assert.assertEquals(2, service.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.clear();
		actions.add(Action.DELETED);//get only deletes
		Assert.assertEquals(1, service.getAuditLogs(null, actions, null, null, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)}
	 */
	@Test
	@Verifies(value = "should return all audit logs in the database if all args are null", method = "getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnAllAuditLogsInTheDatabaseIfAllArgsAreNull() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Assert.assertEquals(4, service.getAuditLogs(null, null, null, null, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(List<QClass<OpenmrsObject>>,List<Action>,Date,Date,
	 *      Integer,Integer)}
	 */
	@Test
	@Verifies(value = "should match on the specified classes", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldMatchOnTheSpecifiedClasses() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Class<? extends OpenmrsObject>> clazzes = new ArrayList<Class<? extends OpenmrsObject>>();
		clazzes.add(Concept.class);
		Assert.assertEquals(3, service.getAuditLogs(clazzes, null, null, null, null, null).size());
		clazzes.add(ConceptName.class);
		Assert.assertEquals(4, service.getAuditLogs(clazzes, null, null, null, null, null).size());
	}
	
	/**
	 * @see {@link
	 *      AuditLogService#getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,
	 *      Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created on or after the specified startDate", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedOnOrAfterTheSpecifiedStartDate() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 3, 1, 0, 1, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date startDate = cal.getTime();
		Assert.assertEquals(3, service.getAuditLogs(null, null, startDate, null, null, null).size());
	}
	
	/**
	 * @see {@link
	 *      AuditLogService#getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,
	 *      Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created on or before the specified endDate", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedOnOrBeforeTheSpecifiedEndDate() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 3, 1, 0, 3, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date endDate = cal.getTime();
		Assert.assertEquals(3, service.getAuditLogs(null, null, null, endDate, null, null).size());
	}
	
	/**
	 * @see {@link
	 *      AuditLogService#getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,
	 *      Integer)}
	 */
	@Test
	@Verifies(value = "should return logs created within the specified start and end dates", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnLogsCreatedWithinTheSpecifiedStartAndEndDates() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(2012, 3, 1, 0, 0, 1);
		Date startDate = cal.getTime();
		cal.set(2012, 3, 1, 0, 3, 1);
		Date endDate = cal.getTime();
		Assert.assertEquals(2, service.getAuditLogs(null, null, startDate, endDate, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,
	 *      Integer,Integer)}
	 */
	@Test(expected = APIException.class)
	@Verifies(value = "should reject a start date that is in the future", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldRejectAStartDateThatIsInTheFuture() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 1);
		Date startDate = cal.getTime();
		service.getAuditLogs(null, null, startDate, null, null, null);
	}
	
	/**
	 * @see {@link
	 *      AuditLogService#getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,
	 *      Integer)}
	 */
	@Test
	@Verifies(value = "should ignore end date it it is in the future", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldIgnoreEndDateItItIsInTheFuture() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 1);
		Date endDate = cal.getTime();
		Assert.assertEquals(4, service.getAuditLogs(null, null, null, endDate, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,
	 *      Integer,Integer)}
	 */
	@Test
	@Verifies(value = "should sort the logs by date of creation starting with the latest", method = "getAuditLogs(List<Class<OpenmrsObject>>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldSortTheLogsByDateOfCreationStartingWithTheLatest() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<AuditLog> auditLogs = service.getAuditLogs(null, null, null, null, null, null);
		Assert.assertFalse(auditLogs.isEmpty());
		Date currMaxDate = auditLogs.get(0).getDateCreated();
		for (AuditLog auditLog : auditLogs) {
			Assert.assertTrue(OpenmrsUtil.compare(currMaxDate, auditLog.getDateCreated()) >= 0);
		}
	}
}
