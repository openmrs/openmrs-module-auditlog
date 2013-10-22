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
package org.openmrs.module.auditlog.util;

import static org.openmrs.module.auditlog.AuditLog.Action.DELETED;
import static org.openmrs.module.auditlog.AuditLog.Action.UPDATED;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.APIException;
import org.openmrs.module.auditlog.AuditLog;

/**
 * Contains utility methods used by the module
 */
public class AuditLogUtil {
	
	private static final Log log = LogFactory.getLog(AuditLogUtil.class);
	
	/**
	 * Converts a set of class objects to a list of class name strings
	 * 
	 * @param clazzes
	 * @return
	 */
	public static List<String> getAsListOfClassnames(Set<Class<?>> clazzes) {
		List<String> classnames = new ArrayList<String>(clazzes.size());
		for (Class<?> clazz : clazzes) {
			classnames.add(clazz.getName());
		}
		return classnames;
	}
	
	/**
	 * Gets the class of the collection elements if the property with the specified name is a
	 * collection
	 * 
	 * @param owningType the type the collection belongs to
	 * @param propertyName the property name of the collection
	 * @return the class of the elements of the matching property
	 * @should return the class of the property
	 */
	public static Class<?> getCollectionElementType(Class<?> owningType, String propertyName) {
		Field field = getField(owningType, propertyName);
		if (field != null) {
			if (Collection.class.isAssignableFrom(field.getType())) {
				Type type = field.getGenericType();
				if (type instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) type;
					if (!ArrayUtils.isEmpty(pt.getActualTypeArguments())) {
						return (Class<?>) pt.getActualTypeArguments()[0];
					}
				}
			}
		} else {
			log.warn("Failed to find property " + propertyName + " in class " + owningType.getName());
		}
		
		return null;
	}
	
	/**
	 * Convenience method that find a field with the specified name in the specified class The
	 * method is recursively called to check all superclasses too
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Class<?> clazz, String fieldName) {
		Field field = null;
		try {
			field = clazz.getDeclaredField(fieldName);
		}
		catch (Exception e) {
			//check the super classes if any
			if (clazz.getSuperclass() != null)
				field = getField(clazz.getSuperclass(), fieldName);
		}
		
		return field;
	}
	
	/**
	 * Returns a map of changes for AuditLogs with action UPDATED
	 * 
	 * @param auditLog
	 * @return a map of changes
	 */
	public static Map<String, List> getChangesOfUpdatedItem(AuditLog auditLog) {
		if (auditLog.getAction() != UPDATED) {
			throw new APIException("Can't call this method for an AuditLog item with action " + auditLog.getAction());
		}
		
		Map<String, List> changes = new HashMap<String, List>();
		if (StringUtils.isNotBlank(auditLog.getSerializedData())) {
			try {
				changes = new ObjectMapper().readValue(auditLog.getSerializedData(), Map.class);
			}
			catch (Exception e) {
				log.warn("Failed to convert changes data to a map", e);
			}
		}
		
		return changes;
	}
	
	/**
	 * Returns a map of property names and values for AuditLogs with action DELETED
	 * 
	 * @param auditLog
	 * @return a map of property names and values
	 */
	public static Map<String, String> getLastStateOfDeletedItem(AuditLog auditLog) {
		if (auditLog.getAction() != DELETED) {
			throw new APIException("Can't call this method for an AuditLog item with action " + auditLog.getAction());
		}
		
		Map<String, String> changes = new HashMap<String, String>();
		if (StringUtils.isNotBlank(auditLog.getSerializedData())) {
			try {
				changes = new ObjectMapper().readValue(auditLog.getSerializedData(), Map.class);
			}
			catch (Exception e) {
				log.warn("Failed to convert serialized last state data to a map", e);
			}
		}
		
		return changes;
	}
	
	/**
	 * Gets the new property value for the specified property
	 * 
	 * @param propertyName
	 * @param auditLog
	 * @return the new property value if found
	 */
	public static String getNewValueOfUpdatedItem(String propertyName, AuditLog auditLog) {
		Map<String, List> changes = getChangesOfUpdatedItem(auditLog);
		if (changes.get(propertyName) != null) {
			return ((List<String>) changes.get(propertyName)).get(0);
		}
		return null;
	}
	
	/**
	 * Gets the old property value for the specified property
	 * 
	 * @param propertyName
	 * @param auditLog
	 * @return the old property value if found
	 */
	public static String getPreviousValueOfUpdatedItem(String propertyName, AuditLog auditLog) {
		Map<String, List> changes = getChangesOfUpdatedItem(auditLog);
		if (changes.get(propertyName) != null) {
			return ((List<String>) changes.get(propertyName)).get(1);
		}
		return null;
	}
	
}
