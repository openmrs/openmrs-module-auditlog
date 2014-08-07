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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.openmrs.module.auditlog.web.util.AuditLogWebConstants;
import org.openmrs.util.Reflect;

/**
 * Processes DWR calls for the module
 */
public class DWRAuditLogService {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private AuditLogService service;
	
	private AuditLogService getService() {
		if (service == null) {
			service = Context.getService(AuditLogService.class);
		}
		return service;
	}
	
	/**
	 * Gets the {@link AuditLogDetails} for the auditlog with the specified uuid
	 * 
	 * @param auditLogUuid
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public AuditLogDetails getAuditLogDetails(String auditLogUuid) {
		
		Context.requirePrivilege(AuditLogWebConstants.PRIV_VIEW_AUDITLOG);
		
		if (StringUtils.isNotBlank(auditLogUuid)) {
			AuditLog auditLog = getService().getObjectByUuid(AuditLog.class, auditLogUuid);
			if (auditLog != null) {
				String displayString = "";
				boolean objectExists = false;
				String objectId = null;
				Map<String, Object> otherData = new HashMap<String, Object>();
				try {
					Class<? extends OpenmrsObject> clazz = (Class<? extends OpenmrsObject>) Context.loadClass(auditLog
					        .getClassName());
					if (!auditLog.getAction().equals(Action.DELETED)) {
						
						OpenmrsObject obj = getService().getObjectByUuid(clazz, auditLog.getObjectUuid());
						if (obj != null) {
							objectExists = true;
							//some objects don't support this method e.g GlobalProperties
							if (!GlobalProperty.class.isAssignableFrom(obj.getClass())) {
								try {
									objectId = obj.getId() != null ? obj.getId().toString() : "";
								}
								catch (UnsupportedOperationException e) {
									//ignore
								}
								displayString = getDisplayString(obj, false);
							} else {
								displayString += ((GlobalProperty) obj).getProperty();
							}
						}
						
						if (auditLog.getAction().equals(Action.UPDATED)) {
							Map<String, List> changes = AuditLogUtil.getChangesOfUpdatedItem(auditLog);
							if (changes.size() > 0) {
								for (Map.Entry<String, List> entry : changes.entrySet()) {
									String propertyName = entry.getKey();
									String previousValue = null;
									String newValueDisplay = "";
									String preValueDisplay = "";
									String newValue = null;
									if (CollectionUtils.isNotEmpty(entry.getValue())) {
										newValue = AuditLogUtil.getNewValueOfUpdatedItem(propertyName, auditLog);
										previousValue = AuditLogUtil.getPreviousValueOfUpdatedItem(propertyName, auditLog);
										if (StringUtils.isNotBlank(newValue) || StringUtils.isNotBlank(previousValue)) {
											newValueDisplay += getPrettyPropertyValue(propertyName, newValue, clazz);
											preValueDisplay += getPrettyPropertyValue(propertyName, previousValue, clazz);
										}
									}
									
									otherData.put(propertyName, new String[] { newValueDisplay, preValueDisplay });
								}
							}
						}
						
					} else {
						Map<String, String> changes = AuditLogUtil.getLastStateOfDeletedItem(auditLog);
						for (Map.Entry<String, String> entry : changes.entrySet()) {
							otherData.put(entry.getKey(), getPrettyPropertyValue(entry.getKey(), entry.getValue(), clazz));
						}
					}
				}
				catch (ClassNotFoundException e) {
					log.error("Cannot log class:" + auditLog.getClassName());
				}
				
				AuditLogDetails details = new AuditLogDetails(displayString, auditLog.getObjectUuid(),
				        auditLog.getClassName(), auditLog.getAction().name(), objectId, auditLog.getUuid(),
				        auditLog.getOpenmrsVersion(), objectExists, otherData);
				if (auditLog.hasChildLogs()) {
					List<AuditLogDetails> childDetails = new ArrayList<AuditLogDetails>();
					for (AuditLog childLog : auditLog.getChildAuditLogs()) {
						childDetails.add(new AuditLogDetails(null, childLog.getUuid(), childLog.getSimpleClassname(),
						        childLog.getAction().name(), null, childLog.getUuid(), auditLog.getOpenmrsVersion(), false,
						        null));
					}
					details.setChildAuditLogDetails(childDetails);
				}
				
				return details;
			}
		}
		return null;
	}
	
	/**
	 * Gets the display string for a property
	 * 
	 * @param owningType
	 * @param propertyName
	 * @param propertyValue
	 * @return the display text
	 */
	private String getPropertyDisplayString(Class<?> owningType, String propertyName, Class<?> propertyType,
	                                        String propertyValue) {
		String displayString = "";
		try {
			Object actualObject = null;
			if (Reflect.isCollection(propertyType)) {
				//TODO this not to fail if the primary key was a String e.g for privileges and had a ',' in it
				String[] uuidsOrIds = StringUtils.split(propertyValue, ",");
				List<Object> items = new ArrayList<Object>();
				List<String> unmatchedUuidsOrIds = new ArrayList<String>();
				for (String currUuidOrStr : uuidsOrIds) {
					Object item = null;
					currUuidOrStr = currUuidOrStr.trim();
					Class<?> itemType = AuditLogUtil.getCollectionElementType(owningType, propertyName);
					if (currUuidOrStr.startsWith(AuditLogConstants.UUID_LABEL)) {
						currUuidOrStr = currUuidOrStr.substring(currUuidOrStr.indexOf(AuditLogConstants.UUID_LABEL)
						        + AuditLogConstants.UUID_LABEL.length());
						item = getService().getObjectByUuid(itemType, currUuidOrStr);
					} else {
						currUuidOrStr = currUuidOrStr.substring(currUuidOrStr.indexOf(AuditLogConstants.ID_LABEL)
						        + AuditLogConstants.ID_LABEL.length());
						try {
							item = getService().getObjectById(itemType, Integer.valueOf(currUuidOrStr));
						}
						catch (NumberFormatException nfe) {
							//ignore
						}
					}
					
					if (item != null) {
						items.add(item);
					} else {
						unmatchedUuidsOrIds.add(currUuidOrStr);
					}
				}
				
				StringBuilder sb = new StringBuilder("<ul class='" + AuditLogConstants.MODULE_ID + "_collection_property'>");
				for (Object o1 : items) {
					sb.append("<li class='" + AuditLogConstants.MODULE_ID + "_collection_item'>"
					        + getDisplayString(o1, true) + "</li>");
				}
				for (String str : unmatchedUuidsOrIds) {
					sb.append("<li class='" + AuditLogConstants.MODULE_ID + "_collection_item "
					        + AuditLogConstants.MODULE_ID + "_collection_item_unmatched'>" + str + "</li>");
				}
				sb.append("</ul>");
				displayString += sb.toString();
			} else {
				if (propertyValue.startsWith(AuditLogConstants.UUID_LABEL)) {
					propertyValue = propertyValue.substring(propertyValue.indexOf(AuditLogConstants.UUID_LABEL)
					        + AuditLogConstants.UUID_LABEL.length());
					actualObject = getService().getObjectByUuid(propertyType, propertyValue);
				} else {
					propertyValue = propertyValue.substring(propertyValue.indexOf(AuditLogConstants.ID_LABEL)
					        + AuditLogConstants.ID_LABEL.length());
					actualObject = getService().getObjectById(propertyType, Integer.valueOf(propertyValue));
				}
				
				if (actualObject != null) {
					displayString = getDisplayString(actualObject, true);
				} else {
					displayString = "<span class=" + AuditLogConstants.MODULE_ID + "'_deleted'>" + propertyValue + "</span>";
				}
			}
		}
		catch (Exception e) {
			log.warn("Error:", e);
		}
		
		return displayString;
	}
	
