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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.openmrs.GlobalProperty;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.api.db.DAOUtils;
import org.openmrs.module.auditlog.strategy.AllAuditStrategy;
import org.openmrs.module.auditlog.strategy.AllExceptAuditStrategy;
import org.openmrs.module.auditlog.strategy.AuditStrategy;
import org.openmrs.module.auditlog.strategy.ExceptionBasedAuditStrategy;
import org.openmrs.module.auditlog.strategy.NoneAuditStrategy;
import org.openmrs.module.auditlog.strategy.NoneExceptAuditStrategy;
import org.openmrs.module.auditlog.util.AuditLogConstants;
import org.openmrs.module.auditlog.util.AuditLogUtil;
import org.springframework.stereotype.Component;

@Component("auditLogHelper")
public class AuditLogHelper implements GlobalPropertyListener {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	public static final List<Class<?>> CORE_EXCEPTIONS;
	static {
		CORE_EXCEPTIONS = new ArrayList<Class<?>>();
		CORE_EXCEPTIONS.add(AuditLog.class);
	}
	
	private static Set<Class<?>> exceptionsTypeCache;
	
	private static AuditStrategy auditingStrategyCache;
	
	private static Set<Class<?>> implicitlyAuditedTypeCache;
	
	public AuditStrategy getAuditingStrategy() {
		if (auditingStrategyCache == null) {
			String gpValue = Context.getAdministrationService().getGlobalProperty(AuditLogConstants.GP_AUDITING_STRATEGY);
			if (StringUtils.isBlank(gpValue)) {
				//Defaults to none, we can't cache this so sorry but we will have to hit the DB
				//for the GP value until it gets set so that we only cache a set value
				return AuditStrategy.NONE;
			} else {
				//We should allow short values like all, all_except, none, none_except
				if (AuditStrategy.SHORT_NAME_NONE.equalsIgnoreCase(gpValue)) {
					auditingStrategyCache = AuditStrategy.NONE;
				} else if (AuditStrategy.SHORT_NAME_NONE_EXCEPT.equalsIgnoreCase(gpValue)) {
					auditingStrategyCache = AuditStrategy.NONE_EXCEPT;
				} else if (AuditStrategy.SHORT_NAME_ALL.equalsIgnoreCase(gpValue)) {
					auditingStrategyCache = AuditStrategy.ALL;
				} else if (AuditStrategy.SHORT_NAME_ALL_EXCEPT.equalsIgnoreCase(gpValue)) {
					auditingStrategyCache = AuditStrategy.ALL_EXCEPT;
				}
			}
			
			if (auditingStrategyCache == null) {
				try {
					Class<AuditStrategy> clazz = (Class<AuditStrategy>) Context.loadClass(gpValue);
					if (NoneAuditStrategy.class.equals(clazz)) {
						auditingStrategyCache = AuditStrategy.NONE;
					} else if (NoneExceptAuditStrategy.class.equals(clazz)) {
						auditingStrategyCache = AuditStrategy.NONE_EXCEPT;
					} else if (AllAuditStrategy.class.equals(clazz)) {
						auditingStrategyCache = AuditStrategy.ALL;
					} else if (AllExceptAuditStrategy.class.equals(clazz)) {
						auditingStrategyCache = AuditStrategy.ALL_EXCEPT;
					} else {
						auditingStrategyCache = clazz.newInstance();
					}
				}
				catch (Exception e) {
					throw new APIException("Failed to set the audit strategy", e);
				}
			}
		}
		
		return auditingStrategyCache;
	}
	
	public boolean isAudited(Class<?> clazz) {
		//We need to stop hibernate auto flushing which might happen as we fetch
		//the GP values, Otherwise if a flush happens, then the interceptor
		//logic will be called again which will result in an infinite loop/stack overflow
		if (exceptionsTypeCache == null || auditingStrategyCache == null) {
			SessionFactory sf = DAOUtils.getSessionFactory();
			FlushMode originalFlushMode = sf.getCurrentSession().getFlushMode();
			sf.getCurrentSession().setFlushMode(FlushMode.MANUAL);
			try {
				return isAuditedInternal(clazz);
			}
			finally {
				//reset
				sf.getCurrentSession().setFlushMode(originalFlushMode);
			}
		}
		
		return isAuditedInternal(clazz);
	}
	
