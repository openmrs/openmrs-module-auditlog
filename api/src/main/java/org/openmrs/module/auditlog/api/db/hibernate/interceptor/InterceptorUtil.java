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

import static org.openmrs.module.auditlog.util.AuditLogConstants.ATTRIBUTE_NAME;
import static org.openmrs.module.auditlog.util.AuditLogConstants.MAP_KEY_VALUE_SEPARATOR;
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_CHANGES;
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_NEW;
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_PREVIOUS;
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_PROPERTY;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;

/**
 * Contains utility methods used by the interceptor
 */
public final class InterceptorUtil {
	
	//private static final Log log = LogFactory.getLog(InterceptorUtil.class);
	
	/**
	 * Utility method that generates the data for edited properties including their previous and new
	 * property values of an edited object
	 * 
	 * @param propertyChangesMap mapping of edited properties to their previous and new values
	 * @return the generated text data
	 */
	protected static String generateChangesData(Map<String, String[]> propertyChangesMap) {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("\n<" + NODE_CHANGES + ">");
		for (Map.Entry<String, String[]> entry : propertyChangesMap.entrySet()) {
			String newValue = entry.getValue()[0];
			String previousValue = entry.getValue()[1];
			//we shouldn't even be here since this is not a change
			if (previousValue == null && newValue == null)
				continue;
			
			sb.append("\n<" + NODE_PROPERTY + " " + ATTRIBUTE_NAME + "=\"").append(entry.getKey()).append("\">");
			//when deserializing, missing tags will be interpreted as NULL
			if (newValue != null) {
				sb.append("\n<" + NODE_NEW + ">");
				sb.append("\n").append(StringEscapeUtils.escapeXml(newValue));
				sb.append("\n</" + NODE_NEW + ">");
			}
			if (previousValue != null) {
				sb.append("\n<" + NODE_PREVIOUS + ">");
				sb.append("\n").append(StringEscapeUtils.escapeXml(previousValue));
				sb.append("\n</" + NODE_PREVIOUS + ">");
			}
			sb.append("\n</" + NODE_PROPERTY + ">");
		}
		
		sb.append("\n</" + NODE_CHANGES + ">");
		
		return sb.toString();
	}
	
	/**
	 * Utility method that generated the a comma delimited string of the passed in collection's
	 * items
	 * 
	 * @param collection the Collection object
	 * @return a comma delimited string of the serialized collection elements
	 */
	protected static String serializeCollection(Collection<?> collection, AuditLogDAO auditLogDao) {
		if (collection == null)
			return null;
		
		String serializedCollectionItems = null;
		boolean isFirst = true;
		for (Object currItem : collection) {
			if (currItem == null)
				continue;
			
			String serializedItem = serializeValue(currItem, auditLogDao);
			if (serializedItem != null) {
				if (serializedCollectionItems == null)
					serializedCollectionItems = "";
				
				if (isFirst) {
					serializedCollectionItems += serializedItem;
					isFirst = false;
				} else {
					serializedCollectionItems += (AuditLogConstants.SEPARATOR + serializedItem);
				}
			}
		}
		
		return serializedCollectionItems;
	}
	
	/**
	 * Utility method that generated the a comma delimited string of the passed in map's entries
	 * 
	 * @param map the Map object
	 * @return a comma delimited string of the serialized map entries
	 */
	protected static String serializeMap(Map<?, ?> map, AuditLogDAO auditLogDao) {
		if (map == null)
			return null;
		
		String serializedMapEntries = null;
		boolean isFirst = true;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (key == null && value == null)
				continue;
			
			String serializedKey = serializeValue(key, auditLogDao);
			String serializedValue = serializeValue(value, auditLogDao);
			String serializedMapEntry = "";
			if (serializedKey != null)
				serializedMapEntry += serializedKey;
			if (serializedValue != null)
				serializedMapEntry += (MAP_KEY_VALUE_SEPARATOR + serializedValue);
			
			if (serializedMapEntries == null)
				serializedMapEntries = "";
			
			if (isFirst) {
				serializedMapEntries += serializedMapEntry;
				isFirst = false;
			} else {
				serializedMapEntries += (AuditLogConstants.SEPARATOR + serializedMapEntry);
			}
		}
		
		return serializedMapEntries;
	}
	
	/**
	 * Serializes the specified object to a String, typically it returns the object's uuid if it is
	 * an OpenmrsObject, if not it returns the primary key value if it is a persistent object
	 * otherwise to calls the toString method except for Date, Enum and Class objects that are
	 * handled in a special way.
	 * 
	 * @param obj the object to serialize
	 * @param auditLogDAO the AuditLogDAO object
	 * @return the serialized String form
	 */
	protected static String serializeValue(Object obj, AuditLogDAO auditLogDAO) {
		if (obj == null)
			return null;
		
		String serializedValue = null;
		Class<?> clazz = obj.getClass();
		if (Date.class.isAssignableFrom(clazz)) {
			//TODO We need to handle time zones issues better
			serializedValue = new SimpleDateFormat(AuditLogConstants.DATE_FORMAT).format(obj);
		} else if (Enum.class.isAssignableFrom(clazz)) {
			//Use value.name() over value.toString() to ensure we always get back the enum
			//constant value and not the value returned by the implementation of value.toString()
			serializedValue = ((Enum<?>) obj).name();
		} else if (Class.class.isAssignableFrom(clazz)) {
			serializedValue = ((Class<?>) obj).getName();
		} else if (OpenmrsObject.class.isAssignableFrom(clazz)) {
			try {
				serializedValue = AuditLogConstants.UUID_LABEL + ((OpenmrsObject) obj).getUuid();
			}
			catch (Exception e) {
				//In case the object doesn't support uuids, buy why?
			}
		}
		
		if (serializedValue == null && auditLogDAO != null) {
			ClassMetadata metadata = auditLogDAO.getClassMetadata(clazz);
			if (metadata != null) {
				Serializable id = metadata.getIdentifier(obj, EntityMode.POJO);
				if (id != null) {
					serializedValue = AuditLogConstants.ID_LABEL + id.toString();
				}
			}
		}
		
		if (serializedValue == null)
			serializedValue = obj.toString();
		
		return serializedValue;
	}
}