	/**
	 * Generates the display text for the specified object
	 * 
	 * @param obj
	 * @return the display text
	 */
	private String getDisplayString(Object obj, boolean includeUuidAndId) {
		String displayString = "";
		if (Concept.class.isAssignableFrom(obj.getClass())) {
			Concept concept = (Concept) obj;
			displayString += ((concept.getName() != null) ? concept.getName().getName() : "");
		} else if (Person.class.isAssignableFrom(obj.getClass())) {
			Person person = (Person) obj;
			displayString += ((person.getPersonName() != null) ? person.getPersonName().getFullName() : "");
		} else if (User.class.isAssignableFrom(obj.getClass())) {
			User user = (User) obj;
			displayString += ((user.getPersonName() != null) ? user.getPersonName().getFullName() : "");
			displayString += " [";
			if (StringUtils.isNotBlank(user.getUsername()))
				displayString += user.getUsername() + " - ";
			displayString += user.getSystemId() + "]";
		} else if (Obs.class.isAssignableFrom(obj.getClass())) {
			Obs obs = (Obs) obj;
			if (obs.getConcept() != null) {
				if (obs.getConcept().getName() != null) {
					displayString += obs.getConcept().getName().getName();
				}
			}
			
			displayString += obs.getValueAsString(Context.getLocale());
		} else if (OpenmrsMetadata.class.isAssignableFrom(obj.getClass())) {
			OpenmrsMetadata metadataObj = (OpenmrsMetadata) obj;
			if (StringUtils.isNotBlank(metadataObj.getName())) {
				displayString += metadataObj.getName();
			}
		}
		
		if (StringUtils.isBlank(displayString)) {
			displayString += obj.toString();
		}
		
		if (includeUuidAndId && OpenmrsObject.class.isAssignableFrom(obj.getClass())) {
			OpenmrsObject openmrsObj = (OpenmrsObject) obj;
			String id = "";
			String uuid = "";
			try {
				if (openmrsObj.getUuid() != null) {
					uuid = " [" + openmrsObj.getUuid() + "]";
				}
			}
			catch (Exception e) {
				//ignore
			}
			try {
				if (openmrsObj.getId() != null) {
					id = " [" + openmrsObj.getId() + "]";
				}
			}
			catch (Exception e) {
				//ignore
			}
			if (StringUtils.isBlank(displayString)) {
				displayString = uuid + id;
			} else {
				displayString = displayString + " - " + uuid + id;
			}
		}
		
		return displayString;
	}
	
	private String getPrettyPropertyValue(String propertyName, Object value, Class<?> clazz) {
		String prettyValue = null;
		String stringValue = null;
		Field field = AuditLogUtil.getField(clazz, propertyName);
		//This can be null if the auditlog was created and then 
		//later upgraded to a version where the field was removed
		if (field != null && value != null) {
			stringValue = value.toString();
			if (stringValue.startsWith(AuditLogConstants.UUID_LABEL) || stringValue.startsWith(AuditLogConstants.ID_LABEL)) {
				prettyValue = getPropertyDisplayString(clazz, propertyName, field.getType(), stringValue);
			}
		}
		
		if (prettyValue == null && stringValue != null) {
			prettyValue = stringValue;
		}
		
		if (prettyValue == null) {
			prettyValue = "";
		}
		
		return prettyValue;
	}
}
