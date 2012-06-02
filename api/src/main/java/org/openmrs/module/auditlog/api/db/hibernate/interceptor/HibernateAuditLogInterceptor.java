package org.openmrs.module.auditlog.api.db.hibernate.interceptor;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.Type;
import org.openmrs.OpenmrsObject;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.module.auditlog.MonitoredObject;
import org.openmrs.module.auditlog.api.db.AuditLogDAO;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A hibernate {@link Interceptor} implementation, intercepts any database inserts, updates and
 * deletes and creates audit log entries for {@link MonitoredObject}s, it logs changes for a single
 * session meaning that if User A and B concurrently make changes to the same object, there will be
 * 2 log entries in the DB, one for each user's session. Any changes/inserts/deletes made to the DB
 * that are not made through the application won't be deteceted by the module.
 */
public class HibernateAuditLogInterceptor extends EmptyInterceptor implements ApplicationContextAware {
	
	private static final long serialVersionUID = 1L;
	
	private static final Log log = LogFactory.getLog(HibernateAuditLogInterceptor.class);
	
	//we use a set because the same object can be loaded multiple times
	private ThreadLocal<HashSet<OpenmrsObject>> inserts = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> updates = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> deletes = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<Boolean> disableInterceptor = new ThreadLocal<Boolean>();
	
	private static Set<String> monitoredClassNames = null;
	
	//will be used to determining if there was an exception so that we don't try 
	//to commit the transaction associated to the saved audit log entry
	private ThreadLocal<Boolean> hadErrorsOnSave = new ThreadLocal<Boolean>();
	
	private AuditLogDAO auditLogDao;
	
	/**
	 * We need access to this to populate the dao property, the saveAuditLog method is not available
	 * to in auditLogservice to ensure no other code creates log entries
	 */
	private ApplicationContext applicationContext;
	
