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
package org.openmrs.module.auditlog.api.db.hibernate.interceptor;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLogHelper;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.api.db.DAOUtils;
import org.openmrs.module.auditlog.util.AuditLogUtil;

/**
 * Contains utility methods used by the interceptor
 */
final class InterceptorUtil {
	
	private static final Log log = LogFactory.getLog(InterceptorUtil.class);
	
	private static AuditLogDAO auditLogDao;
	
	private static AuditLogHelper helper;
	
	/**
	 * @return the dao
	 */
	static AuditLogDAO getAuditLogDao() {
		if (auditLogDao == null) {
			auditLogDao = Context.getRegisteredComponents(AuditLogDAO.class).get(0);
		}
		return auditLogDao;
	}
	
	/**
	 * @return the helper
	 */
	static AuditLogHelper getHelper() {
		if (helper == null) {
			helper = Context.getRegisteredComponents(AuditLogHelper.class).get(0);
		}
		return helper;
	}
	
	static void saveAuditLog(AuditLog auditLog) {
		getAuditLogDao().save(auditLog);
	}
	
	/**
	 * Checks if a class is marked as audited or is explicitly audited
	 * 
	 * @param clazz the clazz to check
	 * @return true if is audited or implicitly audited otherwise false
	 */
	static boolean isAudited(Class<?> clazz) {
		return getHelper().isAudited(clazz) || getHelper().isImplicitlyAudited(clazz);
	}
	
	/**
	 * Serializes mapped hibernate objects
	 * 
	 * @param object the object to serialize
	 * @return the serialized JSON text
	 */
	static String serializePersistentObject(Object object) {
		//TODO Might be better to use xstream
		Map<String, Object> propertyNameValueMap = null;
		ClassMetadata cmd = DAOUtils.getClassMetadata(AuditLogUtil.getActualType(object));
		if (cmd != null) {
			propertyNameValueMap = new HashMap<String, Object>();
			propertyNameValueMap.put(cmd.getIdentifierPropertyName(), cmd.getIdentifier(object, EntityMode.POJO));
			for (String propertyName : cmd.getPropertyNames()) {
				Object value = cmd.getPropertyValue(object, propertyName, EntityMode.POJO);
				if (value != null) {
					Object serializedValue = null;
					if (cmd.getPropertyType(propertyName).isCollectionType()) {
						if (Collection.class.isAssignableFrom(value.getClass())) {
							serializedValue = AuditLogUtil.serializeCollectionItems((Collection) value);
						} else if (Map.class.isAssignableFrom(value.getClass())) {
							serializedValue = AuditLogUtil.serializeMapItems((Map) value);
						}
					} else {
						serializedValue = AuditLogUtil.serializeObject(value);
					}
					if (serializedValue != null) {
						propertyNameValueMap.put(propertyName, serializedValue);
					}
				}
			}
		}
		
		return AuditLogUtil.serializeToJson(propertyNameValueMap);
	}
	
	static SessionFactory getSessionFactory() {
		return Context.getRegisteredComponents(SessionFactory.class).get(0);
	}
	
	static boolean storeLastStateOfDeletedItems() {
		return getAuditLogDao().storeLastStateOfDeletedItems();
	}
	
	static Serializable getId(Object object) {
		return getAuditLogDao().getId(object);
	}
}
