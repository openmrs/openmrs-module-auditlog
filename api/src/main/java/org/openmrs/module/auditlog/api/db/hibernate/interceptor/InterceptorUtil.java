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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.util.AuditLogConstants;

/**
 * Contains utility methods used by the interceptor
 */
final class InterceptorUtil {
	
	private static final Log log = LogFactory.getLog(InterceptorUtil.class);
	
	/**
	 * Utility method that serializes the passed in data to json, this method asssumes all the
	 * passed in data is already serialized
	 * 
	 * @param data the data to serialize
	 * @return the generated json
	 */
	static String serializeToJson(Object data) {
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
	static String serializeCollection(Collection<?> collection) {
		List<String> serializedCollectionItems = null;
		if (CollectionUtils.isNotEmpty(collection)) {
			serializedCollectionItems = new ArrayList<String>(collection.size());
			for (Object collItem : collection) {
				String serializedItem = serializeObject(collItem);
				if (serializedItem != null) {
					serializedCollectionItems.add(serializedItem);
				}
			}
		}
		
		return serializeToJson(serializedCollectionItems);
	}
	
	/**
	 * Utility method that serializes the map entries to a string
	 * 
	 * @param map the Map object
	 * @return The serialized map entries
	 */
	static String serializeMap(Map<?, ?> map) {
		Map<String, String> serializedMap = null;
		if (MapUtils.isNotEmpty(map)) {
			serializedMap = new HashMap<String, String>(map.size());
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String serializedKey = serializeObject(entry.getKey());
				String serializedValue = serializeObject(entry.getValue());
				if (serializedKey != null && serializedValue != null) {
					serializedMap.put(serializedKey, serializedValue);
				}
			}
		}
		
		return serializeToJson(serializedMap);
	}
	
	/**
	 * Serializes the specified object to a String, typically it returns the object's uuid if it is
	 * an OpenmrsObject, if not it returns the primary key value if it is a persistent object
	 * otherwise to calls the toString method except for Date, Enum and Class objects that are
	 * handled in a special way.
	 * 
	 * @param obj the object to serialize
	 * @return the serialized String form of the object
	 */
	static String serializeObject(Object obj) {
		String serializedValue = null;
		if (obj != null) {
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
			
			if (StringUtils.isBlank(serializedValue)) {
				ClassMetadata metadata = getClassMetadata(clazz);
				if (metadata != null) {
					Serializable id = metadata.getIdentifier(obj, EntityMode.POJO);
					if (id != null) {
						serializedValue = AuditLogConstants.ID_LABEL + id.toString();
					}
				}
			}
			
			if (StringUtils.isBlank(serializedValue)) {
				serializedValue = obj.toString();
			}
		}
		
		return serializedValue;
	}
	
	/**
	 * Serializes mapped hibernate objects
	 * 
	 * @param object the object to serialize
	 * @return the serialized JSON text
	 */
	static String serializePersistentObject(Object object) {
		//TODO Might be better to use xstream
		Map<String, Serializable> propertyNameValueMap = null;
		ClassMetadata cmd = InterceptorUtil.getClassMetadata(object.getClass());
		if (cmd != null) {
			propertyNameValueMap = new HashMap<String, Serializable>();
			propertyNameValueMap.put(cmd.getIdentifierPropertyName(), cmd.getIdentifier(object, EntityMode.POJO));
			for (String propertyName : cmd.getPropertyNames()) {
				String serializedValue = serializeObject(cmd.getPropertyValue(object, propertyName, EntityMode.POJO));
				if (serializedValue != null) {
					propertyNameValueMap.put(propertyName, serializedValue);
				}
			}
		}
		
		return serializeToJson(propertyNameValueMap);
	}
	
	static ClassMetadata getClassMetadata(Class<?> clazz) {
		return Context.getRegisteredComponents(SessionFactory.class).get(0).getClassMetadata(clazz);
	}
}