	/**
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * @return the dao
	 */
	public AuditLogDAO getAuditLogDao() {
		if (auditLogDao == null)
			auditLogDao = applicationContext.getBean(AuditLogDAO.class);
		
		return auditLogDao;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onSave(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 * @should create an audit log entry with create type
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (isMonitored(entity)) {
			OpenmrsObject openmrsObject = (OpenmrsObject) entity;
			if (log.isDebugEnabled())
				log.debug("Creating log entry for CREATED object with uuid:" + openmrsObject.getUuid() + " of type:"
				        + entity.getClass().getName());
			
			if (inserts.get() == null)
				inserts.set(new HashSet<OpenmrsObject>());
			
			inserts.get().add(openmrsObject);
			System.out.println("Creating log entry for CREATED object with uuid:" + openmrsObject.getUuid() + " of type:"
			        + entity.getClass().getName());
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
		if (isMonitored(entity) && propertyNames != null) {
			OpenmrsObject openmrsObject = (OpenmrsObject) entity;
			boolean hasChanges = false;
			for (int i = 0; i < propertyNames.length; i++) {
				//we need ignore dateChanged and changedBy fields 
				if ("dateChanged".equals(propertyNames[i]) || "changedBy".equals(propertyNames[i]))
					continue;
				
				//TODO Ignore user defined ignored properties
				
				Object previousValue = (previousState != null) ? previousState[i] : null;
				if (!OpenmrsUtil.nullSafeEquals(currentState[i], previousValue)) {
					//For string properties, ignore changes from null to blank and vice versa
					if (StringType.class.getName().equals(types[i].getClass().getName())
					        || TextType.class.getName().equals(types[i].getClass().getName())) {
						String currentStateString = null;
						if (currentState[i] != null && !StringUtils.isBlank(currentState[i].toString()))
							currentStateString = currentState[i].toString();
						
						String previousValueString = null;
						if (previousValue != null && !StringUtils.isBlank(previousValue.toString()))
							previousValueString = previousValue.toString();
						
						//TODO Case sensibility here should be configurable via a GP
						if (OpenmrsUtil.nullSafeEqualsIgnoreCase(previousValueString, currentStateString))
							continue;
					}
					
					/*System.err.println("\nid=" + openmrsObject.getId() + ", " + propertyNames[i] + ", "
					        + entity.getClass().getSimpleName() + " -> CURRENT=[" + currentState[i] + "], PREV=["
					        + previousState[i] + "]\n");*/
					hasChanges = true;
					break;
				}
			}
			
			if (hasChanges) {
				if (log.isDebugEnabled())
					log.debug("Creating log entry for EDITED object with uuid:" + openmrsObject.getUuid() + " of type:"
					        + entity.getClass().getName());
				
				if (updates.get() == null)
					updates.set(new HashSet<OpenmrsObject>());
				
				updates.get().add(openmrsObject);
				System.out.println("Creating log entry for EDITED object with uuid:" + openmrsObject.getUuid() + " of type:"
				        + entity.getClass().getName());
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
		if (isMonitored(entity)) {
			OpenmrsObject openmrsObject = (OpenmrsObject) entity;
			if (log.isDebugEnabled())
				log.debug("Creating log entry for DELETED object with uuid:" + openmrsObject.getUuid() + " of type:"
				        + entity.getClass().getName());
			
			if (deletes.get() == null)
				deletes.set(new HashSet<OpenmrsObject>());
			
			deletes.get().add(openmrsObject);
			System.out.println("Creating log entry for DELETED object with uuid:" + openmrsObject.getUuid() + " of type:"
			        + entity.getClass().getName());
		}
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#afterTransactionCompletion(org.hibernate.Transaction)
	 */
	@Override
	public void afterTransactionCompletion(Transaction tx) {
		Date date = new Date();
		try {
			if (disableInterceptor.get() == null && tx.wasCommitted()) {
				if (inserts.get() == null && updates.get() == null && deletes.get() == null)
					return;
				
				try {
					//it is the first time since application startup to access this list we can now
					//populate it without worrying about hibernate flushes for newly created objects.
					boolean checkAgainForMonitoredClasses = false;
					if (monitoredClassNames == null) {
						checkAgainForMonitoredClasses = true;
						loadMonitoredClassNames();
					}
					
					User user = Context.getAuthenticatedUser();
					if (inserts.get() != null) {
						for (OpenmrsObject insert : inserts.get()) {
							//We should filter out objects of un monitored types that got included 
							//because the list of monitored classnames was still null
							if (checkAgainForMonitoredClasses && !monitoredClassNames.contains(insert.getClass().getName()))
								continue;
							
							createLog(new AuditLog(insert.getClass().getName(), insert.getUuid(), Action.CREATED, user,
							        date, UUID.randomUUID().toString()));
						}
					}
					
					if (updates.get() != null) {
						for (OpenmrsObject update : updates.get()) {
							if (checkAgainForMonitoredClasses && !monitoredClassNames.contains(update.getClass().getName()))
								continue;
							
							createLog(new AuditLog(update.getClass().getName(), update.getUuid(), Action.UPDATED, user,
							        date, UUID.randomUUID().toString()));
						}
					}
					
					if (deletes.get() != null) {
						for (OpenmrsObject delete : deletes.get()) {
							if (checkAgainForMonitoredClasses && !monitoredClassNames.contains(delete.getClass().getName()))
								continue;
							
							createLog(new AuditLog(delete.getClass().getName(), delete.getUuid(), Action.DELETED, user,
							        date, UUID.randomUUID().toString()));
						}
					}
					
					//Ensures 'afterTransactionCompletion(tx)' is not called recursively
					disableInterceptor.set(true);
					
					//Hibernate will bomb if we attempt to flush after an exception
					if (hadErrorsOnSave.get() == null) {
						//at this point, the transaction is already committed, 
						//so we need to call commit() again to save to the DB
						tx.commit();
					}
				}
				catch (Exception e) {
					//error should not bubble out of the interceptor
					log.error("An error occured while creating audit log(s)");
				}
			}
		}
		finally {
			//cleanup
			if (inserts.get() != null)
				inserts.remove();
			if (updates.get() != null)
				updates.remove();
			if (deletes.get() != null)
				deletes.remove();
			if (hadErrorsOnSave.get() != null)
				hadErrorsOnSave.remove();
			if (disableInterceptor.get() != null)
				disableInterceptor.remove();
		}
	}
	
	/**
	 * Saves the log entry to the database
	 * 
	 * @param auditLog
	 */
	private void createLog(AuditLog auditLog) {
		try {
			getAuditLogDao().save(auditLog);
		}
		catch (Exception e) {
			//should not bubble out of the interceptor
			log.error("An error occured while saving audit log");
			hadErrorsOnSave.set(true);
		}
	}
	
	/**
	 * Checks if specified object is among the ones that are monitored and is an
	 * {@link OpenmrsObject}
	 * 
	 * @param obj the object the check
	 * @return true if the object is a monitored one otherwise false
	 */
	private boolean isMonitored(Object obj) {
		//If monitoredClassNames is still null, we can't load it yet because in case there are 
		//any new objects, hibernate will flush and them bomb since they have no ids yet
		return OpenmrsObject.class.isAssignableFrom(obj.getClass())
		        && (monitoredClassNames == null || monitoredClassNames.contains(obj.getClass().getName()));
	}
	
	/**
	 * Convenience method that populates the set of class names for the monitored objects
	 */
	private void loadMonitoredClassNames() {
		List<MonitoredObject> monitoredObjs = getAuditLogDao().getAllMonitoredObjects();
		monitoredClassNames = new HashSet<String>();
		
		for (MonitoredObject monitoredObject : monitoredObjs)
			monitoredClassNames.add(monitoredObject.getClassName());
	}
}
