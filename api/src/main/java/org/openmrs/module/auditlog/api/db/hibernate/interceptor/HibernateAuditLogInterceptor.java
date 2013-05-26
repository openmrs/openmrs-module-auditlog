package org.openmrs.module.auditlog.api.db.hibernate.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.Type;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.util.OpenmrsUtil;

/**
 * A hibernate {@link Interceptor} implementation, intercepts any database inserts, updates and
 * deletes and creates audit log entries for Monitored Objects, it logs changes for a single session
 * meaning that if User A and B concurrently make changes to the same object, there will be 2 log
 * entries in the DB, one for each user's session. Any changes/inserts/deletes made to the DB that
 * are not made through the application won't be detected by the module.
 */
public class HibernateAuditLogInterceptor extends EmptyInterceptor {
	
	private static final long serialVersionUID = 1L;
	
	private static final Log log = LogFactory.getLog(HibernateAuditLogInterceptor.class);
	
	private ThreadLocal<HashSet<OpenmrsObject>> inserts = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> updates = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> deletes = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	//Mapping between object uuids and maps of their changed property names and their older values, 
	//the first item in the array is the old value while the the second is the new value
	private ThreadLocal<Map<String, Map<String, String[]>>> objectChangesMap = new ThreadLocal<Map<String, Map<String, String[]>>>();
	
	//Mapping between entities and lists of their Collections in the current session
	private ThreadLocal<Map<Object, List<Collection<?>>>> entityCollectionsMap = new ThreadLocal<Map<Object, List<Collection<?>>>>();
	
	//Mapping between parent entity uuids and lists of AuditLogs for their collection elements
	private ThreadLocal<Map<String, List<AuditLog>>> ownerUuidChildLogsMap = new ThreadLocal<Map<String, List<AuditLog>>>();
	
	//Mapping between collection element uuids and their AuditLogs, will use 
	//this to avoid creating logs for collections elements multiple times
	private ThreadLocal<Map<String, AuditLog>> childbjectUuidAuditLogMap = new ThreadLocal<Map<String, AuditLog>>();
	
	//Mapping between parent entities and sets of removed collection elements
	private ThreadLocal<Map<OpenmrsObject, HashSet<OpenmrsObject>>> entityRemovedChildrenMap = new ThreadLocal<Map<OpenmrsObject, HashSet<OpenmrsObject>>>();
	
	private ThreadLocal<Date> date = new ThreadLocal<Date>();
	
	private AuditLogDAO auditLogDao;
	
	//Ignore these properties because they match auditLog.user and auditLog.dateCreated
	private static final String[] IGNORED_PROPERTIES = new String[] { "changedBy", "dateChanged", "creator", "dateCreated",
	        "voidedBy", "dateVoided", "retiredBy", "dateRetired", "personChangedBy", "personDateChanged" };
	
