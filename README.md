openmrs-module-auditlog
=======================

Allows keeping an audit trail of changes in data in the database i.e insertions, updates and deletes.

## Technical Details
The module uses a hibernate based interceptor to track created, updated and deleted domain objects in the database.

## Configuration
When the module is first installed, there is really nothing happening, you need to set the values of the global properties below to get it in action.
- **auditlog.auditingStrategy** - Specifies the auditing strategy to be used by the module, allowed values are: ALL, ALL_EXCEPT, NONE, NONE_EXCEPT. The default value is NONE.
- **auditlog.storeLastStateOfDeletedItems** - Specifies whether the last states of deleted items should be serialized and stored in the DB, defaults to false. 
- **auditlog.exceptions** - Specifies the fully qualified java class names of domain objects for which to maintain an audit trail when the auditing strategy is set to NONE_EXCEPT otherwise specifies the class names of objects for which not to maintain an audit log, when the auditing strategy is set to ALL_EXCEPT.

After you've configured the module and you create, update or purge(delete forever) any watched domain objects, from the legacy UI you should be able to see the audit trail by going to the main admin page, under the **Audit Log** section select **View Audit Log**. Green rows indicate newly created items, red rows indicate deleted items while the clear ones indicate updated ones, if you click on a row for an updated item, you should be able to see details of what properties were edited including their old and new values.

## Known Issues
- The module currently writes the audit log details to the DB, this table is expected to quickly grow big for a fairly large implementation depending on their configurations e.g if they track all domain object. Future versions of the module should be able to automatically archive logs older than a certain configured period to the file system in order to keep the size of the table down.
- Any changes applied to the DB via liquibase or by directly running SQL queries against the DB are not caught for logging.
- The module's hibernate interceptor is called via the interceptor chaining process in core API, the order in which the registered interceptors are called is based on alphabetical order of their spring bean ids, this implies that if you run the module alongside another that registers its own interceptor that happens to come after it in the chain, it can potentially affect the auditlog module's functionality in case that other interceptor alters the persistent object's state, this can be addressed by making a switch from a hibernate interceptor to hibernate's event mechanism. 
- Most likely the module is not compatible with versions 2.0 and above of OpenMRS core.

## Alternatives to the module
- Use triggers to create the audit trail or the built-in logging mechanism of your DB.
- Update events module to be based on debezium and embed the auditlog feature in the events module, but with debezium there would be no way to know the user that made the change
