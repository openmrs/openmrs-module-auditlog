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
package org.openmrs.module.auditlog.web.dto;

import java.util.List;

/**
 * Paginated response envelope returned by the Audit Log REST endpoint.
 */
public class AuditLogPageResponse {

	private final List<AuditLogDTO> results;

	private final int startIndex;

	private final int limit;

	public AuditLogPageResponse(List<AuditLogDTO> results, int startIndex, int limit) {
		this.results = results;
		this.startIndex = startIndex;
		this.limit = limit;
	}

	public List<AuditLogDTO> getResults() {
		return results;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getLimit() {
		return limit;
	}

	public int getResultsCount() {
		return results != null ? results.size() : 0;
	}
}
