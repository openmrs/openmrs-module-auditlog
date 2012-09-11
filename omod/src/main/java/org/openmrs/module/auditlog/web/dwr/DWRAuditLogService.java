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
package org.openmrs.module.auditlog.web.dwr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;

/**
 * Processes DWR calls for the module
 */
public class DWRAuditLogService {
	
	//private static final Log log = LogFactory.getLog(DWRAuditLogService.class);
	
	public AuditLogListItem getAuditLogDetails(Integer auditLogId) {
		return null;
	}
	
	/**
	 * Fetches the audit log entries matching the specified arguments
	 * 
	 * @param classnames the classnames to match against e.g for objects of type {@link Concept}
	 * @param actions the list of {@link Action}s to match against
	 * @param startDate the creation date of the log entries to return should be after or equal to
	 *            this date
	 * @param endDate the creation date of the log entries to return should be before or equal to
	 *            this date
	 * @param start index to start with (defaults to 0 if <code>null<code>)
	 * @param length number of results to return (default to return all matching results if
	 *            <code>null<code>)
	 * @return a list of {@link AuditLogListItem}s
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<AuditLogListItem> getAuditLogs(List<String> classnames, List<Action> actions, Date startDate, Date endDate,
	                                           Integer start, Integer length) throws Exception {
		List<Class<? extends OpenmrsObject>> clazzes = null;
		if (CollectionUtils.isNotEmpty(classnames)) {
			clazzes = new ArrayList<Class<? extends OpenmrsObject>>();
			for (String cn : classnames) {
				Class cls = Context.loadClass(cn);
				clazzes.add(cls);
			}
		}
		
		List<AuditLog> auditlogs = Context.getService(AuditLogService.class).getAuditLogs(clazzes, actions, startDate,
		    endDate, start, length);
		
		List<AuditLogListItem> results = new ArrayList<AuditLogListItem>();
		for (AuditLog auditLog : auditlogs) {
			results.add(new AuditLogListItem(auditLog));
		}
		
		return results;
	}
}