	/**
	 * Checks if the specified type is implicitly audit
	 * 
	 * @should return true if a class is implicitly audited
	 * @should return false if a class is not implicitly marked as audited
	 * @should return false if a class is already explicitly marked already as audited
	 * @should return true if a class is implicitly audited and strategy is all except
	 * @should return false if a class is not implicitly audited and strategy is all except
	 * @should return false if a class is already explicitly audited and strategy is all except
	 */
	public boolean isImplicitlyAudited(Class<?> clazz) {
		//We need to stop hibernate auto flushing which might happen as we fetch
		//the GP values, Otherwise if a flush happens, then the interceptor
		//logic will be called again which will result in an infinite loop/stack overflow
		if (implicitlyAuditedTypeCache == null) {
			SessionFactory sf = DAOUtils.getSessionFactory();
			FlushMode originalFlushMode = sf.getCurrentSession().getFlushMode();
			sf.getCurrentSession().setFlushMode(FlushMode.MANUAL);
			try {
				return isImplicitlyAuditedInternal(clazz);
			}
			finally {
				//reset
				sf.getCurrentSession().setFlushMode(originalFlushMode);
			}
		}
		
		return isImplicitlyAuditedInternal(clazz);
	}
	
	/**
	 * Gets implicitly audited classes, this are generated as a result of their owning entity types
	 * being marked as audited if they are not explicitly marked as audited themselves, i.e if
	 * Concept is marked as audited, then ConceptName, ConceptDescription, ConceptMapping etc
	 * implicitly get marked as audited
	 * 
	 * @return a set of implicitly audited classes
	 * @should return a set of implicitly audited classes for none except strategy
	 * @should return a set of implicitly audited classes for all except strategy
	 * @should return an empty set for none strategy
	 * @should return an empty set for all strategy
	 */
	public Set<Class<?>> getImplicitlyAuditedClasses() {
		if (implicitlyAuditedTypeCache == null) {
			implicitlyAuditedTypeCache = new HashSet<Class<?>>();
			if (getAuditingStrategy().equals(AuditStrategy.NONE_EXCEPT)) {
				for (Class<?> auditedClass : getExceptions()) {
					if (!AuditLogHelper.CORE_EXCEPTIONS.contains(auditedClass)) {
						addAssociationTypes(auditedClass);
					}
				}
			} else if (getAuditingStrategy().equals(AuditStrategy.ALL_EXCEPT) && getExceptions().size() > 0) {
				//generate implicitly audited classes so we can track them. The reason behind
				//this is: Say Concept is marked as audited and strategy is set to All Except
				//and say ConceptName is for some reason marked as un audited we should still audit
				//concept names otherwise it poses inconsistencies
				Collection<ClassMetadata> allClassMetadata = DAOUtils.getSessionFactory().getAllClassMetadata().values();
				for (ClassMetadata classMetadata : allClassMetadata) {
					Class<?> mappedClass = classMetadata.getMappedClass(EntityMode.POJO);
					if (!getExceptions().contains(mappedClass)) {
						if (!AuditLogHelper.CORE_EXCEPTIONS.contains(mappedClass)) {
							addAssociationTypes(mappedClass);
						}
					}
				}
			}
		}
		
		return implicitlyAuditedTypeCache;
	}
	
