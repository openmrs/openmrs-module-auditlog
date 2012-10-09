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

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.springframework.beans.BeanUtils;

/**
 * Processes DWR calls for the module
 */
public class DWRAuditLogService {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Gets the {@link AuditLogDetails} for the auditlog with the specified uuid
	 * 
	 * @param auditLogUuid
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public AuditLogDetails getAuditLogDetails(String auditLogUuid) {
		if (StringUtils.isNotBlank(auditLogUuid)) {
			AuditLogService as = Context.getService(AuditLogService.class);
			AuditLog auditLog = as.getObjectByUuid(AuditLog.class, auditLogUuid);
			if (auditLog != null) {
				String displayString = "";
				boolean objectExists = false;
				Integer objectId = null;
				Map<String, String[]> propertyNameChangesMap = null;
				if (!auditLog.getAction().equals(Action.DELETED)) {
					Class<? extends OpenmrsObject> clazz;
					try {
						clazz = (Class<? extends OpenmrsObject>) Context.loadClass(auditLog.getClassName());
						OpenmrsObject obj = as.getObjectByUuid(clazz, auditLog.getObjectUuid());
						if (obj != null) {
							objectExists = true;
							//some objects don't support this method e.g GlobalProperties
							if (!GlobalProperty.class.isAssignableFrom(obj.getClass())) {
								try {
									objectId = obj.getId();
								}
								catch (Exception e) {
									log.error("Error:", e);
								}
								displayString = getDisplayString(obj, false);
							} else {
								displayString += ((GlobalProperty) obj).getProperty();
							}
							
							if (StringUtils.isBlank(displayString))
								displayString += obj.toString();
						}
						
						if (auditLog.getAction().equals(Action.UPDATED) && auditLog.getChanges().size() > 0) {
							propertyNameChangesMap = new HashMap<String, String[]>();
							for (Map.Entry<String, String[]> entry : auditLog.getChanges().entrySet()) {
								String propertyName = entry.getKey();
								String newValueDisplay = "";
								String preValueDisplay = "";
								if (!ArrayUtils.isEmpty(entry.getValue())) {
									String newValue = entry.getValue()[0];
									String previousValue = null;
									if (entry.getValue().length > 0)
										previousValue = entry.getValue()[1];
									if (StringUtils.isNotBlank(newValue) || StringUtils.isNotBlank(previousValue)) {
										if (StringUtils.isNotBlank(newValue)
										        && newValue.startsWith(AuditLogConstants.UUID_LABEL)) {
											newValueDisplay += getPropertyDisplayString(
											    as,
											    auditLog.getClassName(),
											    propertyName,
											    newValue.substring(newValue.indexOf(AuditLogConstants.UUID_LABEL)
											            + AuditLogConstants.UUID_LABEL.length()), true);
										} else if (StringUtils.isNotBlank(newValue)
										        && newValue.startsWith(AuditLogConstants.ID_LABEL)) {
											newValueDisplay += getPropertyDisplayString(
											    as,
											    auditLog.getClassName(),
											    propertyName,
											    newValue.substring(newValue.indexOf(AuditLogConstants.ID_LABEL)
											            + AuditLogConstants.ID_LABEL.length()), false);
										} else {
											newValueDisplay = (newValue != null) ? newValue : "";
										}
										
										if (StringUtils.isNotBlank(previousValue)
										        && previousValue.startsWith(AuditLogConstants.UUID_LABEL)) {
											preValueDisplay += getPropertyDisplayString(
											    as,
											    auditLog.getClassName(),
											    propertyName,
											    previousValue.substring(previousValue.indexOf(AuditLogConstants.UUID_LABEL)
											            + AuditLogConstants.UUID_LABEL.length()), true);
										} else if (StringUtils.isNotBlank(previousValue)
										        && previousValue.startsWith(AuditLogConstants.ID_LABEL)) {
											preValueDisplay += getPropertyDisplayString(
											    as,
											    auditLog.getClassName(),
											    propertyName,
											    previousValue.substring(previousValue.indexOf(AuditLogConstants.ID_LABEL)
											            + AuditLogConstants.ID_LABEL.length()), false);
										} else {
											preValueDisplay = (previousValue != null) ? previousValue : "";
										}
									}
								}
								
								propertyNameChangesMap.put(propertyName, new String[] { newValueDisplay, preValueDisplay });
							}
						}
					}
					catch (ClassNotFoundException e) {
						log.error("Cannot log class:" + auditLog.getClassName());
					}
				}
				return new AuditLogDetails(displayString, auditLog.getObjectUuid(), auditLog.getClassName(), auditLog
				        .getAction().name(), objectId, objectExists, propertyNameChangesMap);
			}
		}
		return null;
	}
	
	/**
	 * Gets the display string for a property
	 * 
	 * @param as
	 * @param owningEntityClassname
	 * @param propertyName
	 * @param uuidOrId
	 * @param isUuid
	 * @return
	 */
	private String getPropertyDisplayString(AuditLogService as, String owningEntityClassname, String propertyName,
	                                        String uuidOrId, boolean isUuid) {
		String displayString = "";
		try {
			PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(Context.loadClass(owningEntityClassname), propertyName);
			Object actualObject = null;
			if (isUuid) {
				actualObject = as.getObjectByUuid(pd.getPropertyType(), uuidOrId);
			} else {
				actualObject = as.getObjectById(pd.getPropertyType(), Integer.valueOf(uuidOrId));
			}
			
			if (actualObject != null) {
				displayString = getDisplayString(actualObject, true);
			}
		}
		catch (Exception e) {
			log.warn("Error", e);
		}
		
		return displayString;
	}
	
	/**
	 * @param obj
	 * @return
	 */
	private String getDisplayString(Object obj, boolean includeUuidAndId) {
		String displayString = "";
		if (OpenmrsMetadata.class.isAssignableFrom(obj.getClass())) {
			OpenmrsMetadata metadataObj = (OpenmrsMetadata) obj;
			if (StringUtils.isNotBlank(metadataObj.getName()))
				displayString += metadataObj.getName();
		} else if (Concept.class.isAssignableFrom(obj.getClass())) {
			Concept concept = (Concept) obj;
			displayString += ((concept.getName() != null) ? concept.getName().getName() : "");
		} else if (Person.class.isAssignableFrom(obj.getClass())) {
			Person person = (Patient) obj;
			displayString += ((person.getPersonName() != null) ? person.getPersonName().getFullName() : "");
		} else if (Obs.class.isAssignableFrom(obj.getClass())) {
			Obs obs = (Obs) obj;
			if (obs.getConcept() != null) {
				if (obs.getConcept().getName() != null)
					displayString += obs.getConcept().getName().getName();
			}
			
			displayString += obs.getValueAsString(Context.getLocale());
		}
		
		if (includeUuidAndId && OpenmrsObject.class.isAssignableFrom(obj.getClass())) {
			OpenmrsObject openmrsObj = (OpenmrsObject) obj;
			displayString = displayString + " - " + openmrsObj.getUuid() + " [" + openmrsObj.getId() + "]";
		}
		return displayString;
	}
}
