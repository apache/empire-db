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
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

public class EmpireTemplate implements InitializingBean {

	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	public final void setDataSource(DataSource dataSource) {
		if (this.jdbcTemplate == null || dataSource != this.jdbcTemplate.getDataSource()) {
			this.jdbcTemplate = new JdbcTemplate(dataSource);
			this.jdbcTemplate.afterPropertiesSet();
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (getJdbcTemplate() == null) {
			throw new IllegalArgumentException("Property 'jdbcTemplate' is required");
		}

	}

	public <K> List<K> query(final DBCommand cmd, final EmpireDataReader<K> dataReader) {
		return query(cmd, new EmpireDataReaderExtractor<K>(dataReader));

	}

	public List<Object> queryForList(final DBCommand cmd, final DBColumnExpr col) {
		return query(cmd, new EmpireValueReader(col));

	}

	public Long queryForLong(final DBCommand cmd, final DBColumnExpr col, Long defaultValue) {
		return DataAccessUtils.uniqueResult(queryForLongList(cmd, col, defaultValue));
	}

	public List<Long> queryForLongList(final DBCommand cmd, final DBColumnExpr col, final Long defaultValue) {
		return query(cmd, new EmpireLongValueReader(col, defaultValue));

	}

	public Integer queryForInteger(final DBCommand cmd, final DBColumnExpr col, Integer defaultValue) {
		return DataAccessUtils.uniqueResult(queryForIntegerList(cmd, col, defaultValue));
	}

	public List<Integer> queryForIntegerList(final DBCommand cmd, final DBColumnExpr col, final Integer defaultValue) {
		return query(cmd, new EmpireIntegerValueReader(col, defaultValue));
	}

	public String queryForString(final DBCommand cmd, final DBColumnExpr col) {
		return DataAccessUtils.uniqueResult(queryForStringList(cmd, col));
	}

	public List<String> queryForStringList(final DBCommand cmd, final DBColumnExpr col) {
		return query(cmd, new EmpireStringValueReader(col));
	}

	public <K> K query(final DBCommand cmd, final EmpireReaderExtractor<K> readerHandler) {

		class QueryCallback implements ConnectionCallback<K> {
			public K doInConnection(Connection connection) throws SQLException, DataAccessException {
				return query(connection, cmd, readerHandler);
			}
		}
		return getJdbcTemplate().execute(new QueryCallback());

	}

	public void query(final DBCommand cmd, final EmpireRowCallbackHandler rowCallbackHandler) {
		query(cmd, new EmpireRowCallbackHandlerExtractor(rowCallbackHandler));
	}

	public <K> K queryForObject(final DBCommand cmd, final EmpireDataReader<K> dataReader) {

		return DataAccessUtils.uniqueResult(query(cmd, dataReader));

	}

	public void deleteRecord(final DBRecord record) {

		class DeleteRecordCallback implements ConnectionCallback<Object> {
			public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
				record.delete(connection);
				return null;
			}
		}
		getJdbcTemplate().execute(new DeleteRecordCallback());

	}

	public void deleteRecord(final DBTable table, final Object key) {
		deleteRecord(table, new Object[] { key });
	}

	public void deleteRecord(final DBTable table, final Object[] keys) {

		class DeleteRecordCallback implements ConnectionCallback<Object> {
			public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
				DBRecord record = new EmpireRecord();
				record.read(table, keys, connection);
				record.delete(connection);
				return null;
			}
		}
		getJdbcTemplate().execute(new DeleteRecordCallback());

	}

	public DBRecord updateRecord(final DBRecord record) {

		class UpdateRecordCallback implements ConnectionCallback<DBRecord> {
			public DBRecord doInConnection(Connection connection) throws SQLException, DataAccessException {
				record.update(connection);
				return record;
			}
		}

		return getJdbcTemplate().execute(new UpdateRecordCallback());

	}

	public int executeUpdate(final DBCommand cmd) {

		class UpdateRecordCallback implements ConnectionCallback<Integer> {
			public Integer doInConnection(Connection connection) throws SQLException, DataAccessException {
				return cmd.getDatabase().executeUpdate(cmd, connection);
			}
		}

		return getJdbcTemplate().execute(new UpdateRecordCallback());

	}

	public int executeDelete(final DBTable table, final DBCommand cmd) {

		class DeleteRecordCallback implements ConnectionCallback<Integer> {
			public Integer doInConnection(Connection connection) throws SQLException, DataAccessException {
				return cmd.getDatabase().executeDelete(table, cmd, connection);
			}
		}

		return getJdbcTemplate().execute(new DeleteRecordCallback());

	}

	public int executeInsert(final DBCommand cmd) {

		class InsertRecordCallback implements ConnectionCallback<Integer> {
			public Integer doInConnection(Connection connection) throws SQLException, DataAccessException {
				return cmd.getDatabase().executeInsert(cmd, connection);
			}
		}

		return getJdbcTemplate().execute(new InsertRecordCallback());

	}

	public DBRecord newRecord(final DBRowSet table) {
		DBRecord record = new EmpireRecord();
		record.create(table);
		return record;
	}

	public DBRecord openRecord(final DBRowSet table, final Object key) {
		return openRecord(table, new Object[] { key });
	}

	public DBRecord openRecord(final DBRowSet table, final Object[] keys) {

		class ReadRecordCallback implements ConnectionCallback<DBRecord> {
			public DBRecord doInConnection(Connection connection) throws SQLException, DataAccessException {
				DBRecord record = new EmpireRecord();
				try {
					record.read(table, keys, connection);
				} catch (RecordNotFoundException e) {
					return null;
				}
				return record;
			}
		}

		return getJdbcTemplate().execute(new ReadRecordCallback());

	}

	public <K> K execute(ConnectionCallback<K> connectionCallback) {
		return getJdbcTemplate().execute(connectionCallback);
	}

	private <K> K query(Connection connection, DBCommand command, EmpireReaderExtractor<K> callback) {
		DBReader reader = newDBReader();
		try {
			reader.open(command, connection);

			return callback.process(reader);

		} finally {
			reader.close();
		}
	}

	private DBReader newDBReader() {
		return new EmpireReader();
	}
	
	public static class EmpireRowCallbackHandlerExtractor implements EmpireReaderExtractor<Object> {

		private final EmpireRowCallbackHandler rowCallbackHandler;

		public EmpireRowCallbackHandlerExtractor(EmpireRowCallbackHandler rowCallbackHandler) {
			Assert.notNull(rowCallbackHandler, "RowCallbackHandler is required");
			this.rowCallbackHandler = rowCallbackHandler;
		}

//		@Override
		public Object process(DBReader reader) {
			try {
				while (reader.moveNext()) {
					this.rowCallbackHandler.processRow(reader);
				}
				return null;
			} finally {
				reader.close();
			}
		}
		
	}

	public static class EmpireDataReaderExtractor<K> implements EmpireReaderExtractor<List<K>> {

		private final EmpireDataReader<K> dataReader;

		public EmpireDataReaderExtractor(EmpireDataReader<K> rowMapper) {
			Assert.notNull(rowMapper, "DataReader is required");
			this.dataReader = rowMapper;
		}

//		@Override
		public List<K> process(DBReader reader) {
			try {
				List<K> results = new ArrayList<K>();

				while (reader.moveNext()) {
					results.add(this.dataReader.read(reader));
				}

				return results;

			} finally {
				reader.close();
			}
		}

	}

}
