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

import org.apache.commons.lang.StringUtils;
import org.openmrs.User;

/**
 * Contains static utility methods
 */
public class AuditLogUtil {
	
	//private static final Log log = LogFactory.getLog(AuditLogUtil.class);
	
	public static final String USER_DETAILS_DELIMITER = "|";
	
	public static final String USER_DETAILS_BLANK_FIELD = "-";
	
	/**
	 * Utility method that generates user details from the specified user object i.e user uuid,
	 * username and fullname separated by a pipe character
	 * 
	 * @param user the user to use to generate details
	 * @return a string consisting of the user details
	 */
	public static String getUserDetails(User user) {
		if (user == null)
			return "";
		
		String personName = "";
		if (user.getPerson() != null && user.getPerson().getPersonName() != null) {
			personName = user.getPerson().getPersonName().getFullName();
		}
		
		return user.getUuid() + USER_DETAILS_DELIMITER
		        + (StringUtils.isBlank(user.getUsername()) ? USER_DETAILS_BLANK_FIELD : user.getUsername())
		        + USER_DETAILS_DELIMITER + personName;
	}
}
