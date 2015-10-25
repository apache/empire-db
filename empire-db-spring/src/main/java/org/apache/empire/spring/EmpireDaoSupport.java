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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DaoSupport;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

public abstract class EmpireDaoSupport extends DaoSupport {

	private EmpireTemplate empireTemplate;
	private DBDatabase database;
	private DBDatabaseDriver driver;

	public void setDriver(DBDatabaseDriver driver) {
		this.driver = driver;
	}

	@Override
	protected final void checkDaoConfig() throws IllegalArgumentException {
		if (this.empireTemplate == null) {
			throw new IllegalArgumentException(
					"Either empireTemplate or jdbcTemplate or dataSource must be set");
		}
		if (this.database == null) {
			throw new IllegalArgumentException("DBDatabase must be set");
		}
		if (!this.database.isOpen() && this.driver == null) {
			throw new RuntimeException("Database isn't open and no driver set.");
		}
	}

	protected EmpireTemplate getEmpireTemplate() {
		return this.empireTemplate;
	}

	public void setEmpireTemplate(EmpireTemplate empireTemplate) {
		this.empireTemplate = empireTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.empireTemplate = new EmpireTemplate();
		this.empireTemplate.setJdbcTemplate(jdbcTemplate);
		this.empireTemplate.afterPropertiesSet();
	}

	public void setDataSource(DataSource dataSource) {
		this.empireTemplate = new EmpireTemplate();
		this.empireTemplate.setDataSource(dataSource);
		this.empireTemplate.afterPropertiesSet();
	}

	protected JdbcTemplate getJdbcTemplate() {
		return this.empireTemplate.getJdbcTemplate();
	}

	protected void initEmpireDao() {
	}

	@Override
	protected final void initDao() throws Exception {
		super.initDao();
		this.initEmpireDao();
	}

	public void setDatabase(DBDatabase database) {
		if (this.database != null && this.database != database) {
			throw new IllegalArgumentException(
					"setting different database not allowed");
		}
		this.database = database;
	}

	@SuppressWarnings("unchecked")
	public <T extends DBDatabase> T getDatabase() {
		if (!this.database.isOpen()) {
			getJdbcTemplate().execute(new ConnectionCallback<Object>() {

				public Object doInConnection(Connection con)
						throws SQLException, DataAccessException {
					EmpireDaoSupport.this.database.open(driver, con);
					return null;
				}
			});

		}
		return (T) this.database;
	}

	public final DataSource getDataSource() {

		return (this.empireTemplate != null ? this.empireTemplate
				.getJdbcTemplate().getDataSource() : null);
	}

	protected final Connection getConnection()
			throws CannotGetJdbcConnectionException {
		return DataSourceUtils.getConnection(getDataSource());
	}

	protected final void releaseConnection(Connection con) {
		DataSourceUtils.releaseConnection(con, getDataSource());
	}

}
