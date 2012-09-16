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

/**
 * Processes DWR calls for the module
 */
public class DWRAuditLogService {
	
	private final Log log = LogFactory.getLog(getClass());
	
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
				if (!auditLog.getAction().equals(Action.DELETED)) {
					Class<? extends OpenmrsObject> clazz;
					try {
						clazz = (Class<? extends OpenmrsObject>) Context.loadClass(auditLog.getClassName());
						OpenmrsObject obj = as.getObjectByUuid(clazz, auditLog.getObjectUuid());
						if (obj != null) {
							objectExists = true;
							//some objects don't support this method e.g GlobalPropeties
							if (!GlobalProperty.class.isAssignableFrom(obj.getClass())) {
								try {
									objectId = obj.getId();
								}
								catch (Exception e) {
									log.error("Error:", e);
								}
								if (OpenmrsMetadata.class.isAssignableFrom(obj.getClass())) {
									OpenmrsMetadata metadataObj = (OpenmrsMetadata) obj;
									if (StringUtils.isNotBlank(metadataObj.getName()))
										displayString += metadataObj.getName();
									if (StringUtils.isNotBlank(metadataObj.getDescription()))
										displayString += "[" + metadataObj.getDescription() + "]";
								} else if (Concept.class.isAssignableFrom(obj.getClass())) {
									Concept concept = (Concept) obj;
									displayString += ((concept.getName() != null) ? concept.getName().getName() : "");
								} else if (Person.class.isAssignableFrom(obj.getClass())) {
									Person person = (Patient) obj;
									displayString += ((person.getPersonName() != null) ? person.getPersonName()
									        .getFullName() : "");
								} else if (Obs.class.isAssignableFrom(obj.getClass())) {
									Obs obs = (Obs) obj;
									if (obs.getConcept() != null) {
										if (obs.getConcept().getName() != null)
											displayString += obs.getConcept().getName().getName();
									}
									
									displayString += obs.getValueAsString(Context.getLocale());
								}
							} else {
								displayString += ((GlobalProperty) obj).getProperty();
							}
							
							if (StringUtils.isBlank(displayString))
								displayString += obj.toString();
						}
					}
					catch (ClassNotFoundException e) {
						log.error("Cannot log class:" + auditLog.getClassName());
					}
				}
				return new AuditLogDetails(displayString, objectId, objectExists, auditLog.getChanges());
			}
		}
		return null;
	}
}
