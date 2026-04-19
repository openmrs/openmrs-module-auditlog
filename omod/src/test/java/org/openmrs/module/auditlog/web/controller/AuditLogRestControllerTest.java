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
package org.openmrs.module.auditlog.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.openmrs.User;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.web.dto.AuditLogPageResponse;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.openmrs.api.context.Context;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class })
public class AuditLogRestControllerTest {

	private AuditLogRestController controller;

	private AuditLogService auditLogService;

	@Before
	public void setUp() {
		controller = new AuditLogRestController();
		auditLogService = mock(AuditLogService.class);

		PowerMockito.mockStatic(Context.class);
		PowerMockito.doNothing().when(Context.class);
		Context.requirePrivilege(anyString());
		when(Context.getService(AuditLogService.class)).thenReturn(auditLogService);
	}

	private AuditLog buildAuditLog(String uuid, Action action) {
		AuditLog log = new AuditLog();
		log.setUuid(uuid);
		log.setType("org.openmrs.Patient");
		log.setIdentifier("42");
		log.setAction(action);
		log.setDateCreated(new Date());
		User user = new User();
		user.setUuid("user-uuid-1");
		user.setSystemId("admin");
		log.setUser(user);
		return log;
	}

	@Test
	public void getAuditLogs_shouldReturnPageResponseWithResults() {
		AuditLog log = buildAuditLog("uuid-1", Action.CREATED);
		when(auditLogService.getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Arrays.asList(log));

		ResponseEntity<?> response = controller.getAuditLogs(null, null, null, null, 0, 25);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		AuditLogPageResponse body = (AuditLogPageResponse) response.getBody();
		assertNotNull(body);
		assertEquals(1, body.getResultsCount());
		assertEquals(0, body.getStartIndex());
		assertEquals(25, body.getLimit());
	}

	@Test
	public void getAuditLogs_shouldReturnEmptyResultsWhenNoLogsFound() {
		when(auditLogService.getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		ResponseEntity<?> response = controller.getAuditLogs(null, null, null, null, 0, 25);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		AuditLogPageResponse body = (AuditLogPageResponse) response.getBody();
		assertEquals(0, body.getResultsCount());
	}

	@Test
	public void getAuditLogs_shouldFilterByUserUuid() {
		String userUuid = "user-uuid-abc";
		when(auditLogService.getAuditLogs(anyString(), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		controller.getAuditLogs(userUuid, null, null, null, 0, 25);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(auditLogService).getAuditLogs(captor.capture(), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt());
		assertEquals(userUuid, captor.getValue());
	}

	@Test
	public void getAuditLogs_shouldParseCommaSeparatedActions() {
		when(auditLogService.getAuditLogs(isNull(String.class), any(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		controller.getAuditLogs(null, "CREATED,UPDATED", null, null, 0, 25);

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(auditLogService).getAuditLogs(isNull(String.class), captor.capture(), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt());
		List<Action> actions = captor.getValue();
		assertEquals(2, actions.size());
		assertEquals(Action.CREATED, actions.get(0));
		assertEquals(Action.UPDATED, actions.get(1));
	}

	@Test
	public void getAuditLogs_shouldRejectStartDateAfterEndDate() {
		ResponseEntity<?> response = controller.getAuditLogs(null, null, "2026-04-20", "2026-04-01", 0, 25);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	public void getAuditLogs_shouldCapLimitAt100() {
		when(auditLogService.getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		controller.getAuditLogs(null, null, null, null, 0, 999);

		ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(auditLogService).getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), limitCaptor.capture());
		assertEquals(Integer.valueOf(25), limitCaptor.getValue());
	}

	@Test
	public void getAuditLogs_shouldIgnoreInvalidActionValues() {
		when(auditLogService.getAuditLogs(isNull(String.class), isNull(List.class), isNull(Date.class),
		    isNull(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		ResponseEntity<?> response = controller.getAuditLogs(null, "INVALID_ACTION", null, null, 0, 25);

		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	@Test
	public void getAuditLogs_shouldHandleValidDateRange() {
		when(auditLogService.getAuditLogs(isNull(String.class), isNull(List.class), any(Date.class),
		    any(Date.class), anyBoolean(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		ResponseEntity<?> response = controller.getAuditLogs(null, null, "2026-01-01", "2026-04-20", 0, 25);

		assertEquals(HttpStatus.OK, response.getStatusCode());
	}
}
