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
package org.openmrs.module.auditlog;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Encapsulates data for a single audit log entry
 */
public class AuditLog implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static final Log log = LogFactory.getLog(AuditLog.class);
	
	private Integer auditLogId;
	
	//the fully qualified java class name of the create/updated/deleted object
	private String className;
	
	//the uuid of the created/updated/deleted object
	private String objectUuid;
	
	//the performed operation that which could be a create, update or delete
	private Action action;
	
	private User user;
	
	private Date dateCreated;
	
	private String uuid;
	
	private transient Map<String, String[]> changes;
	
	/**
	 * Xml for new and old values in case of edited fields
	 * 
	 * <pre>
	 * 		<changes>
	 * 			<property name="property_name">
	 * 				<new>....</new>
	 * 				<previous>.....</previous>
	 * 			</property>
	 * 
	 * 			..... more properties
	 * 
	 * 		</changes>
	 * </pre>
	 */
	private String changesXml;
	
	public enum Action {
		CREATED, UPDATED, DELETED
		//, REVERTED
	}
	
	/**
	 * Default constructor
	 */
	public AuditLog() {
	}
	
	/**
	 * Convenience constructor
	 * 
	 * @param className the fully qualified classname of the Object type
	 * @param objectId the id of the object
	 * @param action the operation performed on the object
	 * @param user the user that triggered the operation
	 * @param dateCreated the date when the operation was done
	 */
	public AuditLog(String className, String objectUuid, Action action, User user, Date dateCreated) {
		this.className = className;
		this.objectUuid = objectUuid;
		this.action = action;
		this.user = user;
		this.dateCreated = dateCreated;
	}
	
	/**
	 * @return the auditLogId
	 */
	public Integer getAuditLogId() {
		return auditLogId;
	}
	
	/**
	 * @param auditLogId the auditLogId to set
	 */
	public void setAuditLogId(Integer auditLogId) {
		this.auditLogId = auditLogId;
	}
	
	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}
	
	/**
	 * @return the objectUuid
	 */
	public String getObjectUuid() {
		return objectUuid;
	}
	
	/**
	 * @param objectUuid the objectUuid to set
	 */
	public void setObjectUuid(String objectUuid) {
		this.objectUuid = objectUuid;
	}
	
	/**
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}
	
	/**
	 * @param action the action to set
	 */
	public void setAction(Action action) {
		this.action = action;
	}
	
	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}
	
	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}
	
	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}
	
	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}
	
	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * @param newAndPreviousValuesXml the newAndPreviousValuesXml to set
	 */
	public void setChangesXml(String newAndPreviousValuesXml) {
		this.changesXml = newAndPreviousValuesXml;
	}
	
	public String getSimpleClassname() {
		String section = getClassName().substring(getClassName().lastIndexOf(".") + 1);
		String[] sections = StringUtils.splitByCharacterTypeCamelCase(section);
		for (int i = 0; i < sections.length; i++) {
			sections[i] = StringUtils.capitalize(sections[i]);
		}
		
		return StringUtils.join(sections, " ");
	}
	
	/**
	 * Returns a map of changes where the key if the property name and the value is a String array
	 * of length 2 with the new value at index 0 and the previous value at index 1
	 * 
	 * @return
	 */
	public Map<String, String[]> getChanges() {
		if (StringUtils.isNotBlank(changesXml) && changes == null)
			changes = convertChangesXmlToMap(changesXml);
		else if (changes == null)
			changes = new HashMap<String, String[]>();
		
		return changes;
	}
	
	/**
	 * Takes the changes xml and converts it to a map where the key if the property name and the
	 * value is a String array of length 2 with the new value at index 0 and the previous value at
	 * index 1
	 * 
	 * @param changesXml the xml to convert
	 * @return the properties names mapped to their new and previous values
	 */
	private static Map<String, String[]> convertChangesXmlToMap(String changesXml) {
		Map<String, String[]> map = new HashMap<String, String[]>();
		Document doc;
		try {
			doc = AuditLogUtil.createDocument(changesXml);
			Element changesElement = doc.getDocumentElement();
			if (changesElement != null) {
				NodeList propertyElements = changesElement.getElementsByTagName(AuditLogUtil.NODE_PROPERTY);
				for (int i = 0; i < propertyElements.getLength(); i++) {
					Element propertyEle = (Element) propertyElements.item(i);
					String newValue = AuditLogUtil.getPreviousOrNewPropertyValue(propertyEle, true);
					String previousValue = AuditLogUtil.getPreviousOrNewPropertyValue(propertyEle, false);
					map.put(propertyEle.getAttribute(AuditLogUtil.ATTRIBUTE_NAME), new String[] { newValue, previousValue });
				}
			}
		}
		catch (Exception e) {
			log.error("Failed to parse changes xml", e);
		}
		return map;
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (!(obj instanceof AuditLog))
			return false;
		
		AuditLog other = (AuditLog) obj;
		if (getUuid() == null)
			return false;
		
		return other.getUuid().equals(this.getUuid());
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (getUuid() == null)
			return super.hashCode();
		return getUuid().hashCode();
	}
}
