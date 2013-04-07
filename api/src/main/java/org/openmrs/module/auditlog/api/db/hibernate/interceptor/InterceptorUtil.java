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
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_CHANGES;
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_NEW;
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_PREVIOUS;
import static org.openmrs.module.auditlog.util.AuditLogConstants.NODE_PROPERTY;

import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Contains utility methods used by the interceptor
 */
public final class InterceptorUtil {
	
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
			
			sb.append("\n<" + NODE_PROPERTY + " " + ATTRIBUTE_NAME + "=\"" + entry.getKey() + "\">");
			//when deserializing, missing tags will be interpreted as NULL
			if (newValue != null) {
				sb.append("\n<" + NODE_NEW + ">");
				sb.append("\n" + StringEscapeUtils.escapeXml(newValue));
				sb.append("\n</" + NODE_NEW + ">");
			}
			if (previousValue != null) {
				sb.append("\n<" + NODE_PREVIOUS + ">");
				sb.append("\n" + StringEscapeUtils.escapeXml(previousValue));
				sb.append("\n</" + NODE_PREVIOUS + ">");
			}
			sb.append("\n</" + NODE_PROPERTY + ">");
		}
		
		sb.append("\n</" + NODE_CHANGES + ">");
		
		return sb.toString();
	}
}
