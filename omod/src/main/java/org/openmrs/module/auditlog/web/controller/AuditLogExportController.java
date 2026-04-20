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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * AUDIT-43: Streams audit log entries as a CSV file download.
 *
 * <p>GET /module/auditlog/rest/auditlogs/export
 *
 * <p>Supported query parameters:
 * <ul>
 *   <li>{@code action}    – comma-separated actions: CREATED, UPDATED, DELETED</li>
 *   <li>{@code startDate} – inclusive lower bound (yyyy-MM-dd)</li>
 *   <li>{@code endDate}   – inclusive upper bound (yyyy-MM-dd)</li>
 * </ul>
 *
 * <p>Response: {@code text/csv} with Content-Disposition triggering a download
 * named {@code audit_log.csv}.
 */
@Controller
@RequestMapping("/module/auditlog/rest")
public class AuditLogExportController {

	private static final Log log = LogFactory.getLog(AuditLogExportController.class);

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static final String CSV_HEADER = "auditLogId,type,identifier,action,user,dateCreated\n";

	@RequestMapping(value = "/auditlogs/export", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<byte[]> exportAuditLogs(
	        @RequestParam(value = "action", required = false) String actionParam,
	        @RequestParam(value = "startDate", required = false) String startDateParam,
	        @RequestParam(value = "endDate", required = false) String endDateParam) {

		Context.requirePrivilege(AuditLogConstants.PRIV_GET_AUDITLOGS);

		List<Action> actions = parseActions(actionParam);
		Date startDate = parseDate(startDateParam, "startDate");
		Date endDate = parseDate(endDateParam, "endDate");

		if (startDate != null && endDate != null && startDate.after(endDate)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			        .body("startDate must not be after endDate".getBytes());
		}

		List<AuditLog> logs = Context.getService(AuditLogService.class)
		        .getAuditLogs(null, actions, startDate, endDate, true, null, null);

		StringBuilder csv = new StringBuilder(CSV_HEADER);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (AuditLog auditLog : logs) {
			csv.append(buildCsvRow(auditLog, sdf));
		}

		byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_log.csv\"");
		headers.setContentLength(csvBytes.length);

		return new ResponseEntity<byte[]>(csvBytes, headers, HttpStatus.OK);
	}

	private String buildCsvRow(AuditLog auditLog, SimpleDateFormat sdf) {
		StringBuilder sb = new StringBuilder();
		sb.append(auditLog.getAuditLogId()).append(',');
		sb.append(escapeCsv(auditLog.getType())).append(',');
		sb.append(escapeCsv(auditLog.getIdentifier() != null ? auditLog.getIdentifier().toString() : "")).append(',');
		sb.append(auditLog.getAction()).append(',');

		String username = "";
		if (auditLog.getUser() != null) {
			username = StringUtils.defaultString(auditLog.getUser().getUsername());
		}
		sb.append(escapeCsv(username)).append(',');
		sb.append(auditLog.getDateCreated() != null ? sdf.format(auditLog.getDateCreated()) : "").append('\n');
		return sb.toString();
	}

	private String escapeCsv(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
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
