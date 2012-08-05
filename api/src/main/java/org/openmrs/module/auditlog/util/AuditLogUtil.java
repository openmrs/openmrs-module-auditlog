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

import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Contains static utility methods
 */
public class AuditLogUtil {
	
	private static final Log log = LogFactory.getLog(AuditLogUtil.class);
	
	public static final String NODE_CHANGES = "changes";
	
	public static final String NODE_PROPERTY = "property";
	
	public static final String NODE_PREVIOUS = "previous";
	
	public static final String NODE_NEW = "new";
	
	public static final String ATTRIBUTE_NAME = "name";
	
	private static Set<String> monitoredClassnamesCache;
	
	public static boolean areMonitoredClassnamesCached() {
		return monitoredClassnamesCache != null;
	}
	
	/**
	 * Marks the specified classes as monitored by adding their class names to the
	 * {@link GlobalProperty} {@link AuditLogConstants#AUDITLOG_GP_MONITORED_CLASSES}
	 * 
	 * @param clazzes the classes to monitor
	 * @param subclassesToInclude list of subclasses to mark as monitored objects
	 * @should add the class names to the monitored objects global property
	 */
	public static void startMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		updateMonitoredClassesGP(clazzes, true);
	}
	
	/**
	 * Marks the specified classes as not monitored by removing their class names from the
	 * {@link GlobalProperty} {@link AuditLogConstants#AUDITLOG_GP_MONITORED_CLASSES}
	 * 
	 * @param clazzes the class to stop monitoring
	 * @should remove the class names from the monitored objects global property
	 */
	public static void stopMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		updateMonitoredClassesGP(clazzes, false);
	}
	
	/**
	 * Convenience method that checks if the class with the specified fully qualified name is
	 * monitored
	 * 
	 * @param classname the class name to check against
	 * @return true if the class with the specified fully qualified name is monitored otherwise
	 *         false
	 * @should return a list of monitored class names
	 */
	public static Set<String> getMonitoredClassNames() {
		if (monitoredClassnamesCache == null) {
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.AUDITLOG_GP_MONITORED_CLASSES);
			monitoredClassnamesCache = new HashSet<String>();
			if (gp != null && StringUtils.isNotBlank(gp.getPropertyValue())) {
				String[] classnameArray = StringUtils.split(gp.getPropertyValue(), ",");
				for (String classname : classnameArray) {
					monitoredClassnamesCache.add(classname.trim());
				}
			}
		}
		
		return monitoredClassnamesCache;
	}
	
	/**
	 * Utility method that generates the xml for edited properties including their previous and new
	 * property values of an edited object
	 * 
	 * @param propertyChangesMap mapping of edited properties to their previous and new values
	 * @return the generated xml text
	 */
	public static String generateChangesXml(Map<String, Object[]> propertyChangesMap) {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("\n<" + NODE_CHANGES + ">");
		for (Map.Entry<String, Object[]> entry : propertyChangesMap.entrySet()) {
			Object previousObj = entry.getValue()[0];
			Object newObj = entry.getValue()[1];
			//we shouldn't even be here since this is not a change
			if (previousObj == null && newObj == null)
				continue;
			
			sb.append("\n<" + NODE_PROPERTY + " " + ATTRIBUTE_NAME + "=\"" + entry.getKey() + "\">");
			//when deserializing, missing tags will be interpreted as NULL
			if (previousObj != null) {
				sb.append("\n<" + NODE_PREVIOUS + ">");
				sb.append("\n" + StringEscapeUtils.escapeXml(previousObj.toString()));
				sb.append("\n</" + NODE_PREVIOUS + ">");
			}
			if (newObj != null) {
				sb.append("\n<" + NODE_NEW + ">");
				sb.append("\n" + StringEscapeUtils.escapeXml(newObj.toString()));
				sb.append("\n</" + NODE_NEW + ">");
			}
			sb.append("\n</" + NODE_PROPERTY + ">");
		}
		
		sb.append("\n</" + NODE_CHANGES + ">");
		
		return sb.toString();
	}
	
	/**
	 * Gets the text content of a nested previous or new tag inside a property tag with a name
	 * attribute matching the specified property name
	 * 
	 * @param rootElement {@link Element} object
	 * @param propertyName
	 * @param nestedTagName specifies the name of the nested tag of a property tag whose value to
	 *            return i.e previous vs new
	 * @return the text content of the nested tag
	 * @throws Exception
	 */
	public static String getPreviousOrNewPropertyValue(Element rootElement, String propertyName, String nestedTagName)
	    throws Exception {
		Element propertyEle = getPropertyTagByNameAttribute(rootElement, propertyName);
		if (propertyEle != null) {
			Element newEle = getElement(propertyEle, nestedTagName);
			if (newEle != null) {
				if (newEle.getTextContent() != null)
					return newEle.getTextContent().trim();
			}
		}
		return null;
	}
	
	/**
	 * Utility method that converts an xml string to a {@link Document} object
	 * 
	 * @param xml the xml to convert
	 * @return {@link Document} object
	 * @throws Exception
	 */
	public static Document createDocument(String xml) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}
	
	/**
	 * Gets the property tag with a name attribute matching the specified value
	 * 
	 * @param rootElement {@link Element} object
	 * @param attributeValue
	 * @return the matching property {@link Element} object
	 * @throws Exception
	 */
	private static Element getPropertyTagByNameAttribute(Element rootElement, String attributeValue) throws Exception {
		NodeList nodeList = rootElement.getElementsByTagName(AuditLogUtil.NODE_PROPERTY);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element tag = (Element) nodeList.item(i);
			if (OpenmrsUtil.nullSafeEquals(tag.getAttribute(AuditLogUtil.ATTRIBUTE_NAME), attributeValue))
				return tag;
		}
		return null;
	}
	
	/**
	 * @param propertyElement {@link Element} object
	 * @return
	 * @throws Exception
	 */
	private static Element getElement(Element propertyElement, String tagName) throws Exception {
		NodeList nodeList = propertyElement.getElementsByTagName(tagName);
		if (nodeList != null) {
			if (nodeList.getLength() == 1)
				return (Element) nodeList.item(0);
			else if (nodeList.getLength() > 1)
				log.warn("Invalid changes xml: Found multiple " + tagName + " tags");
		}
		return null;
	}
	
	/**
	 * Update the value of the {@link GlobalProperty}
	 * {@link AuditLogConstants#AUDITLOG_GP_MONITORED_CLASSES} in the database
	 * 
	 * @param clazzes the classes to add or remove
	 * @param startMonitoring specifies if the the classes are getting added to removed
	 */
	private static void updateMonitoredClassesGP(Set<Class<? extends OpenmrsObject>> clazzes, boolean startMonitoring) {
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(AuditLogConstants.AUDITLOG_GP_MONITORED_CLASSES);
		if (gp == null) {
			if (!startMonitoring) {
				//should of course be null since the GP is blank too
				monitoredClassnamesCache = null;
				return;
			}
			gp = new GlobalProperty(AuditLogConstants.AUDITLOG_GP_MONITORED_CLASSES, null,
			        "Specifies the class names of objects for which to maintain an audit log");
		}
		
		if (monitoredClassnamesCache == null)
			getMonitoredClassNames();//should effectively load the set
			
		for (Class<? extends OpenmrsObject> clazz : clazzes) {
			if (startMonitoring)
				monitoredClassnamesCache.add(clazz.getName());
			else
				monitoredClassnamesCache.remove(clazz.getName());
		}
		
		gp.setPropertyValue(StringUtils.join(monitoredClassnamesCache, ","));
		try {
			as.saveGlobalProperty(gp);
		}
		finally {
			//The cache needs to be rebuilt
			monitoredClassnamesCache = null;
		}
	}
}
