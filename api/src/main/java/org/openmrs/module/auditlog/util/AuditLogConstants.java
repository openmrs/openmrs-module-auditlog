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
 * Constants used by the module.
 */
public final class AuditLogConstants {
	
	public static final String MODULE_ID = "auditlog";
	
	public static final String UUID_LABEL = "uuid:";
	
	public static final String ID_LABEL = "id:";
	
	public static final String SEPARATOR = ",";
	
	public static final String MAP_KEY_VALUE_SEPARATOR = ":";
	
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	//Specifies the monitoring strategy in use
	public static final String GP_MONITORING_STRATEGY = MODULE_ID + ".monitoringStrategy";
	
	//Classes names for the objects to monitor when the Monitoring strategy is set to NONE_EXCEPT
	public static final String GP_MONITORED_CLASSES = MODULE_ID + ".monitoredClasses";
	
	//Classes names for the objects not to monitor when the Monitoring strategy is set to ALL_EXCEPT
	public static final String GP_UN_MONITORED_CLASSES = MODULE_ID + ".unMonitoredClasses";
	
	/* MODULE PRIVILEGES */
	public static final String PRIV_GET_AUDITLOGS = "Get Audit Logs";
	
	public static final String PRIV_MANAGE_AUDITLOG = "Manage Audit Log";
	
	public static final String PRIV_GET_ITEMS = "Get Items";
	
}
