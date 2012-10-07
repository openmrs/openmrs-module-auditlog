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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

public class AuditLogServiceImpl extends BaseOpenmrsService implements AuditLogService {
	
	//private static final Log log = LogFactory.getLog(AuditLogServiceImpl.class);
	
	private AuditLogDAO dao;
	
	/**
	 * @param dao the dao to set
	 */
	public void setDao(AuditLogDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * setter for AuditLogServiceDAO
	 */
	public void setAuditLogServiceDAO(AuditLogDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#getAuditLogs(List, List, java.util.Date,
	 *      java.util.Date, java.lang.Integer, java.lang.Integer)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<AuditLog> getAuditLogs(List<Class<? extends OpenmrsObject>> clazzes, List<Action> actions, Date startDate,
	                                   Date endDate, Integer start, Integer length) {
		if (OpenmrsUtil.compareWithNullAsEarliest(startDate, new Date()) > 0)
			throw new APIException(Context.getMessageSourceService().getMessage(
			    AuditLogConstants.MODULE_ID + ".exception.startDateInFuture"));
		List classesToMatch = null;
		if (clazzes != null) {
			classesToMatch = new ArrayList<Class<?>>();
			//TODO This should happen at startup/context refresh for performance reasons
			ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
			for (Class c : clazzes) {
				scanner.addIncludeFilter(new AssignableTypeFilter(c));
				try {
					//This assumes modules follow the 'org.openmrs' namespace
					Collection<BeanDefinition> beans = scanner.findCandidateComponents("org.openmrs");
					for (BeanDefinition bean : beans) {
						classesToMatch.add(bean.getBeanClassName());
					}
				}
				finally {
					scanner.resetFilters(false);
				}
			}
		}
		
		return dao.getAuditLogs(classesToMatch, actions, startDate, endDate, start, length);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.AuditLogService#getObjectById(java.lang.Class,
	 *      java.lang.Integer)
	 */
	@Override
	public <T> T getObjectById(Class<T> clazz, Integer id) {
		return dao.getObjectById(clazz, id);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.AuditLogService#getObjectByUuid(java.lang.Class,
	 *      java.lang.String)
	 */
	@Override
	public <T> T getObjectByUuid(Class<T> clazz, String uuid) {
		if (StringUtils.isBlank(uuid))
			return null;
		
		return dao.getObjectByUuid(clazz, uuid);
	}
}
