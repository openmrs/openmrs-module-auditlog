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
package org.openmrs.module.auditlog.web.dto;

import java.util.Date;

import org.openmrs.module.auditlog.AuditLog;

/**
 * Lightweight JSON-serialisable representation of a single {@link AuditLog} entry.
 */
public class AuditLogDTO {

	private String uuid;

	private String type;

	private String simpleType;

	private String identifier;

	private String action;

	private String userUuid;

	private String userDisplay;

	private Date dateCreated;

	private boolean hasChildLogs;

	public static AuditLogDTO from(AuditLog log) {
		AuditLogDTO dto = new AuditLogDTO();
		dto.uuid = log.getUuid();
		dto.type = log.getType();
		dto.simpleType = log.getSimpleTypeName();
		dto.identifier = log.getIdentifier();
		dto.action = log.getAction() != null ? log.getAction().name() : null;
		dto.dateCreated = log.getDateCreated();
		dto.hasChildLogs = log.hasChildLogs();
		if (log.getUser() != null) {
			dto.userUuid = log.getUser().getUuid();
			dto.userDisplay = log.getUser().getUsername() != null
			        ? log.getUser().getUsername()
			        : log.getUser().getSystemId();
		}
		return dto;
	}

	public String getUuid() {
		return uuid;
	}

	public String getType() {
		return type;
	}

	public String getSimpleType() {
		return simpleType;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getAction() {
		return action;
	}

	public String getUserUuid() {
		return userUuid;
	}

	public String getUserDisplay() {
		return userDisplay;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public boolean isHasChildLogs() {
		return hasChildLogs;
	}
}
