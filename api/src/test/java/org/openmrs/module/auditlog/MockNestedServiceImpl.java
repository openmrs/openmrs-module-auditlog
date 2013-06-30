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

import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class MockNestedServiceImpl extends BaseOpenmrsService implements MockNestedService {
	
	@Override
	@Transactional
	public void outerTransaction(Location location, boolean innerRollback, boolean outerRollback) {
		
		Context.getLocationService().saveLocation(location);
		
		try {
			Context.getService(MockNestedService.class).innerTransaction(innerRollback);
		}
		catch (Exception e) {}
		
		if (outerRollback) {
			throw new APIException();
		}
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void innerTransaction(boolean rollback) {
		
		EncounterType et = Context.getEncounterService().getEncounterType(ENCOUNTER_TYPE_ID);
		et.setDescription("Some new description");
		Context.getEncounterService().saveEncounterType(et);
		
		if (rollback) {
			throw new APIException();
		}
	}
	
}
