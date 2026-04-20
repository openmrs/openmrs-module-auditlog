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

import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.web.dto.AuditLogDetailDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Exposes GET /module/auditlog/rest/auditlogs/{uuid} returning full detail for a single
 * audit log entry, including field-level changes and child log entries.
 */
@Controller
@RequestMapping("/module/auditlog/rest/auditlogs/{uuid}")
public class AuditLogDetailController {

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> getAuditLogDetail(@PathVariable("uuid") String uuid) {
		AuditLog auditLog = Context.getService(AuditLogService.class).getObjectByUuid(AuditLog.class, uuid);
		if (auditLog == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
			        .body("{\"error\": \"No audit log found with uuid: " + uuid + "\"}");
		}
		return ResponseEntity.ok(AuditLogDetailDTO.from(auditLog));
	}
}
