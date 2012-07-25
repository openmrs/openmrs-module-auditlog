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

/**
 * Contains static utility methods
 */
public class AuditLogUtil {
	
	//private static final Log log = LogFactory.getLog(AuditLogUtil.class);
	
	public static final String NODE_PROPERTY = "property";
	
	public static final String NODE_PREVIOUS = "previous";
	
	public static final String NODE_NEW = "new";
	
	public static final String ATTRIBUTE_NAME = "name";
	
	/**
	 * Utility method that generates the xml for the old and new property values of an edited object
	 * 
	 * @param propertyName The name of the edited property
	 * @param previousValue the old value of the property
	 * @param newValue the new value of the property
	 * @return the generated xml text
	 */
	public static String generateNewAndPreviousValuesXml(String propertyName, Object previousValue, Object newValue) {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("Not yet implemented");
		
		return sb.toString();
	}
}
