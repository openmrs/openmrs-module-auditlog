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
package org.openmrs.module.auditlog.api.db;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
import org.openmrs.api.context.Context;

public class DAOUtils {
	
	/**
	 * Finds all the types for associations to audit in as recursive way i.e if a Persistent type is
	 * found, then we also find its collection element types and types for fields mapped as one to
	 * one.
	 * 
	 * @param clazz the Class to match against
	 * @return a set of found class names
	 */
	public static Set<Class<?>> getAssociationTypesToAudit(Class<?> clazz) {
		return getAssociationTypesToAuditInternal(clazz, null);
	}
	
	/**
	 * Finds all the types for associations to audit in as recursive way i.e if a Persistent type is
	 * found, then we also find its collection element types and types for fields mapped as one to
	 * one.
	 * 
	 * @param clazz the Class to match against
	 * @param foundAssocTypes the found association types
	 * @return a set of found class names
	 */
	private static Set<Class<?>> getAssociationTypesToAuditInternal(Class<?> clazz, Set<Class<?>> foundAssocTypes) {
		if (foundAssocTypes == null) {
			foundAssocTypes = new HashSet<Class<?>>();
		}
		
		ClassMetadata cmd = getSessionFactory().getClassMetadata(clazz);
		if (cmd != null) {
			for (Type type : cmd.getPropertyTypes()) {
				//If this is a OneToOne or a collection type
				if (type.isCollectionType() || OneToOneType.class.isAssignableFrom(type.getClass())) {
					CollectionType collType = (CollectionType) type;
					boolean isManyToManyColl = false;
					if (collType.isCollectionType()) {
						collType = (CollectionType) type;
						isManyToManyColl = ((SessionFactoryImplementor) getSessionFactory()).getCollectionPersister(
						    collType.getRole()).isManyToMany();
					}
					Class<?> assocType = type.getReturnedClass();
					if (type.isCollectionType()) {
						assocType = collType.getElementType((SessionFactoryImplementor) getSessionFactory())
						        .getReturnedClass();
					}
					
					//Ignore non persistent types
					if (getSessionFactory().getClassMetadata(assocType) == null) {
						continue;
					}
					
					if (!foundAssocTypes.contains(assocType)) {
						//Don't implicitly audit types for many to many collections items
						if (!type.isCollectionType() || (type.isCollectionType() && !isManyToManyColl)) {
							foundAssocTypes.add(assocType);
							//Recursively inspect each association type
							foundAssocTypes.addAll(getAssociationTypesToAuditInternal(assocType, foundAssocTypes));
						}
					}
				}
			}
		}
		return foundAssocTypes;
	}
	
	/**
	 * Gets a set of concrete subclasses for the specified class recursively, note that interfaces
	 * and abstract classes are excluded
	 * 
	 * @param clazz the Super Class
	 * @return a set of subclasses
	 * @should return a list of subclasses for the specified type
	 * @should exclude interfaces and abstract classes
	 */
	public static Set<Class<?>> getPersistentConcreteSubclasses(Class<?> clazz) {
		return getPersistentConcreteSubclassesInternal(clazz, null, null);
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
	 */
	@SuppressWarnings("unchecked")
	private static Set<Class<?>> getPersistentConcreteSubclassesInternal(Class<?> clazz, Set<Class<?>> foundSubclasses,
																		 Collection<EntityPersister> mappedClasses) {
		if (foundSubclasses == null) {
			foundSubclasses = new HashSet<>();
		}

		// Initialize mappedClasses if null by retrieving all EntityPersisters
		if (mappedClasses == null) {
			SessionFactoryImplementor sessionFactoryImpl = (SessionFactoryImplementor) getSessionFactory();
			Map<String, EntityPersister> entityPersisters = sessionFactoryImpl.getMetamodel().entityPersisters();
			mappedClasses = entityPersisters.values();
		}

		if (clazz != null) {
			for (EntityPersister persister : mappedClasses) {
				Class<?> possibleSubclass = persister.getMappedClass();

				// Check if the class is a subclass of the given class
				if (possibleSubclass != null && !clazz.equals(possibleSubclass) && clazz.isAssignableFrom(possibleSubclass)) {
					// Check if the subclass is concrete (not abstract and not an interface)
					if (!Modifier.isAbstract(possibleSubclass.getModifiers()) && !possibleSubclass.isInterface()) {
						foundSubclasses.add(possibleSubclass);
					}
					// Recursively find subclasses
					foundSubclasses.addAll(getPersistentConcreteSubclassesInternal(possibleSubclass, foundSubclasses, mappedClasses));
				}
			}
		}

		return foundSubclasses;
	}
	
	public static ClassMetadata getClassMetadata(Class<?> clazz) {
		return getSessionFactory().getClassMetadata(clazz);
	}
	
	public static SessionFactory getSessionFactory() {
		return Context.getRegisteredComponents(SessionFactory.class).get(0);
	}
}
