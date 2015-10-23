/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.spring;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class DBDatabaseFactoryBean implements FactoryBean<DBDatabase>,
		InitializingBean {

	private boolean singleton = true;

	private DBDatabase singletonInstance;
	private String schema;
	private String linkName;
	private DBDatabaseDriver driver;
	private boolean preparedStatementsEnabled = true;
	private Class<? extends DBDatabase> databaseClass = null;
	private boolean earlyOpen = true;

	private Class<DBDatabaseDriver> driverClass;

	public final void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public void setDatabaseClass(Class<? extends DBDatabase> databaseClass) {
		this.databaseClass = databaseClass;
	}

	public void setEarlyOpen(boolean earlyOpen) {
		this.earlyOpen = earlyOpen;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setLinkName(String linkName) {
		this.linkName = linkName;
	}

	public void setDriver(DBDatabaseDriver driver) {
		this.driver = driver;
	}
	
	public void setDriverClass(Class<DBDatabaseDriver> driverClass) {
		this.driverClass = driverClass;
	}


	public void setPreparedStatementsEnabled(boolean preparedStatementsEnabled) {
		this.preparedStatementsEnabled = preparedStatementsEnabled;
	}

	public DBDatabase getObject() throws Exception {
		if (this.singleton) {
			return this.singletonInstance;
		} else {
			return createInstance();
		}
	}

	public Class<?> getObjectType() {
		return DBDatabase.class;
	}

	public boolean isSingleton() {
		return false;
	}

	public void afterPropertiesSet() throws Exception {
		if (driver == null && driverClass == null){
			throw new RuntimeException("driver or driverClass must be set");	
		}
		
		if (driver == null){
			driver = (DBDatabaseDriver) driverClass.newInstance();
		}
		
		if (this.singleton) {
			this.singletonInstance = createInstance();
		}
	}

	protected DBDatabase createInstance() {
		DBDatabase database = null;
		if (this.databaseClass == null) {
			database = new DefaultDb();
		} else {
			try {
				database = this.databaseClass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Failed to create database: "
						+ this.databaseClass, e);
			}
		}
		if (this.schema != null && this.schema.trim().length() > 0){
			database.setSchema(schema);	
		}
		if (this.linkName != null && this.linkName.trim().length() > 0){
			database.setLinkName(linkName);	
		}
		
		
		database.setPreparedStatementsEnabled(preparedStatementsEnabled);
		if (earlyOpen) {
			database.open(driver, null);
		}
		return database;
	}

	public static class DefaultDb extends DBDatabase {

		private static final long serialVersionUID = 1L;

	}

}
