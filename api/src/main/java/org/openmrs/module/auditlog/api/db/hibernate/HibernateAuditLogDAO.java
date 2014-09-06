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
package org.openmrs.module.auditlog.api.db.hibernate;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.CollectionType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.MonitoringStrategy;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;

public class HibernateAuditLogDAO implements AuditLogDAO, GlobalPropertyListener {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private static Set<Class<? extends OpenmrsObject>> monitoredTypeCache;
	
	private static MonitoringStrategy monitoringStrategyCache;
	
	private static Set<Class<? extends OpenmrsObject>> unMonitoredTypeCache;
	
	private static Set<Class<? extends OpenmrsObject>> implicitlyMonitoredTypeCache;
	
	private SessionFactory sessionFactory;
	
	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#isMonitored(Class)
	 */
	@Override
	public boolean isMonitored(Class<?> clazz) {
		//We need to stop hibernate auto flushing which might happen as we fetch
		//the GP values, Otherwise if a flush happens, then the interceptor
		//logic will be called again which will result in an infinite loop/stack overflow
		if (monitoredTypeCache == null || monitoringStrategyCache == null) {
			FlushMode originalFlushMode = sessionFactory.getCurrentSession().getFlushMode();
			sessionFactory.getCurrentSession().setFlushMode(FlushMode.MANUAL);
			try {
				return isMonitoredInternal(clazz);
			}
			finally {
				//reset
				sessionFactory.getCurrentSession().setFlushMode(originalFlushMode);
			}
		}
		
		return isMonitoredInternal(clazz);
	}
	
