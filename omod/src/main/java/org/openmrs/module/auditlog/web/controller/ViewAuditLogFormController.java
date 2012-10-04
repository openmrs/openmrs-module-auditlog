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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/auditlog/viewAuditLog.form'.
 */
@Controller
public class ViewAuditLogFormController {
	
	/** Logger for this class and subclasses */
	private static final Log log = LogFactory.getLog(ViewAuditLogFormController.class);
	
	/** Success form view name */
	private final String VIEW_AUDIT_LOG_FORM = "module/" + AuditLogConstants.MODULE_ID + "/viewAuditLog";
	
	/**
	 * @return
	 */
	@RequestMapping(VIEW_AUDIT_LOG_FORM)
	public void showForm(ModelMap model) {
		if (log.isDebugEnabled())
			log.debug("Fetching audit log entries...");
		
		model.addAttribute("auditLogs",
		    Context.getService(AuditLogService.class).getAuditLogs(null, null, null, null, null, null));
	}
}
