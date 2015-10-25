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
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * This class simplifies database access methods with Empire. Database queries
 * are executed with the help of callback interfaces, while hides connection
 * handling from the developer. Only the query logic has to be implemented and
 * leave getting and releasing the connection to the template.
 * 
 * Some methods (delete, update) delegates to DBRecord, DBDatabase and DBCommand
 * methods while hiding connection handling from developers.
 * 
 * This class is working with a wrapped JdbcTemplate, so either jdbcTemplate or
 * dataSource must be set.
 * 
 * Allows customization of creating DBReader and DBRecord instances through
 * ObjectFactories.
 * 
 * This class is based on {@link org.springframework.jdbc.core.JdbcTemplate}.
 * 
 *
 */

public class EmpireTemplate implements InitializingBean {

	/** Wrapped JdbcTemplate instance */
	private JdbcTemplate jdbcTemplate;

	/** Factory to create a new DBReader instance, by default returns DBReader */
	private ObjectFactory<DBReader> readerFactory = new ObjectFactory<DBReader>() {

		@Override
		public DBReader getObject() throws BeansException {
			return new DBReader();
		}

	};

	/** Factory to create a new DBRecord instance, by default returns DBRecord */
	private ObjectFactory<DBRecord> recordFactory = new ObjectFactory<DBRecord>() {

		@Override
		public DBRecord getObject() throws BeansException {
			return new DBRecord();
		}

	};

	/** Default constructor */
	public EmpireTemplate() {
		super();
	}

	/**
	 * Setting a custom ObjectFactory to allow custom DBRecord create with
	 * newRecord().
	 * 
	 * @param recordFactory
	 */

	public void setDBRecordFactory(ObjectFactory<DBRecord> recordFactory) {
		this.recordFactory = recordFactory;
	}

