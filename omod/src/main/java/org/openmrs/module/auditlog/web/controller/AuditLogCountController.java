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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * AUDIT-40/AUDIT-44: REST endpoint returning the total count of audit log entries matching the
 * given filters, without pagination overhead.
 *
 * <p>GET /module/auditlog/rest/auditlogs/count
 *
 * <p>Supported query parameters:
 * <ul>
 *   <li>{@code type}      – fully-qualified class name, e.g. org.openmrs.Concept</li>
 *   <li>{@code action}    – comma-separated actions: CREATED, UPDATED, DELETED</li>
 *   <li>{@code startDate} – inclusive lower bound (yyyy-MM-dd)</li>
 *   <li>{@code endDate}   – inclusive upper bound (yyyy-MM-dd)</li>
 * </ul>
 *
 * <p>Example response: {@code {"count": 142}}
 */
@Controller
@RequestMapping("/module/auditlog/rest")
public class AuditLogCountController {

	private static final Log log = LogFactory.getLog(AuditLogCountController.class);

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	@RequestMapping(value = "/auditlogs/count", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> countAuditLogs(
	        @RequestParam(value = "type", required = false) String typeParam,
	        @RequestParam(value = "action", required = false) String actionParam,
	        @RequestParam(value = "startDate", required = false) String startDateParam,
	        @RequestParam(value = "endDate", required = false) String endDateParam) {

		Context.requirePrivilege(AuditLogConstants.PRIV_GET_AUDITLOGS);

		List<Action> actions = parseActions(actionParam);
		Date startDate = parseDate(startDateParam, "startDate");
		Date endDate = parseDate(endDateParam, "endDate");

		if (startDate != null && endDate != null && startDate.after(endDate)) {
			return ResponseEntity.badRequest().body("{\"error\":\"startDate must not be after endDate\"}");
		}

		List<Class<?>> types = null;
		if (StringUtils.isNotBlank(typeParam)) {
			try {
				types = Collections.<Class<?>>singletonList(Context.loadClass(typeParam));
			}
			catch (ClassNotFoundException e) {
				return ResponseEntity.badRequest().body("{\"error\":\"Unknown type: " + typeParam + "\"}");
			}
		}

		long count = Context.getService(AuditLogService.class)
		        .countAuditLogs(types, actions, startDate, endDate, true);

		return ResponseEntity.ok("{\"count\":" + count + "}");
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
