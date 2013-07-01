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

import static org.openmrs.module.auditlog.util.AuditLogConstants.MAP_KEY_VALUE_SEPARATOR;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;

/**
 * Contains utility methods used by the interceptor
 */
public final class InterceptorUtil {
	
	private static final Log log = LogFactory.getLog(InterceptorUtil.class);
	
	/**
	 * Utility method that serializes the passed in data to json
	 * 
	 * @param data the data to serialize
	 * @return the generated json
	 */
	protected static String serializeToJson(Object data) {
		String json = null;
		if (data != null) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				json = mapper.writeValueAsString(data);
			}
			catch (Exception e) {
				log.error("Failed to generate changes data", e);
			}
		}
		
		return json;
	}
	
	/**
	 * Utility method that serializes the collection entries to a string
	 * 
	 * @param collection the Collection object
	 * @return The serialized collection elements
	 */
	protected static String serializeCollection(Collection<?> collection, AuditLogDAO auditLogDao) {
		List<Object> collectionItems = null;
		if (collection != null) {
			for (Object currItem : collection) {
				if (currItem == null)
					continue;
				
				String serializedItem = serializeObject(currItem, auditLogDao);
				if (serializedItem != null) {
					if (collectionItems == null)
						collectionItems = new ArrayList<Object>();
					
					collectionItems.add(serializedItem);
				}
			}
		}
		
		return serializeToJson(collectionItems);
	}
	
	/**
	 * Utility method that serializes the map entries to a string
	 * 
	 * @param map the Map object
	 * @return The serialized map entries
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
			
			String serializedKey = serializeObject(key, auditLogDao);
			String serializedValue = serializeObject(value, auditLogDao);
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
	 * @return the serialized String form of the object
	 */
	protected static String serializeObject(Object obj, AuditLogDAO auditLogDAO) {
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
				//In case the object doesn't support uuids, but why?
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
