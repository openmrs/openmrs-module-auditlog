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

import java.beans.PropertyDescriptor;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.CollectionType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
import org.openmrs.Concept;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.MonitoringStrategy;
import org.openmrs.module.auditlog.api.AuditLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Contains static utility methods
 */
public class AuditLogUtil implements GlobalPropertyListener, ApplicationContextAware {
	
	private static final Log log = LogFactory.getLog(AuditLogUtil.class);
	
	public static final String NODE_CHANGES = "changes";
	
	public static final String NODE_PROPERTY = "property";
	
	public static final String NODE_PREVIOUS = "previous";
	
	public static final String NODE_NEW = "new";
	
	public static final String ATTRIBUTE_NAME = "name";
	
	private static Set<String> monitoredClassnamesCache;
	
	private static MonitoringStrategy monitoringStrategyCache;
	
	private static Set<String> unMonitoredClassnamesCache;
	
	private static Set<String> implicitlyMonitoredClassnamesCache;
	
	private static ApplicationContext applicationContext;
	
	private static SessionFactory sessionFactory;
	
	@Override
	public void setApplicationContext(ApplicationContext appContext) throws BeansException {
		applicationContext = appContext;
	}
	
	/**
	 * @return the monitoringStrategy
	 */
	public static MonitoringStrategy getMonitoringStrategy() {
		if (monitoringStrategyCache == null) {
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.GP_MONITORING_STRATEGY);
			if (gp != null) {
				if (StringUtils.isNotBlank(gp.getPropertyValue())) {
					String value = gp.getPropertyValue();
					monitoringStrategyCache = MonitoringStrategy.valueOf(value);
				}
			}
		}
		
		//default
		if (monitoringStrategyCache == null)
			monitoringStrategyCache = MonitoringStrategy.NONE;
		