	/**
	 * Returns a set of exception classes as specified by the {@link org.openmrs.GlobalProperty}
	 * GLOBAL_PROPERTY_EXCEPTION
	 * 
	 * @return a set of audited classes
	 * @should return a set of exception classes
	 * @should fail for non exception based audit strategies
	 */
	public Set<Class<?>> getExceptions() {
		if (!(getAuditingStrategy() instanceof ExceptionBasedAuditStrategy)) {
			throw new APIException("Not supported by the configured audit strategy");
		}
		
		if (exceptionsTypeCache == null) {
			exceptionsTypeCache = new HashSet<Class<?>>();
			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(
			    ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION);
			
			if (gp != null && StringUtils.isNotBlank(gp.getPropertyValue())) {
				String[] classnameArray = StringUtils.split(gp.getPropertyValue(), ",");
				for (String classname : classnameArray) {
					classname = classname.trim();
					try {
						Class<?> auditedClass = Context.loadClass(classname);
						exceptionsTypeCache.add(auditedClass);
						
						Set<Class<?>> subclasses = DAOUtils.getPersistentConcreteSubclasses(auditedClass);
						for (Class<?> subclass : subclasses) {
							exceptionsTypeCache.add(subclass);
						}
					}
					catch (ClassNotFoundException e) {
						log.error("Failed to load class:" + classname);
					}
				}
			}
		}
		
		return exceptionsTypeCache;
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#supportsPropertyName(java.lang.String)
	 */
	@Override
	public boolean supportsPropertyName(String gpName) {
		return AuditLogConstants.GP_AUDITING_STRATEGY.equals(gpName)
		        || ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION.equals(gpName);
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyChanged(org.openmrs.GlobalProperty)
	 */
	@Override
	public void globalPropertyChanged(GlobalProperty gp) {
		auditingStrategyCache = null;
		implicitlyAuditedTypeCache = null;
		exceptionsTypeCache = null;
		if (AuditLogConstants.GP_AUDITING_STRATEGY.equals(gp.getProperty())) {
			AuditLogUtil.setGlobalProperty(ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION, "");
		}
	}
	
	/**
	 * @see org.openmrs.api.GlobalPropertyListener#globalPropertyDeleted(java.lang.String)
	 */
	@Override
	public void globalPropertyDeleted(String gpName) {
		auditingStrategyCache = null;
		implicitlyAuditedTypeCache = null;
		exceptionsTypeCache = null;
		if (AuditLogConstants.GP_AUDITING_STRATEGY.equals(gpName)) {
			AuditLogUtil.setGlobalProperty(ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION, "");
		}
	}
	
	public void stopAuditing(Set<Class<?>> clazzes) {
		if (getAuditingStrategy().equals(AuditStrategy.NONE) || getAuditingStrategy().equals(AuditStrategy.ALL)) {
			throw new APIException("Can't call AuditLogService.stopAuditing when the Audit strategy is set to "
			        + AuditStrategy.NONE + " or " + AuditStrategy.ALL);
		}
		
		updateGlobalProperty(clazzes, false);
	}
	
	public void updateGlobalProperty(Set<Class<?>> clazzes, boolean startAuditing) {
		boolean isNoneExceptStrategy = getAuditingStrategy().equals(AuditStrategy.NONE_EXCEPT);
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION);
		if (gp == null) {
			String description = "Specifies the class names of objects to audit or not depending on the auditing strategy";
			gp = new GlobalProperty(ExceptionBasedAuditStrategy.GLOBAL_PROPERTY_EXCEPTION, null, description);
		}
		
		if (isNoneExceptStrategy) {
			for (Class<?> clazz : clazzes) {
				if (startAuditing) {
					getExceptions().add(clazz);
				} else {
					getExceptions().remove(clazz);
					//remove subclasses too
					Set<Class<?>> subclasses = DAOUtils.getPersistentConcreteSubclasses(clazz);
					for (Class<?> subclass : subclasses) {
						getExceptions().remove(subclass);
					}
				}
			}
		} else {
			for (Class<?> clazz : clazzes) {
				if (startAuditing) {
					getExceptions().remove(clazz);
					Set<Class<?>> subclasses = DAOUtils.getPersistentConcreteSubclasses(clazz);
					for (Class<?> subclass : subclasses) {
						getExceptions().remove(subclass);
					}
				} else {
					getExceptions().add(clazz);
				}
			}
		}
		
		gp.setPropertyValue(StringUtils.join(AuditLogUtil.getAsListOfClassnames(getExceptions()), ","));
		
		try {
			as.saveGlobalProperty(gp);
		}
		catch (Exception e) {
			//The cache needs to be rebuilt since we already updated the 
			//cached above but the GP value didn't get updated in the DB
			exceptionsTypeCache = null;
			implicitlyAuditedTypeCache = null;
			
			throw new APIException("Failed to " + ((startAuditing) ? "start" : "stop") + " auditing " + clazzes, e);
		}
	}
	
	/**
	 * Checks if specified object is among the ones that are audited
	 * 
	 * @param clazz the class to check against
	 * @return true if it is audited otherwise false
	 */
	private boolean isAuditedInternal(Class<?> clazz) {
		if (CORE_EXCEPTIONS.contains(clazz)) {
			return false;
		}
		if (getAuditingStrategy() == null || getAuditingStrategy().equals(AuditStrategy.NONE)) {
			return false;
		}
		if (getAuditingStrategy().equals(AuditStrategy.ALL)) {
			return true;
		}
		
		if (getAuditingStrategy().equals(AuditStrategy.NONE_EXCEPT)) {
			return getExceptions().contains(clazz);
		}
		//Strategy is ALL_EXCEPT or NONE_EXCEPT
		return !getExceptions().contains(clazz);
	}
	
	/**
	 * @param clazz the class whose association types to add
	 */
	private void addAssociationTypes(Class<?> clazz) {
		for (Class<?> assocType : DAOUtils.getAssociationTypesToAudit(clazz)) {
			//If this type is not explicitly marked as audited
			if (!isAudited(assocType)) {
				if (implicitlyAuditedTypeCache == null) {
					implicitlyAuditedTypeCache = new HashSet<Class<?>>();
				}
				implicitlyAuditedTypeCache.add(assocType);
			}
		}
	}
	
	/**
	 * Checks if specified object is among the ones that are implicitly audited
	 * 
	 * @param clazz the class to check against
	 * @return true if it is implicitly audited otherwise false
	 */
	private boolean isImplicitlyAuditedInternal(Class<?> clazz) {
		if (CORE_EXCEPTIONS.contains(clazz)) {
			return false;
		}
		if (getAuditingStrategy() == null || getAuditingStrategy().equals(AuditStrategy.NONE)) {
			return false;
		}
		
		return getImplicitlyAuditedClasses().contains(clazz);
	}
}
