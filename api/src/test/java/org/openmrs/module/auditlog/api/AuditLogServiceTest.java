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
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.MonitoredObject;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

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
	 * @see {@link AuditLogService#getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)}
	 */
	@Test
	@Verifies(value = "should match on the specified audit log action", method = "getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldMatchOnTheSpecifiedAuditLogAction() throws Exception {
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
	 * @see {@link AuditLogService#getAllMonitoredObjects()}
	 */
	@Test
	@Verifies(value = "should get all the monitored objects in the db", method = "getAllMonitoredObjects()")
	public void getAllMonitoredObjects_shouldGetAllTheMonitoredObjectsInTheDb() throws Exception {
		Assert.assertEquals(4, service.getAllMonitoredObjects().size());
	}
	
	/**
	 * @see {@link AuditLogService#purgeMonitoredObject(MonitoredObject)}
	 */
	@Test
	@Verifies(value = "should delete the monitored object from the db", method = "purgeMonitoredObject(MonitoredObject)")
	public void purgeMonitoredObject_shouldDeleteTheMonitoredObjectFromTheDb() throws Exception {
		int originalCount = service.getAllMonitoredObjects().size();
		MonitoredObject monitoredObject = service.get(MonitoredObject.class, 1);
		Assert.assertNotNull(monitoredObject);
		
		service.purgeMonitoredObject(monitoredObject);
		Assert.assertNull(service.get(MonitoredObject.class, 1));
		Assert.assertEquals(--originalCount, service.getAllMonitoredObjects().size());
	}
	
	/**
	 * @see {@link AuditLogService#getSavedObject(Class<T>,Integer)}
	 */
	@Test
	@Verifies(value = "should get the saved object matching the specified arguments", method = "get(Class<T>,Integer)")
	public void get_shouldGetTheSavedObjectMatchingTheSpecifiedArguments() throws Exception {
		MonitoredObject monitoredObject = service.get(MonitoredObject.class, 1);
		Assert.assertNotNull(monitoredObject);
		Assert.assertEquals("org.openmrs.Concept", monitoredObject.getClassName());
		
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		AuditLog auditLog = service.get(AuditLog.class, 2);
		Assert.assertNotNull(auditLog);
		Assert.assertEquals("4f7d57f0-9077-11e1-aaa4-00248140a5ec", auditLog.getUuid());
	}
	
	/**
	 * @see {@link AuditLogService#markAsMonitoredObjects(Class<C>,List<Class<C>>)}
	 */
	@Test
	@Verifies(value = "should return a list of all saved monitored objects", method = "markAsMonitoredObjects(Class<C>,List<Class<C>>)")
	public void markAsMonitoredObjects_shouldReturnAListOfAllSavedMonitoredObjects() throws Exception {
		int originalCount = service.getAllMonitoredObjects().size();
		MonitoredObject monitoredObject = new MonitoredObject(Order.class.getName());
		monitoredObject.setDateCreated(new Date());
		
		List<Class<? extends Order>> subClasses = new ArrayList<Class<? extends Order>>();
		subClasses.add(DrugOrder.class);
		List<MonitoredObject> savedObjects = service.markAsMonitoredObjects(Order.class, subClasses);
		
		Assert.assertEquals(2, savedObjects.size());
		Assert.assertEquals(originalCount + 2, service.getAllMonitoredObjects().size());
		
		for (MonitoredObject savedObject : savedObjects) {
			Assert.assertNotNull(savedObject.getMonitoredObjectId());
			Assert.assertNotNull(savedObject.getDateCreated());
			Assert.assertNotNull(savedObject.getCreatorDetails());
			Assert.assertEquals(AuditLogUtil.getUserDetails(Context.getAuthenticatedUser()), savedObject.getCreatorDetails());
		}
	}
	
	/**
	 * @see {@link AuditLogService#markAsMonitoredObjects(Class<C>,List<Class<C>>)}
	 */
	@Test
	@Verifies(value = "should save the monitored object to the db", method = "markAsMonitoredObjects(Class<C>,List<Class<C>>)")
	public void markAsMonitoredObjects_shouldSaveTheMonitoredObjectToTheDb() throws Exception {
		int originalCount = service.getAllMonitoredObjects().size();
		List<MonitoredObject> savedObjects = service.markAsMonitoredObjects(Encounter.class, null);
		
		Assert.assertEquals(1, savedObjects.size());
		MonitoredObject monitoredObject = savedObjects.get(0);
		Assert.assertNotNull(monitoredObject.getMonitoredObjectId());
		Assert.assertEquals(++originalCount, service.getAllMonitoredObjects().size());
		
		//Should have set the creator details and date created
		Assert.assertNotNull(monitoredObject.getDateCreated());
		Assert.assertNotNull(monitoredObject.getCreatorDetails());
		Assert.assertEquals(AuditLogUtil.getUserDetails(Context.getAuthenticatedUser()), monitoredObject.getCreatorDetails());
	}
}