	/**
	 * Checks if specified object is among the ones that are monitored and is an
	 * {@link OpenmrsObject}
	 * 
	 * @param clazz the class to check against
	 * @return true if it is monitored otherwise false
	 */
	private boolean isMonitoredInternal(Class<?> clazz) {
		if (!OpenmrsObject.class.isAssignableFrom(clazz) || getMonitoringStrategy() == null
		        || getMonitoringStrategy() == MonitoringStrategy.NONE) {
			return false;
		}
		if (getMonitoringStrategy() == MonitoringStrategy.ALL) {
			return true;
		}
		
		if (getMonitoringStrategy() == MonitoringStrategy.NONE_EXCEPT) {
			return getMonitoredClasses().contains(clazz);
		}
		//Strategy is ALL_EXCEPT
		return !getUnMonitoredClasses().contains(clazz);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#isImplicitlyMonitored(java.lang.Class)
	 */
	@Override
	public boolean isImplicitlyMonitored(Class<?> clazz) {
		//We need to stop hibernate auto flushing which might happen as we fetch
		//the GP values, Otherwise if a flush happens, then the interceptor
		//logic will be called again which will result in an infinite loop/stack overflow
		if (implicitlyMonitoredTypeCache == null) {
			FlushMode originalFlushMode = sessionFactory.getCurrentSession().getFlushMode();
			sessionFactory.getCurrentSession().setFlushMode(FlushMode.MANUAL);
			try {
				return isImplicitlyMonitoredInternal(clazz);
			}
			finally {
				//reset
				sessionFactory.getCurrentSession().setFlushMode(originalFlushMode);
			}
		}
		
		return isImplicitlyMonitoredInternal(clazz);
	}
	
	/**
	 * Checks if specified object is among the ones that are implicitly monitored and is an
	 * {@link OpenmrsObject}
	 * 
	 * @param clazz the class to check against
	 * @return true if it is implicitly monitored otherwise false
	 */
	private boolean isImplicitlyMonitoredInternal(Class<?> clazz) {
		if (!OpenmrsObject.class.isAssignableFrom(clazz) || getMonitoringStrategy() == null
		        || getMonitoringStrategy() == MonitoringStrategy.NONE) {
			return false;
		}
		
		return getImplicitlyMonitoredClasses().contains(clazz);
	}
	
	/**
	 * @see AuditLogDAO#getAuditLogs(String, List, List, Date, Date, boolean, Integer, Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<AuditLog> getAuditLogs(String uuid, List<Class<? extends OpenmrsObject>> types, List<Action> actions,
	                                   Date startDate, Date endDate, boolean excludeChildAuditLogs, Integer start,
	                                   Integer length) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(AuditLog.class);
		if (uuid != null) {
			criteria.add(Restrictions.eq("objectUuid", uuid));
		}
		
		if (types != null) {
			criteria.add(Restrictions.in("type", types));
		}
		if (actions != null) {
			criteria.add(Restrictions.in("action", actions));
		}
		if (excludeChildAuditLogs) {
			criteria.add(Restrictions.isNull("parentAuditLog"));
		}
		if (startDate != null) {
			criteria.add(Restrictions.ge("dateCreated", startDate));
		}
		if (endDate != null) {
			criteria.add(Restrictions.le("dateCreated", endDate));
		}
		if (start != null) {
			criteria.setFirstResult(start);
		}
		if (length != null && length > 0) {
			criteria.setMaxResults(length);
		}
		
		//Show the latest logs first
		criteria.addOrder(Order.desc("dateCreated"));
		
		return criteria.list();
	}
	
	/**
	 * @see AuditLogDAO#save(Object)
	 */
	@Override
	public <T> T save(T object) {
		if (object instanceof AuditLog) {
			AuditLog auditLog = (AuditLog) object;
			//Hibernate has issues with saving the parentAuditLog field if the parent isn't yet saved
			//so we need to first save the parent before its children
			if (auditLog.getParentAuditLog() != null && auditLog.getParentAuditLog().getAuditLogId() == null) {
				save(auditLog.getParentAuditLog());
			}
		}
		
		sessionFactory.getCurrentSession().saveOrUpdate(object);
		return object;
	}
	
	/**
	 * @see AuditLogDAO#delete(Object)
	 */
	@Override
	public void delete(Object object) {
		sessionFactory.getCurrentSession().delete(object);
	}
	
	/**
	 * @see AuditLogDAO#getObjectById(Class, Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObjectById(Class<T> clazz, Integer id) {
		return (T) sessionFactory.getCurrentSession().get(clazz, id);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getObjectByUuid(java.lang.Class,
	 *      java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObjectByUuid(Class<T> clazz, String uuid) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(clazz);
		criteria.add(Restrictions.eq("uuid", uuid));
		return (T) criteria.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getPersistentConcreteSubclasses(java.lang.Class)
	 */
	@Override
	public Set<Class<? extends OpenmrsObject>> getPersistentConcreteSubclasses(Class<? extends OpenmrsObject> clazz) {
		return getPersistentConcreteSubclassesInternal(clazz, null, null);
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getAssociationTypesToMonitor(java.lang.Class)
	 */
	@Override
	public Set<Class<? extends OpenmrsObject>> getAssociationTypesToMonitor(Class<? extends OpenmrsObject> clazz) {
		return getAssociationTypesToMonitorInternal(clazz, null);
	}
	
	@Override
	public MonitoringStrategy getMonitoringStrategy() {
		if (monitoringStrategyCache == null) {
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.GP_MONITORING_STRATEGY);
			if (gp != null) {
				if (StringUtils.isNotBlank(gp.getPropertyValue())) {
					monitoringStrategyCache = MonitoringStrategy.valueOf(gp.getPropertyValue().trim());
				}
			}
		}
		
		//default
		if (monitoringStrategyCache == null) {
			monitoringStrategyCache = MonitoringStrategy.NONE;
		}
		return monitoringStrategyCache;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getMonitoredClasses()
	 */
	@Override
	public Set<Class<? extends OpenmrsObject>> getMonitoredClasses() {
		if (monitoredTypeCache == null) {
			monitoredTypeCache = new HashSet<Class<? extends OpenmrsObject>>();
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.GP_MONITORED_CLASSES);
			if (gp != null && StringUtils.isNotBlank(gp.getPropertyValue())) {
				String[] classnameArray = StringUtils.split(gp.getPropertyValue(), ",");
				for (String classname : classnameArray) {
					classname = classname.trim();
					try {
						Class<? extends OpenmrsObject> monitoredClass = (Class<? extends OpenmrsObject>) Context
						        .loadClass(classname);
						monitoredTypeCache.add(monitoredClass);
						Set<Class<? extends OpenmrsObject>> subclasses = getPersistentConcreteSubclasses(monitoredClass);
						for (Class<? extends OpenmrsObject> subclass : subclasses) {
							monitoredTypeCache.add(subclass);
						}
					}
					catch (ClassNotFoundException e) {
						log.error("Failed to load class:" + classname);
					}
				}
			}
			
			//in case implicit classes cache was already created, update it
			if (implicitlyMonitoredTypeCache != null) {
				implicitlyMonitoredTypeCache.removeAll(monitoredTypeCache);
			}
		}
		
		return monitoredTypeCache;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getUnMonitoredClasses()
	 */
	@Override
	public Set<Class<? extends OpenmrsObject>> getUnMonitoredClasses() {
		if (unMonitoredTypeCache == null) {
			unMonitoredTypeCache = new HashSet<Class<? extends OpenmrsObject>>();
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    AuditLogConstants.GP_UN_MONITORED_CLASSES);
			if (gp != null && StringUtils.isNotBlank(gp.getPropertyValue())) {
				String[] classnameArray = StringUtils.split(gp.getPropertyValue(), ",");
				for (String classname : classnameArray) {
					classname = classname.trim();
					try {
						Class<? extends OpenmrsObject> unMonitoredClass = (Class<? extends OpenmrsObject>) Context
						        .loadClass(classname);
						unMonitoredTypeCache.add(unMonitoredClass);
						Set<Class<? extends OpenmrsObject>> subclasses = getPersistentConcreteSubclasses(unMonitoredClass);
						for (Class<? extends OpenmrsObject> subclass : subclasses) {
							unMonitoredTypeCache.add(subclass);
						}
					}
					catch (ClassNotFoundException e) {
						log.error("Failed to load class:" + classname);
					}
				}
			}
		}
		
		return unMonitoredTypeCache;
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#startMonitoring(java.util.Set)
	 */
	@Override
	public void startMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		if (getMonitoringStrategy() == MonitoringStrategy.NONE_EXCEPT
		        || getMonitoringStrategy() == MonitoringStrategy.ALL_EXCEPT) {
			updateGlobalProperty(clazzes, true);
		}
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#stopMonitoring(java.util.Set)
	 */
	@Override
	public void stopMonitoring(Set<Class<? extends OpenmrsObject>> clazzes) {
		if (getMonitoringStrategy() == MonitoringStrategy.NONE_EXCEPT
		        || getMonitoringStrategy() == MonitoringStrategy.ALL_EXCEPT) {
			updateGlobalProperty(clazzes, false);
		}
	}
	
	/**
	 * @see org.openmrs.module.auditlog.api.db.AuditLogDAO#getImplicitlyMonitoredClasses()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<Class<? extends OpenmrsObject>> getImplicitlyMonitoredClasses() {
		if (implicitlyMonitoredTypeCache == null) {
			implicitlyMonitoredTypeCache = new HashSet<Class<? extends OpenmrsObject>>();
			if (getMonitoringStrategy() == MonitoringStrategy.NONE_EXCEPT) {
				for (Class<? extends OpenmrsObject> monitoredClass : getMonitoredClasses()) {
					addAssociationTypes(monitoredClass);
					Set<Class<? extends OpenmrsObject>> subclasses = getPersistentConcreteSubclasses(monitoredClass);
					for (Class<? extends OpenmrsObject> subclass : subclasses) {
						addAssociationTypes(subclass);
					}
				}
			} else if (getMonitoringStrategy() == MonitoringStrategy.ALL_EXCEPT && getUnMonitoredClasses().size() > 0) {
				//generate implicitly monitored classes so we can track them. The reason behind 
				//this is: Say Concept is marked as monitored and strategy is set to All Except
				//and say ConceptName is for some reason marked as un monitored we should still monitor
				//concept names otherwise it poses inconsistencies
				Collection<ClassMetadata> allClassMetadata = sessionFactory.getAllClassMetadata().values();
				for (ClassMetadata classMetadata : allClassMetadata) {
					Class<?> mappedClass = classMetadata.getMappedClass(EntityMode.POJO);
					if (OpenmrsObject.class.isAssignableFrom(mappedClass)) {
						addAssociationTypes((Class<? extends OpenmrsObject>) mappedClass);
					}
				}
			}
		}
		
		return implicitlyMonitoredTypeCache;
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyChanged(org.openmrs.GlobalProperty)
	 */
	@Override
	public void globalPropertyChanged(GlobalProperty gp) {
		if (AuditLogConstants.GP_MONITORED_CLASSES.equals(gp.getProperty())) {
			monitoredTypeCache = null;
		} else if (AuditLogConstants.GP_UN_MONITORED_CLASSES.equals(gp.getProperty())) {
			unMonitoredTypeCache = null;
		} else {
			//we need to invalidate all caches when the strategy is changed
			monitoringStrategyCache = null;
			monitoredTypeCache = null;
			unMonitoredTypeCache = null;
		}
		implicitlyMonitoredTypeCache = null;
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyDeleted(java.lang.String)
	 */
	@Override
	public void globalPropertyDeleted(String gpName) {
		if (AuditLogConstants.GP_MONITORED_CLASSES.equals(gpName)) {
			monitoredTypeCache = null;
		} else if (AuditLogConstants.GP_UN_MONITORED_CLASSES.equals(gpName)) {
			unMonitoredTypeCache = null;
		} else {
			monitoringStrategyCache = null;
			monitoredTypeCache = null;
			unMonitoredTypeCache = null;
		}
		implicitlyMonitoredTypeCache = null;
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
	 * Finds all the types for associations to monitor in as recursive way i.e if a Persistent type
	 * is found, then we also find its collection element types and types for fields mapped as one
	 * to one, note that this only includes sub types of {@link OpenmrsObject} and that this method
	 * is recursive
	 * 
	 * @param clazz the Class to match against
	 * @param foundAssocTypes the found association types
	 * @return a set of found class names
	 */
	private Set<Class<? extends OpenmrsObject>> getAssociationTypesToMonitorInternal(Class<? extends OpenmrsObject> clazz,
	                                                                                 Set<Class<? extends OpenmrsObject>> foundAssocTypes) {
		if (foundAssocTypes == null) {
			foundAssocTypes = new HashSet<Class<? extends OpenmrsObject>>();
		}
		
		ClassMetadata cmd = sessionFactory.getClassMetadata(clazz);
		if (cmd != null) {
			for (Type type : cmd.getPropertyTypes()) {
				//If this is a OneToOne or a collection type
				if (type.isCollectionType() || OneToOneType.class.isAssignableFrom(type.getClass())) {
					Class<? extends OpenmrsObject> assocType = type.getReturnedClass();
					if (type.isCollectionType()) {
						assocType = ((CollectionType) type).getElementType((SessionFactoryImplementor) sessionFactory)
						        .getReturnedClass();
					}
					if (!foundAssocTypes.contains(assocType)) {
						foundAssocTypes.add(assocType);
						foundAssocTypes.addAll(getAssociationTypesToMonitorInternal(assocType, foundAssocTypes));
					}
				}
			}
		}
		return foundAssocTypes;
	}
	
	/**
	 * Update the value of the {@link GlobalProperty} {@link AuditLogConstants#GP_MONITORED_CLASSES}
	 * in the database
	 * 
	 * @param clazzes the classes to add or remove
	 * @param startMonitoring specifies if the the classes are getting added to removed
	 */
	private void updateGlobalProperty(Set<Class<? extends OpenmrsObject>> clazzes, boolean startMonitoring) {
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
				if (startMonitoring) {
					getMonitoredClasses().add(clazz);
				} else {
					getMonitoredClasses().remove(clazz);
					//remove subclasses too
					Set<Class<? extends OpenmrsObject>> subclasses = getPersistentConcreteSubclasses(clazz);
					for (Class<? extends OpenmrsObject> subclass : subclasses) {
						getMonitoredClasses().remove(subclass);
					}
				}
			}
			
			gp.setPropertyValue(StringUtils.join(AuditLogUtil.getAsListOfClassnames(getMonitoredClasses()), ","));
		} else {
			for (Class<? extends OpenmrsObject> clazz : clazzes) {
				if (startMonitoring) {
					getUnMonitoredClasses().remove(clazz);
					Set<Class<? extends OpenmrsObject>> subclasses = getPersistentConcreteSubclasses(clazz);
					for (Class<? extends OpenmrsObject> subclass : subclasses) {
						getUnMonitoredClasses().remove(subclass);
					}
				} else {
					getUnMonitoredClasses().add(clazz);
				}
			}
			
			gp.setPropertyValue(StringUtils.join(AuditLogUtil.getAsListOfClassnames(getUnMonitoredClasses()), ","));
		}
		
		try {
			as.saveGlobalProperty(gp);
		}
		catch (Exception e) {
			//The cache needs to be rebuilt since we already updated the 
			//cached above but the GP value didn't get updated in the DB
			if (isNoneExceptStrategy) {
				monitoredTypeCache = null;
			} else {
				unMonitoredTypeCache = null;
			}
			implicitlyMonitoredTypeCache = null;
			
			throw new APIException("Failed to " + ((startMonitoring) ? "start" : "stop") + " monitoring " + clazzes, e);
		}
	}
	
	/**
	 * Gets a set of concrete subclasses for the specified class recursively, note that interfaces
	 * and abstract classes are excluded
	 * 
	 * @param clazz the Super Class
	 * @param foundSubclasses the list of subclasses found in previous recursive calls, should be
	 *            null for the first call
	 * @param mappedClasses the ClassMetadata Collection
	 * @return a set of subclasses
	 * @should return a list of subclasses for the specified type
	 * @should exclude interfaces and abstract classes
	 */
	@SuppressWarnings("unchecked")
	private Set<Class<? extends OpenmrsObject>> getPersistentConcreteSubclassesInternal(Class<? extends OpenmrsObject> clazz,
	                                                                                    Set<Class<? extends OpenmrsObject>> foundSubclasses,
	                                                                                    Collection<ClassMetadata> mappedClasses) {
		if (foundSubclasses == null) {
			foundSubclasses = new HashSet<Class<? extends OpenmrsObject>>();
		}
		if (mappedClasses == null) {
			mappedClasses = sessionFactory.getAllClassMetadata().values();
		}
		
		if (clazz != null) {
			for (ClassMetadata cmd : mappedClasses) {
				Class<? extends OpenmrsObject> possibleSubclass = cmd.getMappedClass(EntityMode.POJO);
				if (!clazz.equals(possibleSubclass) && clazz.isAssignableFrom(possibleSubclass)) {
					if (!Modifier.isAbstract(possibleSubclass.getModifiers()) && !possibleSubclass.isInterface()) {
						foundSubclasses.add(possibleSubclass);
					}
					foundSubclasses.addAll(getPersistentConcreteSubclassesInternal(possibleSubclass, foundSubclasses,
					    mappedClasses));
				}
			}
		}
		
		return foundSubclasses;
	}
	
	/**
	 * @param clazz the class whose association types to add
	 */
	private void addAssociationTypes(Class<? extends OpenmrsObject> clazz) {
		for (Class<? extends OpenmrsObject> assocType : getAssociationTypesToMonitor(clazz)) {
			//If this type is not explicitly marked as monitored
			if (!getMonitoredClasses().contains(assocType)) {
				getImplicitlyMonitoredClasses().add(assocType);
			}
		}
	}
}
