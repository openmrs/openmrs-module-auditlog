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
package org.openmrs.module.auditlog.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;

public class AuditLogServiceImplTest {

	private AuditLogServiceImpl service;

	private AuditLogDAO dao;

	@Before
	public void setUp() {
		dao = mock(AuditLogDAO.class);
		service = new AuditLogServiceImpl();
		service.setDao(dao);
	}

	@Test
	public void getAuditLogs_byUserUuid_shouldDelegateToDao() {
		String userUuid = "user-uuid-abc";
		List<Action> actions = Arrays.asList(Action.CREATED);
		Date startDate = new Date(0);
		Date endDate = new Date();

		AuditLog mockLog = new AuditLog();
		mockLog.setUuid("log-uuid-1");
		when(dao.getAuditLogs(userUuid, actions, startDate, endDate, false, 0, 10))
		        .thenReturn(Arrays.asList(mockLog));

		List<AuditLog> result = service.getAuditLogs(userUuid, actions, startDate, endDate, false, 0, 10);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("log-uuid-1", result.get(0).getUuid());
		verify(dao).getAuditLogs(userUuid, actions, startDate, endDate, false, 0, 10);
	}

	@Test
	public void getAuditLogs_byUserUuid_shouldReturnEmptyListWhenNoneFound() {
		when(dao.getAuditLogs(anyString(), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		List<AuditLog> result = service.getAuditLogs("some-uuid", null, null, null, true, 0, 25);

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void getAuditLogs_byUserUuid_shouldPassNullUserUuidToDao() {
		when(dao.getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		service.getAuditLogs((String) null, null, null, null, false, 0, 25);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(dao).getAuditLogs(captor.capture(), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt());
		assertEquals(null, captor.getValue());
	}

	@Test
	public void getAuditLogs_byUserUuid_shouldPassPaginationParamsToDao() {
		when(dao.getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		service.getAuditLogs((String) null, null, null, null, false, 50, 10);

		ArgumentCaptor<Integer> startCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(dao).getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), startCaptor.capture(), lengthCaptor.capture());
		assertEquals(Integer.valueOf(50), startCaptor.getValue());
		assertEquals(Integer.valueOf(10), lengthCaptor.getValue());
	}
}