		return monitoringStrategyCache;
	}
	
	/**
	 * @return
	 */
	public static boolean isMonitoringStrategyCached() {
		return monitoringStrategyCache != null;
	}
	
	/**
	 * @return
	 */
	public static boolean areMonitoredClassnamesCached() {
		return monitoredClassnamesCache != null;
	}
	
	/**
	 * @return
	 */
	public static boolean areUnMonitoredClassnamesCached() {
		return unMonitoredClassnamesCache != null;
	}
	
	/**
	 * Convenience method that marks a given object type as monitored
	 * 
	 * @param clazz the type to start monitoring
	 */
	public static void startMonitoring(Class<? extends OpenmrsObject> clazz) {
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(clazz);
		startMonitoring(classes);
	}
	
	/**
	 * Convenience method that marks a given object type as un monitored
	 * 
	 * @param clazz the type to stop monitoring
	 */
	public static void stopMonitoring(Class<? extends OpenmrsObject> clazz) {
		Set<Class<? extends OpenmrsObject>> classes = new HashSet<Class<? extends OpenmrsObject>>();
		classes.add(clazz);
		stopMonitoring(classes);
	}
	
	/**
	 * Marks the specified classes as monitored by adding their class names to the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * 
	 * @param clazzes the classes to monitor
	 * @param subclassesToInclude list of subclasses to mark as monitored objects
	 * @should update the monitored class names global property if the strategy is none_except
	 * @should not update any global property if the strategy is all
	 * @should not update any global property if the strategy is none
	 * @should update the un monitored class names global property if the strategy is all_except
	 * @should mark a class and its known subclasses as monitored
	 */
	public static void startMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		if (getMonitoringStrategy() == MonitoringStrategy.NONE_EXCEPT
		        || getMonitoringStrategy() == MonitoringStrategy.ALL_EXCEPT) {
			updateGlobalProperty(clazzes, true);
		}
	}
	
	/**
	 * Marks the specified classes as not monitored by removing their class names from the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * 
	 * @param clazzes the class to stop monitoring
	 * @should update the monitored class names global property if the strategy is none_except
	 * @should not update any global property if the strategy is all
	 * @should not update any global property if the strategy is none
	 * @should update the un monitored class names global property if the strategy is all_except
	 * @should mark a class and its known subclasses as un monitored
	 */
	public static void stopMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		if (getMonitoringStrategy() == MonitoringStrategy.NONE_EXCEPT
		        || getMonitoringStrategy() == MonitoringStrategy.ALL_EXCEPT) {
			updateGlobalProperty(clazzes, false);
		}
	}
	
	/**
	 * Convenience method that returns a set of monitored class names as specified by the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * 
	 * @return a set of monitored class names
	 * @should return a set of monitored class names
	 */
	public static Set<String> getMonitoredClassNames() {
		if (monitoredClassnamesCache == null) {
			monitoredClassnamesCache = new HashSet<String>();
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.GP_MONITORED_CLASSES);
			if (gp != null && StringUtils.isNotBlank(gp.getPropertyValue())) {
				String[] classnameArray = StringUtils.split(gp.getPropertyValue(), ",");
				for (String classname : classnameArray) {
					classname = classname.trim();
					monitoredClassnamesCache.add(classname);
					try {
						Set<Class<?>> subclasses = getConcreteSubclasses(Context.loadClass(classname), null);
						for (Class<?> subclass : subclasses) {
							monitoredClassnamesCache.add(subclass.getName());
						}
					}
					catch (ClassNotFoundException e) {
						log.error("Failed to load class:" + classname);
					}
				}
			}
		}
		
		return monitoredClassnamesCache;
	}
	
	/**
	 * Convenience method that returns a set of un monitored class names as specified by the
	 * {@link GlobalProperty} {@link AuditLogConstants#GP_UN_MONITORED_CLASSES}
	 * 
	 * @return a set of monitored class names
	 * @should return a set of un monitored class names
	 */
	public static Set<String> getUnMonitoredClassNames() {
		if (unMonitoredClassnamesCache == null) {
			unMonitoredClassnamesCache = new HashSet<String>();
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.GP_UN_MONITORED_CLASSES);
			if (gp != null && StringUtils.isNotBlank(gp.getPropertyValue())) {
				String[] classnameArray = StringUtils.split(gp.getPropertyValue(), ",");
				for (String classname : classnameArray) {
					classname = classname.trim();
					unMonitoredClassnamesCache.add(classname);
					try {
						Set<Class<?>> subclasses = getConcreteSubclasses(Context.loadClass(classname), null);
						for (Class<?> subclass : subclasses) {
							unMonitoredClassnamesCache.add(subclass.getName());
						}
					}
					catch (ClassNotFoundException e) {
						log.error("Failed to load class:" + classname);
					}
				}
			}
		}
		
		return unMonitoredClassnamesCache;
	}
	
	/**
	 * Gets implicitly monitored classnames, this are generated as a result of their owning entity
	 * types being marked as monitored if they are not explicitly marked as monitored, i.e if
	 * Concept is marked as monitored, then ConceptName, ConceptDesctiption, ConceptMapping etc
	 * implicitly get marked as monitored
	 * 
	 * @return a set of implicitly monitored classnames
	 * @should return a set of implicitly monitored classnames
	 */
	public static Set<String> getImplicitlyMonitoredClassNames() {
		if (implicitlyMonitoredClassnamesCache == null) {
			implicitlyMonitoredClassnamesCache = new HashSet<String>();
			for (String classname : getMonitoredClassNames()) {
				try {
					Class<?> monitoredClass = Context.loadClass(classname);
					addAssociationTypes(monitoredClass);
					
					Set<Class<?>> subclasses = getConcreteSubclasses(monitoredClass, null);
					for (Class<?> subclass : subclasses) {
						addAssociationTypes(subclass);
					}
				}
				catch (ClassNotFoundException e) {
					log.error("Failed to load class:" + classname);
				}
			}
		}
		
		return implicitlyMonitoredClassnamesCache;
	}
	
	private static void addAssociationTypes(Class<?> clazz) {
		for (Class<?> assocType : getAssociationTypesToMonitor(clazz, null)) {
			//If this type is not explicitly marked as monitored
			if (!getMonitoredClassNames().contains(assocType.getName())) {
				implicitlyMonitoredClassnamesCache.add(assocType.getName());
			}
		}
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
	 * @param propertyEle {@link Element} object
	 * @param getNew specifies which value to value to return i.e previous vs new
	 * @return the text content of the nested tag
	 * @throws Exception
	 */
	public static String getPreviousOrNewPropertyValue(Element propertyEle, boolean getNew) throws Exception {
		if (propertyEle != null) {
			String tagName = (getNew) ? NODE_NEW : NODE_PREVIOUS;
			Element ele = getElement(propertyEle, tagName);
			if (ele != null) {
				if (ele.getTextContent() != null)
					return ele.getTextContent().trim();
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
	 * Update the value of the {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * in the database
	 * 
	 * @param clazzes the classes to add or remove
	 * @param startMonitoring specifies if the the classes are getting added to removed
	 */
	private static void updateGlobalProperty(Set<Class<? extends OpenmrsObject>> clazzes, boolean startMonitoring) {
		boolean isNoneExceptStrategy = getMonitoringStrategy() == MonitoringStrategy.NONE_EXCEPT;
		AdministrationService as = Context.getAdministrationService();
		String gpName = isNoneExceptStrategy ? AuditLogConstants.GP_MONITORED_CLASSES
		        : AuditLogConstants.GP_UN_MONITORED_CLASSES;
		GlobalProperty gp = as.getGlobalPropertyObject(gpName);
		if (gp == null) {
			String description = (isNoneExceptStrategy) ? "Specifies the class names of objects for which to maintain an audit log, this property is only used when the monitoring strategy is set to NONE_EXCEPT"
			        : "Specifies the class names of objects for which not to maintain an audit log, this property is only used when the	monitoring strategy is set to ALL_EXCEPT";
			gp = new GlobalProperty(gpName, null, description);
		}
		
		if (isNoneExceptStrategy) {
			for (Class<? extends OpenmrsObject> clazz : clazzes) {
				if (startMonitoring)
					getMonitoredClassNames().add(clazz.getName());
				else {
					getMonitoredClassNames().remove(clazz.getName());
					//remove subclasses too
					Set<Class<?>> subclasses = getConcreteSubclasses(clazz, null);
					for (Class<?> subclass : subclasses) {
						getMonitoredClassNames().remove(subclass.getName());
					}
				}
			}
			
			gp.setPropertyValue(StringUtils.join(getMonitoredClassNames(), ","));
		} else {
			for (Class<? extends OpenmrsObject> clazz : clazzes) {
				if (startMonitoring) {
					getUnMonitoredClassNames().remove(clazz.getName());
					Set<Class<?>> subclasses = getConcreteSubclasses(clazz, null);
					for (Class<?> subclass : subclasses) {
						getUnMonitoredClassNames().remove(subclass.getName());
					}
				} else
					getUnMonitoredClassNames().add(clazz.getName());
			}
			
			gp.setPropertyValue(StringUtils.join(getUnMonitoredClassNames(), ","));
		}
		
		try {
			as.saveGlobalProperty(gp);
		}
		catch (Exception e) {
			//The cache needs to be rebuilt since we already updated the 
			//cached above but the GP value didn't get updated in the DB
			if (isNoneExceptStrategy)
				monitoredClassnamesCache = null;
			else
				unMonitoredClassnamesCache = null;
			implicitlyMonitoredClassnamesCache = null;
		}
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyChanged(org.openmrs.GlobalProperty)
	 */
	@Override
	public void globalPropertyChanged(GlobalProperty gp) {
		if (AuditLogConstants.GP_MONITORED_CLASSES.equals(gp.getProperty()))
			monitoredClassnamesCache = null;
		else if (AuditLogConstants.GP_UN_MONITORED_CLASSES.equals(gp.getProperty()))
			unMonitoredClassnamesCache = null;
		else {
			//we need to invalidate all caches when the strategy is changed
			monitoringStrategyCache = null;
			monitoredClassnamesCache = null;
			unMonitoredClassnamesCache = null;
		}
		implicitlyMonitoredClassnamesCache = null;
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyDeleted(java.lang.String)
	 */
	@Override
	public void globalPropertyDeleted(String gpName) {
		if (AuditLogConstants.GP_MONITORED_CLASSES.equals(gpName))
			monitoredClassnamesCache = null;
		else if (AuditLogConstants.GP_UN_MONITORED_CLASSES.equals(gpName))
			unMonitoredClassnamesCache = null;
		else {
			monitoringStrategyCache = null;
			monitoredClassnamesCache = null;
			unMonitoredClassnamesCache = null;
		}
		implicitlyMonitoredClassnamesCache = null;
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#supportsPropertyName(java.lang.String)
	 */
	@Override
	public boolean supportsPropertyName(String gpName) {
		return AuditLogConstants.GP_MONITORING_STRATEGY.equals(gpName)
		        || AuditLogConstants.GP_MONITORED_CLASSES.equals(gpName)
		        || AuditLogConstants.GP_UN_MONITORED_CLASSES.equals(gpName);
	}
	
	/**
	 * Gets a set of concrete subclasses for the specified class recursively, note that interfaces
	 * and abstract classes are excluded
	 * 
	 * @param clazz
	 * @param foundSubclasses the list of subclasses found in previous recursive calls, should be
	 *            null for the first call
	 * @return a set of subclasses
	 * @should return a list of subclasses for the specified type
	 * @should exclude interfaces and abstract classes
	 */
	public static Set<Class<?>> getConcreteSubclasses(Class<?> clazz, Set<Class<?>> foundSubclasses) {
		if (foundSubclasses == null)
			foundSubclasses = new HashSet<Class<?>>();
		
		if (clazz != null) {
			ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
			scanner.addIncludeFilter(new AssignableTypeFilter(clazz));
			try {
				//This assumes modules follow the 'org.openmrs' namespace
				Collection<BeanDefinition> beans = scanner.findCandidateComponents("org.openmrs");
				for (BeanDefinition bean : beans) {
					if (!clazz.getName().equals(bean.getBeanClassName())) {
						Class<?> beanClass = Context.loadClass(bean.getBeanClassName());
						foundSubclasses.add(beanClass);
						foundSubclasses.addAll(getConcreteSubclasses(beanClass, foundSubclasses));
					}
				}
			}
			catch (ClassNotFoundException e) {
				log.warn("Error:" + e.getMessage());//why?
			}
			finally {
				scanner.resetFilters(false);
			}
		}
		
		return foundSubclasses;
	}
	
	/**
	 * @param as
	 * @param owningEntityClassname
	 * @param propertyName
	 * @param uuidOrId
	 * @param isUuid
	 * @return
	 */
	public static String getPropertyDisplayString(AuditLogService as, String owningEntityClassname, String propertyName,
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
	public static String getDisplayString(Object obj, boolean includeUuidAndId) {
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
			displayString = "[" + openmrsObj.getId() + "] " + displayString + " - " + openmrsObj.getUuid();
		}
		return displayString;
	}
	
	/**
	 * Finds all the types for associations to monitor in as recursive way i.e if a Persistent type
	 * is found, then we also find its collection element types and types for fields mapped as one
	 * to one, note that this only includes sub types of {@link OpenmrsObject}
	 * 
	 * @param clazz
	 * @param foundAssocTypes the found
	 * @return a set of found class names
	 */
	private static Set<Class<?>> getAssociationTypesToMonitor(Class<?> clazz, Set<Class<?>> foundAssocTypes) {
		if (foundAssocTypes == null)
			foundAssocTypes = new HashSet<Class<?>>();
		
		ClassMetadata cmd = getSessionFactory().getClassMetadata(clazz);
		if (cmd != null) {
			for (Type type : cmd.getPropertyTypes()) {
				//If this is a OneToOne or a collection type
				if (type.isCollectionType() || OneToOneType.class.isAssignableFrom(type.getClass())) {
					Class<?> assocType = type.getReturnedClass();
					if (type.isCollectionType()) {
						assocType = ((CollectionType) type).getElementType((SessionFactoryImplementor) getSessionFactory())
						        .getReturnedClass();
					}
					if (OpenmrsObject.class.isAssignableFrom(assocType) && !foundAssocTypes.contains(assocType)) {
						foundAssocTypes.add(assocType);
						foundAssocTypes.addAll(getAssociationTypesToMonitor(assocType, foundAssocTypes));
					}
				}
			}
		}
		return foundAssocTypes;
	}
	
	/**
	 * Gets the {@link SessionFactory} object
	 * 
	 * @return
	 */
	private static SessionFactory getSessionFactory() {
		if (sessionFactory == null)
			sessionFactory = ((SessionFactory) applicationContext.getBean("sessionFactory"));
		return sessionFactory;
	}
}
