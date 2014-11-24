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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.openmrs.api.APIException;

/**
 * Constants used by the module.
 */
public final class AuditLogConstants {
	
	public static final String MODULE_ID = "auditlog";
	
	public static final String SEPARATOR = ",";
	
	public static final String MAP_KEY_VALUE_SEPARATOR = ":";
	
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	//Specifies the auditing strategy in use
	public static final String GP_AUDITING_STRATEGY = MODULE_ID + ".auditingStrategy";
	
	//Classes names for the objects to audit or not depending on the Auditing strategy
	public static final String GP_EXCEPTIONS = MODULE_ID + ".exceptions";
	
	//Specifies whether the last states of deleted items should be stored on the auditlog
	public static final String GP_STORE_LAST_STATE_OF_DELETED_ITEMS = MODULE_ID + ".storeLastStateOfDeletedItems";
	
	/* MODULE PRIVILEGES */
	public static final String PRIV_GET_AUDITLOGS = "Get Audit Logs";
	
	public static final String PRIV_MANAGE_AUDITLOG = "Manage Audit Log";
	
	public static final String PRIV_GET_ITEMS = "Get Items";
	
	public static final String MODULE_VERSION;
	
	static {
		InputStream file = AuditLogConstants.class.getClassLoader().getResourceAsStream(
		    "org/openmrs/module/auditlog/util/module.properties");
		if (file == null) {
			throw new APIException("Unable to find the module.properties file");
		}
		
		try {
			Properties props = new Properties();
			props.load(file);
			file.close();
			MODULE_VERSION = props.getProperty("moduleVersion");
		}
		catch (IOException e) {
			throw new APIException("Unable to parse the module.properties file", e);
		}
		finally {
			IOUtils.closeQuietly(file);
		}
	}
	
}
