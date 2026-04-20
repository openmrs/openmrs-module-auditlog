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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.util.AuditLogUtil;

/**
 * Full detail representation of a single {@link AuditLog} entry, including field-level
 * changes parsed from serialized_data and any child log entries.
 */
public class AuditLogDetailDTO {

	private static final Log log = LogFactory.getLog(AuditLogDetailDTO.class);

	private String uuid;

	private String type;

	private String simpleType;

	private String identifier;

	private String action;

	private String userUuid;

	private String userDisplay;

	private Date dateCreated;

	private boolean hasChildLogs;

	private List<FieldChange> changes;

	private List<AuditLogDTO> childLogs;

	public static AuditLogDetailDTO from(AuditLog auditLog) {
		AuditLogDetailDTO dto = new AuditLogDetailDTO();
		dto.uuid = auditLog.getUuid();
		dto.type = auditLog.getType();
		dto.simpleType = auditLog.getSimpleTypeName();
		dto.identifier = auditLog.getIdentifier();
		dto.action = auditLog.getAction() != null ? auditLog.getAction().name() : null;
		dto.dateCreated = auditLog.getDateCreated();
		dto.hasChildLogs = auditLog.hasChildLogs();
		if (auditLog.getUser() != null) {
			dto.userUuid = auditLog.getUser().getUuid();
			dto.userDisplay = auditLog.getUser().getUsername() != null
			        ? auditLog.getUser().getUsername()
			        : auditLog.getUser().getSystemId();
		}
		dto.changes = buildChanges(auditLog);
		dto.childLogs = buildChildLogs(auditLog);
		return dto;
	}

	private static List<FieldChange> buildChanges(AuditLog auditLog) {
		if (auditLog.getAction() == null) {
			return Collections.emptyList();
		}
		List<FieldChange> result = new ArrayList<FieldChange>();
		try {
			if (auditLog.getAction() == AuditLog.Action.UPDATED) {
				Map<String, List> rawChanges = AuditLogUtil.getChangesOfUpdatedItem(auditLog);
				for (Map.Entry<String, List> entry : rawChanges.entrySet()) {
					List values = entry.getValue();
					String newVal = values.size() > 0 && values.get(0) != null ? values.get(0).toString() : null;
					String oldVal = values.size() > 1 && values.get(1) != null ? values.get(1).toString() : null;
					result.add(new FieldChange(entry.getKey(), oldVal, newVal));
				}
			} else if (auditLog.getAction() == AuditLog.Action.DELETED) {
				Map<String, String> lastState = AuditLogUtil.getLastStateOfDeletedItem(auditLog);
				for (Map.Entry<String, String> entry : lastState.entrySet()) {
					result.add(new FieldChange(entry.getKey(), entry.getValue(), null));
				}
			}
		}
		catch (Exception e) {
			log.warn("Could not parse serialized data for audit log " + auditLog.getUuid(), e);
		}
		return result;
	}

	private static List<AuditLogDTO> buildChildLogs(AuditLog auditLog) {
		List<AuditLogDTO> children = new ArrayList<AuditLogDTO>();
		for (AuditLog child : auditLog.getChildAuditLogs()) {
			children.add(AuditLogDTO.from(child));
		}
		return children;
	}

	public String getUuid() { return uuid; }
	public String getType() { return type; }
	public String getSimpleType() { return simpleType; }
	public String getIdentifier() { return identifier; }
	public String getAction() { return action; }
	public String getUserUuid() { return userUuid; }
	public String getUserDisplay() { return userDisplay; }
	public Date getDateCreated() { return dateCreated; }
	public boolean isHasChildLogs() { return hasChildLogs; }
	public List<FieldChange> getChanges() { return changes; }
	public List<AuditLogDTO> getChildLogs() { return childLogs; }

	public static class FieldChange {

		private final String field;

		private final String oldValue;

		private final String newValue;

		public FieldChange(String field, String oldValue, String newValue) {
			this.field = field;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public String getField() { return field; }
		public String getOldValue() { return oldValue; }
		public String getNewValue() { return newValue; }
	}
}
