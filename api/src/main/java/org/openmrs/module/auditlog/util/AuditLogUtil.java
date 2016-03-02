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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.proxy.HibernateProxy;
import org.openmrs.GlobalProperty;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.api.db.DAOUtils;

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
		if (auditLog.getSerializedData() != null) {
			try {
				String serializedStr = getAsString(auditLog.getSerializedData());
				if (StringUtils.isNotBlank(serializedStr)) {
					changes = new ObjectMapper().readValue(serializedStr, Map.class);
				}
			}
			catch (Exception e) {
				log.warn("Failed to convert serialized data to a map", e);
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
		if (auditLog.getSerializedData() != null) {
			try {
				String serializedStr = getAsString(auditLog.getSerializedData());
				if (StringUtils.isNotBlank(serializedStr)) {
					changes = new ObjectMapper().readValue(serializedStr, Map.class);
				}
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
	
	/**
	 * Gets the CollectionPersister for the collection matching the specified name in the specified
	 * class
	 * 
	 * @param collPropertyName
	 * @param clazz
	 * @should return the collection persister
	 * @should return the collection persister if the property is declared in a super class
	 */
	public static CollectionPersister getCollectionPersister(String collPropertyName, Class<?> clazz,
	                                                         SessionFactoryImplementor sfi) {
		if (sfi == null) {
			sfi = (SessionFactoryImplementor) DAOUtils.getSessionFactory();
		}
		CollectionPersister cp = null;
		try {
			cp = sfi.getCollectionPersister(clazz.getName() + "." + collPropertyName);
		}
		catch (MappingException e) {
			//check the super classes if any
			if (clazz.getSuperclass() != null) {
				cp = getCollectionPersister(collPropertyName, clazz.getSuperclass(), sfi);
			}
		}
		
		return cp;
	}
	
	public static void setGlobalProperty(String property, String propertyValue) {
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(property);
		if (gp == null) {
			gp = new GlobalProperty(property, propertyValue);
		} else {
			gp.setPropertyValue(propertyValue);
		}
		as.saveGlobalProperty(gp);
	}
	
	public static Class<?> getActualType(Object persistentObject) {
		Class<?> type = persistentObject.getClass();
		if (persistentObject instanceof HibernateProxy) {
			type = ((HibernateProxy) persistentObject).getHibernateLazyInitializer().getPersistentClass();
		}
		return type;
	}
	
	public static boolean isPersistent(Class<?> clazz) {
		return getClassMetadata(clazz) != null;
	}
	
	public static ClassMetadata getClassMetadata(Class<?> clazz) {
		return DAOUtils.getClassMetadata(clazz);
	}
	
	public static String getAsString(Blob blob) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(blob.getBinaryStream()));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
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
	public static String serializeObject(Object obj) {
		String serializedValue = null;
		if (obj != null) {
			Class<?> clazz = getActualType(obj);
			if (Date.class.isAssignableFrom(clazz)) {
				//TODO We need to handle time zones issues better
				serializedValue = new SimpleDateFormat(AuditLogConstants.DATE_FORMAT).format(obj);
			} else if (Enum.class.isAssignableFrom(clazz)) {
				//Use value.name() over value.toString() to ensure we always get back the enum
				//constant value and not the value returned by the implementation of value.toString()
				serializedValue = ((Enum<?>) obj).name();
			} else if (Class.class.isAssignableFrom(clazz)) {
				serializedValue = ((Class<?>) obj).getName();
			} else if (Collection.class.isAssignableFrom(clazz)) {
				serializedValue = serializeCollection((Collection) obj);
			} else if (Map.class.isAssignableFrom(clazz)) {
				serializedValue = serializeMap((Map) obj);
			}
			if (StringUtils.isBlank(serializedValue)) {
				ClassMetadata metadata = getClassMetadata(clazz);
				if (metadata != null) {
					Serializable id = metadata.getIdentifier(obj, EntityMode.POJO);
					if (id != null) {
						serializedValue = id.toString();
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
	 * Utility method that serializes the collection entries to a string
	 * 
	 * @param collection the Collection object
	 * @return The serialized collection elements
	 */
	public static String serializeCollection(Collection<?> collection) {
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
	public static String serializeMap(Map<?, ?> map) {
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
	 * Utility method that serializes the passed in data to json, this method asssumes all the
	 * passed in data is already serialized
	 * 
	 * @param data the data to serialize
	 * @return the generated json
	 */
	public static String serializeToJson(Object data) {
		String json = null;
		if (data != null) {
			try {
				json = new ObjectMapper().writeValueAsString(data);
			}
			catch (Exception e) {
				log.error("Failed to generate changes data", e);
			}
		}
		
		return json;
	}
}