	/**
	 * @return the dao
	 */
	public AuditLogDAO getAuditLogDao() {
		if (auditLogDao == null)
			auditLogDao = Context.getRegisteredComponents(AuditLogDAO.class).get(0);
		
		return auditLogDao;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#afterTransactionBegin(org.hibernate.Transaction)
	 */
	@Override
	public void afterTransactionBegin(Transaction tx) {
		inserts.set(new HashSet<OpenmrsObject>());
		updates.set(new HashSet<OpenmrsObject>());
		deletes.set(new HashSet<OpenmrsObject>());
		objectChangesMap.set(new HashMap<String, Map<String, String[]>>());
		entityCollectionsMap.set(new HashMap<Object, List<Collection<?>>>());
		ownerUuidChildLogsMap.set(new HashMap<String, List<AuditLog>>());
		childbjectUuidAuditLogMap.set(new HashMap<String, AuditLog>());
		entityRemovedChildrenMap.set(new HashMap<OpenmrsObject, HashSet<OpenmrsObject>>());
		date.set(new Date());
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onSave(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (isAuditable(entity)) {
			OpenmrsObject openmrsObject = (OpenmrsObject) entity;
			if (log.isDebugEnabled())
				log.debug("Creating log entry for created object with uuid:" + openmrsObject.getUuid() + " of type:"
				        + entity.getClass().getName());
			
			inserts.get().add(openmrsObject);
		}
		
		return false;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onFlushDirty(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
	                            String[] propertyNames, Type[] types) {
		
		if (propertyNames != null && isAuditable(entity)) {
			OpenmrsObject openmrsObject = (OpenmrsObject) entity;
			Map<String, String[]> propertyChangesMap = null;//Map<propertyName, Object[]{currentValue, PreviousValue}>
			for (int i = 0; i < propertyNames.length; i++) {
				//we need to ignore dateChanged and changedBy fields in any case they
				//are actually part of the Auditlog in form of user and dateCreated
				if (ArrayUtils.contains(IGNORED_PROPERTIES, propertyNames[i]))
					continue;
				
				Object previousValue = (previousState != null) ? previousState[i] : null;
				Object currentValue = (currentState != null) ? currentState[i] : null;
				if (!types[i].isCollectionType() && !OpenmrsUtil.nullSafeEquals(currentValue, previousValue)) {
					//For string properties, ignore changes from null to blank and vice versa
					//TODO This should be user configurable via a module GP
					if (StringType.class.getName().equals(types[i].getClass().getName())
					        || TextType.class.getName().equals(types[i].getClass().getName())) {
						String currentStateString = null;
						if (currentValue != null && !StringUtils.isBlank(currentValue.toString()))
							currentStateString = currentValue.toString();
						
						String previousValueString = null;
						if (previousValue != null && !StringUtils.isBlank(previousValue.toString()))
							previousValueString = previousValue.toString();
						
						//TODO Case sensibility here should be configurable via a GP by admin
						if (OpenmrsUtil.nullSafeEqualsIgnoreCase(previousValueString, currentStateString))
							continue;
					}
					
					if (propertyChangesMap == null)
						propertyChangesMap = new HashMap<String, String[]>();
					
					String serializedPreviousValue = InterceptorUtil.serializeValue(previousValue, getAuditLogDao());
					String serializedCurrentValue = InterceptorUtil.serializeValue(currentValue, getAuditLogDao());
					
					propertyChangesMap.put(propertyNames[i],
					    new String[] { serializedCurrentValue, serializedPreviousValue });
				}
			}
			
			if (MapUtils.isNotEmpty(propertyChangesMap)) {
				if (log.isDebugEnabled())
					log.debug("Creating log entry for updated object with uuid:" + openmrsObject.getUuid() + " of type:"
					        + entity.getClass().getName());
				
				updates.get().add(openmrsObject);
				objectChangesMap.get().put(openmrsObject.getUuid(), propertyChangesMap);
			}
		}
		
		return false;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onDelete(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (isAuditable(entity)) {
			OpenmrsObject openmrsObject = (OpenmrsObject) entity;
			if (log.isDebugEnabled())
				log.debug("Creating log entry for deleted object with uuid:" + openmrsObject.getUuid() + " of type:"
				        + entity.getClass().getName());
			
			deletes.get().add(openmrsObject);
		}
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onCollectionUpdate(java.lang.Object,
	 *      java.io.Serializable)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		if (collection != null) {
			PersistentCollection persistentColl = ((PersistentCollection) collection);
			if (isAuditable(persistentColl.getOwner())) {
				OpenmrsObject owningObject = (OpenmrsObject) persistentColl.getOwner();
				updates.get().add(owningObject);
				Map previousStoredSnapshotMap = (Map) persistentColl.getStoredSnapshot();
				String ownerUuid = owningObject.getUuid();
				String propertyName = persistentColl.getRole().substring(persistentColl.getRole().lastIndexOf('.') + 1);
				
				if (objectChangesMap.get().get(ownerUuid) == null) {
					objectChangesMap.get().put(ownerUuid, new HashMap<String, String[]>());
				}
				
				String previousSerializedItems = null;
				String newSerializedItems = null;
				if (Collection.class.isAssignableFrom(collection.getClass())) {
					Collection currentColl = (Collection) collection;
					previousSerializedItems = InterceptorUtil.serializeCollection(previousStoredSnapshotMap.values(),
					    getAuditLogDao());
					newSerializedItems = InterceptorUtil.serializeCollection(currentColl, getAuditLogDao());
					
					//Track removed items so that when we create logs for them,
					//and link them to the parent's log
					Set<Object> removedItems = new HashSet<Object>();
					removedItems.addAll(CollectionUtils.subtract(previousStoredSnapshotMap.values(), currentColl));
					if (!removedItems.isEmpty()) {
						Class<?> elementClass = removedItems.iterator().next().getClass();
						if (OpenmrsObject.class.isAssignableFrom(elementClass)) {
							if (entityRemovedChildrenMap.get().get(owningObject) == null) {
								entityRemovedChildrenMap.get().put(owningObject, new HashSet<OpenmrsObject>());
							}
							for (Object removedItem : removedItems) {
								OpenmrsObject removed = (OpenmrsObject) removedItem;
								entityRemovedChildrenMap.get().get(owningObject).add(removed);
							}
						}
					}
				} else if (Map.class.isAssignableFrom(collection.getClass())) {
					previousSerializedItems = InterceptorUtil.serializeMap(previousStoredSnapshotMap, getAuditLogDao());
					newSerializedItems = InterceptorUtil.serializeMap((Map) collection, getAuditLogDao());
				}
				
				objectChangesMap.get().get(ownerUuid)
				        .put(propertyName, new String[] { newSerializedItems, previousSerializedItems });
			}
		}
	}
	
	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
		//We need to get all collection elements and link their childlogs them to the parent's
		if (collection != null) {
			PersistentCollection persistentColl = (PersistentCollection) collection;
			if (isAuditable(persistentColl.getOwner())) {
				OpenmrsObject owningObject = (OpenmrsObject) persistentColl.getOwner();
				if (Collection.class.isAssignableFrom(collection.getClass())) {
					Collection coll = (Collection) collection;
					if (!coll.isEmpty()) {
						Class<?> elementClass = coll.iterator().next().getClass();
						if (OpenmrsObject.class.isAssignableFrom(elementClass)) {
							if (entityRemovedChildrenMap.get().get(owningObject) == null) {
								entityRemovedChildrenMap.get().put(owningObject, new HashSet<OpenmrsObject>());
							}
							for (Object removedItem : coll) {
								OpenmrsObject removed = (OpenmrsObject) removedItem;
								entityRemovedChildrenMap.get().get(owningObject).add(removed);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * This is a hacky way to find all loaded persistent objects in this session that have
	 * collections
	 * 
	 * @see org.hibernate.EmptyInterceptor#findDirty(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
	                       String[] propertyNames, Type[] types) {
		if (getAuditLogDao().isMonitored(entity.getClass())) {
			if (entityCollectionsMap.get().get(entity) == null) {
				//This is the first time we are trying to find collection elements for this object
				if (log.isDebugEnabled())
					log.debug("Finding collections for object:" + entity.getClass() + " #" + id);
				
				for (int i = 0; i < propertyNames.length; i++) {
					if (types[i].isCollectionType()) {
						Object coll = currentState[i];
						if (coll != null && Collection.class.isAssignableFrom(coll.getClass())) {
							Collection<?> collection = (Collection<?>) coll;
							if (!collection.isEmpty()) {
								if (entityCollectionsMap.get().get(entity) == null) {
									entityCollectionsMap.get().put(entity, new ArrayList<Collection<?>>());
								}
								
								entityCollectionsMap.get().get(entity).add(collection);
							}
						} else {
							//TODO handle maps too because hibernate treats maps to be of CollectionType
						}
					}
				}
			}
		}
		
		return super.findDirty(entity, id, currentState, previousState, propertyNames, types);
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#beforeTransactionCompletion(org.hibernate.Transaction)
	 */
	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		try {
			if (inserts.get().isEmpty() && updates.get().isEmpty() && deletes.get().isEmpty())
				return;
			
			try {
				//TODO handle daemon or un authenticated operations
				
				//If we have any entities in the session that have child collections and there were some updates, 
				//check all collection items to find dirty ones so that we can mark the the owners as dirty too
				//I.e if a ConceptName/Mapping/Description was edited, mark the the Concept as dirty too
				for (Map.Entry<Object, List<Collection<?>>> entry : entityCollectionsMap.get().entrySet()) {
					for (Collection<?> coll : entry.getValue()) {
						for (Object obj : coll) {
							//Apparently inserts.get().contains(obj) always returns false
							//yet when looping and using obj.equals(ins) works					
							//Strangely, updates.get().contains(obj) works
							boolean isInsert = false;
							for (OpenmrsObject ins : inserts.get()) {
								if (obj.equals(ins)) {
									isInsert = true;
									break;
								}
							}
							
							//noinspection SuspiciousMethodCalls
							//We handle the removed collections items below because either way they
							//are nolonger in the current collection
							if (isInsert || updates.get().contains(obj)) {
								OpenmrsObject owner = (OpenmrsObject) entry.getKey();
								if (updates.get().contains(owner)) {
									if (log.isDebugEnabled())
										log.debug("There is already an  auditlog for:" + owner.getClass() + " - "
										        + owner.getUuid());
								} else if (!isInsert) {
									//A collection item was updated and no other update had been made on the owner
									if (log.isDebugEnabled())
										log.debug("Creating log entry for edited object with uuid:" + owner.getUuid()
										        + " of type:" + owner.getClass().getName()
										        + " due to an update for a item in a child collection");
									updates.get().add(owner);
								}
								
								if (getAuditLogDao().isMonitored(obj.getClass())) {
									if (ownerUuidChildLogsMap.get().get(owner.getUuid()) == null)
										ownerUuidChildLogsMap.get().put(owner.getUuid(), new ArrayList<AuditLog>());
									
									OpenmrsObject collElement = (OpenmrsObject) obj;
									AuditLog childLog = instantiateAuditLog(collElement, (isInsert) ? Action.CREATED
									        : Action.UPDATED);
									
									childbjectUuidAuditLogMap.get().put(collElement.getUuid(), childLog);
									ownerUuidChildLogsMap.get().get(owner.getUuid()).add(childLog);
								}
								
								//TODO add this collection to the list of changes properties
								/*Map<String, String[]> propertyValuesMap = objectChangesMap.get().get(owner.getUuid());
								if(propertyValuesMap == null)
									propertyValuesMap = new HashMap<String, String[]>();
									propertyValuesMap.put(arg0, arg1);*/
							}
						}
					}
				}
				entityCollectionsMap.remove();//free some memory
				
				for (Map.Entry<OpenmrsObject, HashSet<OpenmrsObject>> entry : entityRemovedChildrenMap.get().entrySet()) {
					OpenmrsObject removedItemsOwner = entry.getKey();
					for (OpenmrsObject removed : entry.getValue()) {
						//This should fail for collections that don't have all-delete-orphan cascade
						if (deletes.get().contains(removed)) {
							if (getAuditLogDao().isMonitored(removed.getClass())) {
								if (ownerUuidChildLogsMap.get().get(removedItemsOwner.getUuid()) == null)
									ownerUuidChildLogsMap.get().put(removedItemsOwner.getUuid(), new ArrayList<AuditLog>());
								
								AuditLog childLog = instantiateAuditLog(removed, Action.DELETED);
								
								childbjectUuidAuditLogMap.get().put(removed.getUuid(), childLog);
								ownerUuidChildLogsMap.get().get(removedItemsOwner.getUuid()).add(childLog);
							}
						}
					}
				}
				
				entityRemovedChildrenMap.remove();
				
				for (OpenmrsObject insert : inserts.get()) {
					createIfNecessaryAndSaveAuditLog(insert, Action.CREATED);
				}
				inserts.remove();
				
				for (OpenmrsObject delete : deletes.get()) {
					createIfNecessaryAndSaveAuditLog(delete, Action.DELETED);
				}
				deletes.remove();
				
				for (OpenmrsObject update : updates.get()) {
					createIfNecessaryAndSaveAuditLog(update, Action.UPDATED);
				}
				updates.remove();
			}
			catch (Exception e) {
				//error should not bubble out of the intercepter
				log.error("An error occured while creating audit log(s):", e);
			}
		}
		finally {
			//cleanup
			inserts.remove();
			updates.remove();
			deletes.remove();
			objectChangesMap.remove();
			entityCollectionsMap.remove();
			ownerUuidChildLogsMap.remove();
			childbjectUuidAuditLogMap.remove();
			entityRemovedChildrenMap.remove();
			date.remove();
		}
	}
	
	/**
	 * Creates if necessary and saves an auditLog in the DB for the specified object
	 * 
	 * @param object the object to create for the AuditLog
	 * @param action see {@link Action}
	 */
	private void createIfNecessaryAndSaveAuditLog(OpenmrsObject object, Action action) {
		//If this is a collection element, we already created a log for it
		AuditLog auditLog = childbjectUuidAuditLogMap.get().get(object.getUuid());
		if (auditLog == null) {
			auditLog = instantiateAuditLog(object, action);
			getAuditLogDao().save(auditLog);
		}
		
		if ((ownerUuidChildLogsMap != null && ownerUuidChildLogsMap.get().containsKey(object.getUuid()))) {
			for (AuditLog al : ownerUuidChildLogsMap.get().get(object.getUuid())) {
				//We do this for unit tests to pass and in memory reads
				auditLog.addChildAuditLog(al);
				//Hibernate has issues with updating a child if the parent has already been saved
				//So we need to explicitly call saved for the children
				getAuditLogDao().save(al);
			}
		}
	}
	
	/**
	 * Creates a new instance of an {@link AuditLog} for the specified object and Action
	 * 
	 * @param object the object to create for the AuditLog
	 * @param action see {@link Action}
	 * @return the created AuditLog
	 */
	private AuditLog instantiateAuditLog(OpenmrsObject object, Action action) {
		Date tempDate = (date.get() != null) ? date.get() : new Date();
		AuditLog auditLog = new AuditLog(object.getClass().getName(), object.getUuid(), action,
		        Context.getAuthenticatedUser(), tempDate);
		if (action == Action.UPDATED) {
			Map<String, String[]> propertyValuesMap = objectChangesMap.get().get(object.getUuid());
			if (propertyValuesMap != null) {
				auditLog.setChangesData(InterceptorUtil.generateChangesData(propertyValuesMap));
			}
		}
		return auditLog;
	}
	
	/**
	 * Checks if a class is marked as monitored or is explicitly monitored
	 * 
	 * @param entity the entity to check
	 * @return true if is auditable otherwise false
	 */
	private boolean isAuditable(Object entity) {
		return getAuditLogDao().isMonitored(entity.getClass()) || getAuditLogDao().isImplicitlyMonitored(entity.getClass());
	}
}
