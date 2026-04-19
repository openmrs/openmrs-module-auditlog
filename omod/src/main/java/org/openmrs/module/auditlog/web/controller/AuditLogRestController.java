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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.web.dto.AuditLogDTO;
import org.openmrs.module.auditlog.web.dto.AuditLogPageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * AUDIT-36: REST endpoint for querying audit logs with filtering by user, action, and date range.
 *
 * <p>GET /module/auditlog/rest/auditlogs
 *
 * <p>Supported query parameters:
 * <ul>
 *   <li>{@code user}       – UUID of the user who performed the action</li>
 *   <li>{@code action}     – comma-separated actions: CREATED, UPDATED, DELETED</li>
 *   <li>{@code startDate}  – inclusive lower bound (yyyy-MM-dd)</li>
 *   <li>{@code endDate}    – inclusive upper bound (yyyy-MM-dd)</li>
 *   <li>{@code startIndex} – zero-based offset for pagination (default 0)</li>
 *   <li>{@code limit}      – page size, capped at 100 (default 25)</li>
 * </ul>
 */
@Controller
@RequestMapping("/module/auditlog/rest")
public class AuditLogRestController {

	private static final Log log = LogFactory.getLog(AuditLogRestController.class);

	private static final int DEFAULT_LIMIT = 25;

	private static final int MAX_LIMIT = 100;

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	@RequestMapping(value = "/auditlogs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> getAuditLogs(
	        @RequestParam(value = "user", required = false) String userUuid,
	        @RequestParam(value = "action", required = false) String actionParam,
	        @RequestParam(value = "startDate", required = false) String startDateParam,
	        @RequestParam(value = "endDate", required = false) String endDateParam,
	        @RequestParam(value = "startIndex", required = false, defaultValue = "0") int startIndex,
	        @RequestParam(value = "limit", required = false, defaultValue = "25") int limit) {

		Context.requirePrivilege(AuditLogConstants.PRIV_GET_AUDITLOGS);

		if (limit <= 0 || limit > MAX_LIMIT) {
			limit = DEFAULT_LIMIT;
		}
		if (startIndex < 0) {
			startIndex = 0;
		}

		List<Action> actions = parseActions(actionParam);
		Date startDate = parseDate(startDateParam, "startDate");
		Date endDate = parseDate(endDateParam, "endDate");

		if (startDate != null && endDate != null && startDate.after(endDate)) {
			return ResponseEntity.badRequest().body("{\"error\":\"startDate must not be after endDate\"}");
		}

		AuditLogService service = Context.getService(AuditLogService.class);

		List<AuditLog> logs = service.getAuditLogs(userUuid, actions, startDate, endDate, true, startIndex, limit);
		List<AuditLogDTO> results = new ArrayList<AuditLogDTO>(logs.size());
		for (AuditLog log : logs) {
			results.add(AuditLogDTO.from(log));
		}

		AuditLogPageResponse response = new AuditLogPageResponse(results, startIndex, limit);
		return ResponseEntity.ok(response);
	}

	private List<Action> parseActions(String actionParam) {
		if (StringUtils.isBlank(actionParam)) {
			return null;
		}
		List<Action> actions = new ArrayList<Action>();
		for (String raw : actionParam.split(",")) {
			String trimmed = raw.trim().toUpperCase();
			try {
				actions.add(Action.valueOf(trimmed));
			}
			catch (IllegalArgumentException e) {
				log.warn("Ignoring unrecognised action value: " + trimmed);
			}
		}
		return actions.isEmpty() ? null : actions;
	}

	private Date parseDate(String dateParam, String fieldName) {
		if (StringUtils.isBlank(dateParam)) {
			return null;
		}
		try {
			return new SimpleDateFormat(DATE_FORMAT).parse(dateParam);
		}
		catch (ParseException e) {
			log.warn("Invalid " + fieldName + " value '" + dateParam + "', expected " + DATE_FORMAT);
			return null;
		}
	}
}
