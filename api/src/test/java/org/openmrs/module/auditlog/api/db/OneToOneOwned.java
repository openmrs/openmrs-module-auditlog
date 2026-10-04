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

/**
 * Test entity mapped with a one to one association in TestOneToOne.hbm.xml
 */
public class OneToOneOwned {
	
	private Integer id;
	
	private OneToOneOwner owner;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public OneToOneOwner getOwner() {
		return owner;
	}
	
	public void setOwner(OneToOneOwner owner) {
		this.owner = owner;
	}
}
