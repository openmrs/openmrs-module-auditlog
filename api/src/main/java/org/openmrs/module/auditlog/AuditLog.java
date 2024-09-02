package org.openmrs.module.auditlog;

import org.apache.commons.lang.StringUtils;
import org.openmrs.User;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auditlog_audit_log")
public class AuditLog implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@Column(name = "audit_log_id")
	private Integer auditLogId;

	@Column(name = "uuid", length = 38, nullable = false, unique = true)
	private String uuid = UUID.randomUUID().toString();

	@Column(name = "type", length = 512, nullable = false)
	private String type;

	@Column(name = "identifier", length = 255, nullable = false)
	private String identifier;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", length = 50, nullable = false)
	private Action action;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "date_created", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateCreated;

	@Column(name = "openmrs_version", length = 50, nullable = false)
	private String openmrsVersion;

	@Column(name = "module_version", length = 50, nullable = false)
	private String moduleVersion;

	@ManyToOne
	@JoinColumn(name = "parent_auditlog_id")
	private AuditLog parentAuditLog;

	@OneToMany(mappedBy = "parentAuditLog", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<AuditLog> childAuditLogs = new LinkedHashSet<>();

	@Lob
	@Column(name = "serialized_data")
	private Blob serializedData;

	public enum Action {
		CREATED, UPDATED, DELETED
	}

	// Constructors, getters, and setters

	public AuditLog() {
	}

	public AuditLog(String type, Serializable identifier, Action action, User user, Date dateCreated) {
		this();
		this.type = type;
		this.identifier = String.valueOf(identifier);
		this.action = action;
		this.user = user;
		this.dateCreated = dateCreated;
	}

	public Integer getAuditLogId() {
		return auditLogId;
	}

	public void setAuditLogId(Integer auditLogId) {
		this.auditLogId = auditLogId;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public String getOpenmrsVersion() {
		return openmrsVersion;
	}

	public void setOpenmrsVersion(String openmrsVersion) {
		this.openmrsVersion = openmrsVersion;
	}

	public String getModuleVersion() {
		return moduleVersion;
	}

	public void setModuleVersion(String moduleVersion) {
		this.moduleVersion = moduleVersion;
	}

	public AuditLog getParentAuditLog() {
		return parentAuditLog;
	}

	public void setParentAuditLog(AuditLog parentAuditLog) {
		this.parentAuditLog = parentAuditLog;
	}

	public Set<AuditLog> getChildAuditLogs() {
		return childAuditLogs;
	}

	public void setChildAuditLogs(Set<AuditLog> childAuditLogs) {
		this.childAuditLogs = childAuditLogs;
	}

	public Blob getSerializedData() {
		return serializedData;
	}

	public void setSerializedData(Blob serializedData) {
		this.serializedData = serializedData;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj
				|| (obj instanceof AuditLog && getUuid() != null && ((AuditLog) obj).getUuid().equals(this.getUuid()));
	}

	@Override
	public int hashCode() {
		return (getUuid() != null) ? getUuid().hashCode() : super.hashCode();
	}

	@Override
	public String toString() {
		return action + " " + type + " " + identifier;
	}

	public boolean hasChildLogs() {
		return !getChildAuditLogs().isEmpty();
	}

	/**
	 * Returns the simple forms of the classname property e.g 'Concept Name' will be returned for
	 * ConceptName
	 *
	 * @return the classname
	 */
	public String getSimpleTypeName() {
		String[] sections = new String[0];
		try {
			sections = StringUtils.splitByCharacterTypeCamelCase(Class.forName(getType()).getSimpleName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return StringUtils.join(sections, " ");
	}
}