	/**
	 * Setting a custom DBRecord class to use in newRecord().
	 * 
	 * @param recordClass
	 *            the class which extends DBRecord.class
	 */
	public void setDBRecordClass(final Class<? extends DBRecord> recordClass) {
		this.recordFactory = new ObjectFactory<DBRecord>() {

			@Override
			public DBRecord getObject() throws BeansException {
				try {
					return recordClass.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	/**
	 * Setting a custom ObjectFactory to allow custom DBReaders to use in
	 * queries.
	 * 
	 * @param readerFactory
	 */

	public void setDBReaderFactory(ObjectFactory<DBReader> readerFactory) {
		this.readerFactory = readerFactory;
	}

	/**
	 * Setting a custom DBReader class to use in queries.
	 * 
	 * @param readerClass
	 *            the class which extends DBReader.class
	 */
	public void setDBReaderClass(final Class<? extends DBReader> readerClass) {
		this.readerFactory = new ObjectFactory<DBReader>() {

			@Override
			public DBReader getObject() throws BeansException {
				try {
					return readerClass.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	/** Setting the wrapped JdbcTemplate */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/** Setting the datasource */
	public final void setDataSource(DataSource dataSource) {
		if (this.jdbcTemplate == null
				|| dataSource != this.jdbcTemplate.getDataSource()) {
			this.jdbcTemplate = new JdbcTemplate(dataSource);
			this.jdbcTemplate.afterPropertiesSet();
		}
	}

	public void afterPropertiesSet() {
		if (getJdbcTemplate() == null) {
			throw new IllegalArgumentException(
					"Property 'jdbcTemplate' is required, either jdbcTemplate or dataSource must be set.");
		}

	}

	/**
	 * Executes a given DBCommand, mapping each row to a Java object via a
	 * DBRecordMapper.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param recordMapper
	 *            the mapper which maps each DBRecordData to a Java object
	 * @return the extracted list
	 */
	public <K> List<K> query(final DBCommand cmd,
			final DBRecordMapper<K> recordMapper) {
		return query(cmd, new DbRecordMapperExtractor<K>(recordMapper));

	}

	/**
	 * Executes a given DBCommand, mapping a single row to a Java object using
	 * the provided DBRecordMapper.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param recordMapper
	 *            the DBRecordMapper to map
	 * @return the single Object
	 * @throws IncorrectResultSizeDataAccessException
	 *             if more than one result object has been found
	 */
	public <K> K queryForObject(final DBCommand cmd,
			final DBRecordMapper<K> recordMapper) {

		return DataAccessUtils.uniqueResult(query(cmd, recordMapper));

	}

	/**
	 * Executes a given DBCommand, mapping a single column to a Java object
	 * using on DBRecordData.getValue() method.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param col
	 *            the column to map
	 * @return the extracted list
	 */
	public List<Object> queryForList(final DBCommand cmd, final DBColumnExpr col) {
		class SingleValueMapper implements DBRecordMapper<Object> {

			@Override
			public Object mapRecord(DBRecordData record, int rowNum) {
				return record.getValue(col);
			}

		}
		return query(cmd, new SingleValueMapper());

	}

	/**
	 * Executes a given DBCommand, mapping a single column to a single Long. If
	 * the value in the database is null, defaultValue is returned.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param col
	 *            the column to map
	 * @param defaultValue
	 *            the value to return in case of the database value is null
	 * @return the single Long value
	 * @throws IncorrectResultSizeDataAccessException
	 *             if more than one result object has been found
	 */
	public Long queryForLong(final DBCommand cmd, final DBColumnExpr col,
			Long defaultValue) {
		return DataAccessUtils.uniqueResult(queryForLongList(cmd, col,
				defaultValue));
	}

	/**
	 * Executes a given DBCommand, extracting a single column to a List of Long.
	 * If the value in the database is null, defaultValue is added to the list.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param col
	 *            the column to map
	 * @param defaultValue
	 *            the value to return in case of the database value is null
	 * @return the extracted list
	 */
	public List<Long> queryForLongList(final DBCommand cmd,
			final DBColumnExpr col, final Long defaultValue) {
		class SingleLongMapper implements DBRecordMapper<Long> {

			@Override
			public Long mapRecord(DBRecordData record, int rowNum) {
				return record.isNull(col) ? defaultValue : record.getLong(col);
			}

		}
		return query(cmd, new SingleLongMapper());

	}

	/**
	 * Executes a given DBCommand, mapping a single column to a single Integer.
	 * If the value in the database is null, defaultValue is returned.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param col
	 *            the column to map
	 * @param defaultValue
	 *            the value to return in case of the database value is null
	 * @return the single Integer value
	 * @throws IncorrectResultSizeDataAccessException
	 *             if more than one result object has been found
	 */
	public Integer queryForInteger(final DBCommand cmd, final DBColumnExpr col,
			Integer defaultValue) {
		return DataAccessUtils.uniqueResult(queryForIntegerList(cmd, col,
				defaultValue));
	}

	/**
	 * Executes a given DBCommand, extracting a single column to a List of
	 * Integers. If the value in the database is null, defaultValue is added to
	 * the list.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param col
	 *            the column to map
	 * @param defaultValue
	 *            the value to return in case of the database value is null
	 * @return the extracted list
	 */
	public List<Integer> queryForIntegerList(final DBCommand cmd,
			final DBColumnExpr col, final Integer defaultValue) {
		class SingleIntegerMapper implements DBRecordMapper<Integer> {

			@Override
			public Integer mapRecord(DBRecordData record, int rowNum) {
				return record.isNull(col) ? defaultValue : record.getInt(col);
			}

		}
		return query(cmd, new SingleIntegerMapper());
	}

	/**
	 * Executes a given DBCommand, mapping a single column to a single String.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param col
	 *            the column to map
	 * @return the single String value
	 * @throws IncorrectResultSizeDataAccessException
	 *             if more than one result object has been found
	 */
	public String queryForString(final DBCommand cmd, final DBColumnExpr col) {
		return DataAccessUtils.uniqueResult(queryForStringList(cmd, col));
	}

	/**
	 * Executes a given DBCommand, extracting a single column to a List of
	 * Strings.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param col
	 *            the column to map
	 * @return the extracted list
	 */
	public List<String> queryForStringList(final DBCommand cmd,
			final DBColumnExpr col) {
		class SingleStringMapper implements DBRecordMapper<String> {

			@Override
			public String mapRecord(DBRecordData record, int rowNum) {
				return record.getString(col);
			}

		}
		return query(cmd, new SingleStringMapper());
	}

	/**
	 * Executes a given DBCommand and handles the DBReader with the provided
	 * DBReaderExtractor.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param readerExtractor
	 * @return the result returned by the readerExtractor
	 */
	public <K> K query(final DBCommand cmd,
			final DBReaderExtractor<K> readerExtractor) {

		class QueryCallback implements ConnectionCallback<K> {
			public K doInConnection(Connection connection) throws SQLException,
					DataAccessException {
				return query(connection, cmd, readerExtractor);
			}
		}
		return getJdbcTemplate().execute(new QueryCallback());

	}

	/**
	 * Executes a given DBCommand and handles each row of the DBReader with the
	 * provided DBRecordCallbackHandler.
	 * 
	 * @param cmd
	 *            the DBCommand to execute
	 * @param recordCallbackHandler
	 */
	public void query(final DBCommand cmd,
			final DBRecordCallbackHandler recordCallbackHandler) {
		query(cmd, new DbRecordCallbackHandlerExtractor(recordCallbackHandler));
	}

	/**
	 * Deletes a given record from the database. Calls
	 * DBRecord.delete(connection).
	 * 
	 * @see org.apache.empire.db.DBRecord#delete(Connection)
	 * @param record
	 *            to delete
	 */
	public void deleteRecord(final DBRecord record) {

		class DeleteRecordCallback implements ConnectionCallback<Object> {
			public Object doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				record.delete(connection);
				return null;
			}
		}
		getJdbcTemplate().execute(new DeleteRecordCallback());

	}

	/**
	 * Deletes a record from a table with a given single primary key.
	 * 
	 * @param table
	 *            the table to delete from
	 * @param key
	 *            the primary key
	 */

	public void deleteRecord(final DBTable table, final Object key) {
		deleteRecord(table, new Object[] { key });
	}

	/**
	 * Deletes a record from a table with a given multiple primary key. Calls
	 * DBTable.deleteRecord(Object[], Connection).
	 * 
	 * @see org.apache.empire.db.DBTable.deleteRecord(Object[], Connection)
	 * @param table
	 *            the table to delete from
	 * @param keys
	 *            the primary keys array
	 */
	public void deleteRecord(final DBTable table, final Object[] keys) {

		class DeleteRecordCallback implements ConnectionCallback<Object> {
			public Object doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				table.deleteRecord(keys, connection);
				return null;
			}
		}
		getJdbcTemplate().execute(new DeleteRecordCallback());

	}

	/**
	 * Updates the record and saves all changes in the database. Calls
	 * DBRecord.update(connection).
	 *
	 * @see org.apache.empire.db.DBRecord#update(Connection)
	 * @param record
	 *            to update
	 * @return the updated record
	 */
	public DBRecord updateRecord(final DBRecord record) {

		class UpdateRecordCallback implements ConnectionCallback<DBRecord> {
			public DBRecord doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				record.update(connection);
				return record;
			}
		}

		return getJdbcTemplate().execute(new UpdateRecordCallback());

	}

	/**
	 * Executes an Update statement from a command object. This method delegates
	 * to DBDatabase.executeUpdate(cmd, connection), getting the DBDatabase
	 * instance from the DBCOmmand object.
	 *
	 * @see org.apache.empire.db.DBDatabase#executeUpdate(DBCommand, Connection)
	 * 
	 * @param cmd
	 *            the command object containing the update command
	 * @return the number of records that have been updated with the supplied
	 *         statement
	 */
	public int executeUpdate(final DBCommand cmd) {

		class UpdateRecordCallback implements ConnectionCallback<Integer> {
			public Integer doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				return cmd.getDatabase().executeUpdate(cmd, connection);
			}
		}

		return getJdbcTemplate().execute(new UpdateRecordCallback());

	}

	/**
	 * Executes a Delete statement from a command object. This method delegates
	 * to DBDatabase.executeDelete(cmd, connection), getting the DBDatabase
	 * instance from the DBCOmmand object.
	 *
	 * @see org.apache.empire.db.DBDatabase#executeDelete(DBCommand, Connection)
	 * 
	 * @param cmd
	 *            the command object containing the delete command
	 * @return the number of records that have been deleted with the supplied
	 *         statement
	 */
	public int executeDelete(final DBTable table, final DBCommand cmd) {

		class DeleteRecordCallback implements ConnectionCallback<Integer> {
			public Integer doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				return cmd.getDatabase().executeDelete(table, cmd, connection);
			}
		}

		return getJdbcTemplate().execute(new DeleteRecordCallback());

	}

	/**
	 * Executes an Insert statement from a command object. This method delegates
	 * to DBDatabase.executeInsert(cmd, connection), getting the DBDatabase
	 * instance from the DBCOmmand object.
	 *
	 * @see org.apache.empire.db.DBDatabase#executeInsert(DBCommand, Connection)
	 * 
	 * @param cmd
	 *            the command object containing the insert command
	 * @return the number of records that have been inserted with the supplied
	 *         statement
	 */
	public int executeInsert(final DBCommand cmd) {

		class InsertRecordCallback implements ConnectionCallback<Integer> {
			public Integer doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				return cmd.getDatabase().executeInsert(cmd, connection);
			}
		}

		return getJdbcTemplate().execute(new InsertRecordCallback());

	}

	/**
	 * Helper method to an create a DBRecord instance. Can be customized through
	 * setRecordFactory or setRecordClass methods.
	 *
	 * @param table
	 *            the table
	 * @return the new DBRecord instance
	 */
	public DBRecord newRecord(final DBRowSet table) {
		DBRecord record = this.recordFactory.getObject();
		record.create(table);
		return record;
	}

	/**
	 * Opens a DBRecord instance with the given multiple primary keys. In
	 * contrast of getRecord(DBRowSet, Object) this method throws
	 * RecordNotFoundExpcetion if matching record exists.
	 *
	 * @param table
	 *            the table to read the record from
	 * @param key
	 *            the primary key
	 * @return the DBRecord instance, never null
	 * @throws org.apache.empire.db.exceptions.RecordNotFoundException
	 *             in case of the record not found
	 */
	public DBRecord openRecord(final DBRowSet table, final Object key) {
		return openRecord(table, new Object[] { key });
	}

	/**
	 * Opens a DBRecord instance with the given multiple primary keys. In
	 * contrast of getRecord(DBRowSet, Object[]) this method throws
	 * RecordNotFoundExpcetion if matching record exists.
	 *
	 * @param table
	 *            the table to read the record from
	 * @param keys
	 *            the primary key array
	 * @return the DBRecord instance, never null
	 * @throws org.apache.empire.db.exceptions.RecordNotFoundException
	 *             in case of the record not found
	 */
	public DBRecord openRecord(final DBRowSet table, final Object[] keys) {

		class ReadRecordCallback implements ConnectionCallback<DBRecord> {
			public DBRecord doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				DBRecord record = new EmpireRecord();
				record.read(table, keys, connection);
				return record;
			}
		}

		return getJdbcTemplate().execute(new ReadRecordCallback());

	}

	/**
	 * Opens a DBRecord instance with the given single primary key. In contrast
	 * of openRecord(DBRowSet, Object) this method returns null if no matching
	 * record exists.
	 *
	 * @param table
	 *            the table to read the record from
	 * @param key
	 *            the primary key
	 * @return the DBRecord instance, can be null
	 */
	public DBRecord getRecord(final DBRowSet table, final Object key) {
		return openRecord(table, new Object[] { key });
	}

	/**
	 * Opens a DBRecord instance with the given multiple primary keys. In
	 * contrast of openRecord(DBRowSet, Object[]) this method returns null if no
	 * matching record exists.
	 *
	 * @param table
	 *            the table to read the record from
	 * @param keys
	 *            the primary keys array
	 * @return the DBRecord instance, can be null
	 */
	public DBRecord getRecord(final DBRowSet table, final Object[] keys) {

		class ReadRecordCallback implements ConnectionCallback<DBRecord> {
			public DBRecord doInConnection(Connection connection)
					throws SQLException, DataAccessException {
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

	/**
	 * Executes a given DBCommand query and maps each row to Class<T> object
	 * using DBReader.getBeanList method.
	 * 
	 * @see org.apache.empire.db.DBReader.getBeanList(C, Class<T>, int)
	 * 
	 * @param cmd
	 *            the query command
	 * @param c
	 *            the collection to add the objects to
	 * @param t
	 *            the class type of the objects in the list
	 * @param maxCount
	 *            the maximum number of objects
	 * 
	 * @return the list of T
	 */
	public <C extends Collection<T>, T> C queryForBeanList(final DBCommand cmd,
			final C c, final Class<T> t, final int maxCount) {

		class GetBeanListCallback implements DBReaderExtractor<C> {

			@Override
			public C process(DBReader reader) {
				return reader.getBeanList(c, t, maxCount);
			}
		}

		return query(cmd, new GetBeanListCallback());

	}

	/**
	 * Executes a given DBCommand query and maps each row to Class<T> object
	 * using DBReader.getBeanList method.
	 * 
	 * @see org.apache.empire.db.DBReader.getBeanList(Class<T>, int)
	 * 
	 * @param cmd
	 *            the query command
	 * @param t
	 *            the class type of the objects in the list
	 * @param maxCount
	 *            the maximum number of objects
	 * 
	 * @return the list of T
	 */
	public <T> List<T> queryForBeanList(DBCommand cmd, Class<T> t, int maxItems) {
		return queryForBeanList(cmd, new ArrayList<T>(), t, maxItems);
	}

	/**
	 * Executes a given DBCommand query and maps each row to Class<T> object
	 * using DBReader.getBeanList method.
	 * 
	 * @see org.apache.empire.db.DBReader.getBeanList(Class<T>)
	 * 
	 * @param cmd
	 *            the query command
	 * @param t
	 *            the class type of the objects in the list
	 * 
	 * @return the list of T
	 */

	public <T> List<T> queryForBeanList(DBCommand cmd, Class<T> t) {
		return queryForBeanList(cmd, t, -1);
	}

	/**
	 * Executes a given DBCommand query and maps a single row to Class<T> object
	 * using DBReader.getBeanList method.
	 * 
	 * @param cmd
	 *            the query command
	 * @param t
	 *            the class type of the object to return
	 * 
	 * @return the list of T
	 * @throws IncorrectResultSizeDataAccessException
	 *             if more than one result object has been found
	 */
	public <T> T queryForBean(DBCommand cmd, Class<T> t) {
		return DataAccessUtils.uniqueResult(queryForBeanList(cmd, t, -1));
	}

	/**
	 * Executes a ConnectionCallback. Delegates to the underlying JdbcTemplate.
	 * 
	 * @see 
	 *      org.springframework.jdbc.core.JdbcTemplate.execute(ConnectionCallback
	 *      <K>)
	 * 
	 * @param connectionCallback
	 * @return arbitrary object
	 */
	public <K> K execute(ConnectionCallback<K> connectionCallback) {
		return getJdbcTemplate().execute(connectionCallback);
	}

	private <K> K query(Connection connection, DBCommand command,
			DBReaderExtractor<K> callback) {
		DBReader reader = newDBReader();
		try {
			reader.open(command, connection);

			return callback.process(reader);

		} finally {
			reader.close();
		}
	}

	/**
	 * Creates a new DBReader instance from the readerFactory;
	 * 
	 * @return DBReader instance
	 */

	private DBReader newDBReader() {

		return this.readerFactory.getObject();
	}

	private static class DbRecordCallbackHandlerExtractor implements
			DBReaderExtractor<Object> {

		private final DBRecordCallbackHandler rowCallbackHandler;

		public DbRecordCallbackHandlerExtractor(
				DBRecordCallbackHandler rowCallbackHandler) {
			Assert.notNull(rowCallbackHandler, "RowCallbackHandler is required");
			this.rowCallbackHandler = rowCallbackHandler;
		}

		// @Override
		public Object process(DBReader reader) {
			try {
				while (reader.moveNext()) {
					this.rowCallbackHandler.processRecord(reader);
				}
				return null;
			} finally {
				reader.close();
			}
		}

	}

	private static class DbRecordMapperExtractor<K> implements
			DBReaderExtractor<List<K>> {

		private final DBRecordMapper<K> dataReader;

		public DbRecordMapperExtractor(DBRecordMapper<K> rowMapper) {
			Assert.notNull(rowMapper, "DataReader is required");
			this.dataReader = rowMapper;
		}

		// @Override
		public List<K> process(DBReader reader) {
			try {
				List<K> results = new ArrayList<K>();
				int rowNum = 0;

				while (reader.moveNext()) {
					results.add(this.dataReader.mapRecord(reader, rowNum));
					rowNum++;
				}

				return results;

			} finally {
				reader.close();
			}
		}

	}

}
