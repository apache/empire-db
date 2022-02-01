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
package org.apache.empire.db.codegen;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.db.DBView.DBViewColumn;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to create a in memory DBDatabase of a given SQLConnection
 * and Configuration
 * 
 * @author Benjamin Venditti
 */
public class CodeGenParser {

	public static class InMemoryDatabase extends DBDatabase {
        // *Deprecated* private static final long serialVersionUID = 1L;
        
        @Override
        public List<DBRelation> getRelations()
        {
            return relations;
        }
	}
	
	public static class InMemoryView extends DBView {
    // *Deprecated* private static final long serialVersionUID = 1L;

		public InMemoryView(String name, DBDatabase db) {
			super(name, db);
		}
		
		public DBViewColumn addCol(String columnName,DataType dataType)
		{
			return addColumn(columnName, dataType);
		}
		
		@Override
		public DBCommandExpr createCommand() {
			return null;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(CodeGenParser.class);
	
	private DatabaseMetaData dbMeta;
	private Connection con;
	private CodeGenConfig config;

	/**
	 * create a empty in memory Database and populates it
	 */
	public CodeGenParser(CodeGenConfig config) {
	    this.config = config;
	}

	/**
	 * returns the populated DBDatabase
	 */
	public DBDatabase loadDbModel() {
		DBDatabase db = new InMemoryDatabase();
	    try {           
            con = openJDBCConnection(config);
            populateDatabase(db);
        } 
        catch (SQLException e) 
        {
            throw new RuntimeException("Unable to read database metadata: " + e.getMessage(), e);
        }
        finally 
        {
            close(con);
        }
        return db;
	}

	/**
     * Opens and returns a JDBC-Connection.
     * JDBC url, user and password for the connection are obained from the SampleConfig bean
     * Please use the config.xml file to change connection params.
     */
	protected Connection openJDBCConnection(CodeGenConfig config) throws SQLException{
        log.info("Connecting to Database'" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
        Connection conn = null;
        try {
            Class.forName(config.getJdbcClass()).newInstance();
        }catch(Exception ex){
        	throw new SQLException("Could not load database dbms: " + config.getJdbcClass());
        }
        conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
        log.info("Connected successfully");
        return conn;
    }

	/**
	 * Returns whether to add the table or ignore it
	 * @param tableName
	 */
    protected boolean isPopulateTable(String tableName)
    {
        if (tableName.indexOf('$') >= 0)
            return false;
        return true;
    }
    
    /**
     * Returns whether to add the column or ignore it
     * @param columnName
     */
    protected boolean isPopulateColumn(String columnName)
    {
        return true;
    }
	
	/**
	 * Queries the metadata of the database for tables and vies and populates the
	 * database with those
	 * @throws SQLException 
	 */
    protected void populateDatabase(DBDatabase db) throws SQLException {
		ResultSet tables = null;
		ArrayList<String> populatedTables=new ArrayList<String>();
		try{
            this.dbMeta = con.getMetaData();
            String[] tablePatterns = {null}; // Could be null, so start that way.
			if(config.getDbTablePattern() != null)
				tablePatterns = config.getDbTablePattern().split(","); // Support a comma separated list of table patterns (i.e. specify a list of table names in the config file).
            
            int tableCount = 0; // Moved to be outside table pattern loop.
            int viewCount = 0;
            for(String pattern : tablePatterns){
                // types
                String[] types = config.isGenerateViews() ? new String[] { "TABLE", "VIEW" }
                                                          : new String[] { "TABLE" }; 
			    // Get table metadata
	            tables = dbMeta.getTables(
			            config.getDbCatalog(), 
			            config.getDbSchema(), 
			            pattern == null ? pattern: pattern.trim(),
						types);
	            
	            // Add all tables and views 
				while (tables.next()) {
					String tableName = tables.getString("TABLE_NAME");
			        if (!isPopulateTable(tableName)) {
                        log.info("Ignoring table " + tableName);
			            continue;
			        }
					// show
					String tableType = tables.getString("TABLE_TYPE");
					String tableSchema = tables.getString("TABLE_SCHEM");
					String templ = StringUtils.isNotEmpty(tableSchema) ? "{0}: {1} ({2})" : "{0}: {1}"; 
				    log.info(MessageFormat.format(templ, tableType, tableName, tableSchema));
					// Table or View
					if(tableType.equalsIgnoreCase("VIEW")){
						InMemoryView view = new InMemoryView(tableName, db);
						populateView(view);
						viewCount++;
					} else {
						DBTable table = new DBTable(tableName, db);
						populateTable(table);
						populatedTables.add(tableName);
						tableCount++;
					}
				}
			}
			// Add all relations
			gatherRelations(db, dbMeta, populatedTables);

			if (tableCount==0 && viewCount==0) {
			    // getTables returned no result
			    String info = "catalog="+config.getDbCatalog(); 
                info += "/ schema="+config.getDbSchema(); 
                info += "/ pattern="+config.getDbTablePattern(); 
			    log.warn("DatabaseMetaData.getTables() returned no tables or views! Please check parameters: "+info);
				log.info("Available catalogs: " + getCatalogs(dbMeta));
				log.info("Available schemata: " + getSchemata(dbMeta));
			}
		} finally {
			close(tables);
		}
	}
	
    protected void gatherRelations(DBDatabase db, DatabaseMetaData dbMeta, ArrayList<String> tables) throws SQLException{
		ResultSet relations = null;
		String fkTableName, pkTableName, fkColName, pkColName, relName;
		DBTableColumn fkCol, pkCol;
		DBTable fkTable, pkTable;
		DBColumn col;
		
		// Add all Relations
		for (String tableName :tables) {
			
			// check for foreign-keys
			relations = dbMeta.getImportedKeys(config.getDbCatalog(), config .getDbSchema(), tableName);
			while (relations.next()) {
				pkCol=fkCol=null;
				
				fkTableName=relations.getString("FKTABLE_NAME");
				pkTableName=relations.getString("PKTABLE_NAME");
				fkColName=relations.getString("FKCOLUMN_NAME");
				pkColName=relations.getString("PKCOLUMN_NAME");

				// Detect relation name
				relName=relations.getString("FK_NAME");
				if (StringUtils.isEmpty(relName))
					relName=fkTableName+"."+fkColName+"-"+pkTableName+"."+pkColName;
				
				pkTable = db.getTable(pkTableName);
				fkTable = db.getTable(fkTableName);
				
				// check if both tables really exist in the model
				if(pkTable==null || fkTable==null){
					log.error("Unable to add the relation \""+relName+"\"! One of the tables could not be found.");
					continue;
				}
				
				col=pkTable.getColumn(pkColName);
				if(col instanceof DBTableColumn)
					pkCol = (DBTableColumn) col;
	
				col=fkTable.getColumn(fkColName);
				if(col instanceof DBTableColumn)
					fkCol = (DBTableColumn) col;
				
				// check if both columns really exist in the model
				if(fkCol==null || pkCol==null){
					log.error("Unable to add the relation \""+relName+"\"! One of the columns could not be found.");
					continue;
				}
				
				// add the relation
				DBRelation.DBReference reference = fkCol.referenceOn(pkCol);
				DBRelation.DBReference[] refs = null;
		    	DBRelation r = db.getRelation(relName);
		        if (r!=null) {
		        	DBRelation.DBReference[] refsOld = r.getReferences();
		        	refs = new DBRelation.DBReference[refsOld.length+1];
		        	int i=0;
		        	for (; i<refsOld.length; i++)
		        		refs[i]=refsOld[i];
	        		refs[i]=reference;
		        	// remove old relation
                    log.warn("Duplicate relation {}", r.getName());
	        		db.getRelations().remove(r);
		        } else {
		        	refs = new DBRelation.DBReference[] { reference };
		        }
				// Add a new relation
				db.addRelation(relName, refs);
				log.info("Added relation (FK-PK): "+relName);
			}
		}
	}

    protected String getCatalogs(DatabaseMetaData dbMeta) throws SQLException {
		String retVal = "";
		ResultSet rs = dbMeta.getCatalogs();
		while (rs.next()) {
			retVal += rs.getString("TABLE_CAT") + ", ";
		}
		if(retVal.length()>2)
			retVal=retVal.substring(0,retVal.length()-2);
		
		return retVal;
	}

    protected String getSchemata(DatabaseMetaData dbMeta) throws SQLException {
		String retVal = "";
		ResultSet rs = dbMeta.getSchemas();
		while (rs.next()) {
			retVal += rs.getString("TABLE_SCHEM") + ", ";
		}
		if(retVal.length()>2)
			retVal=retVal.substring(0,retVal.length()-2);
		return retVal;
	}

	/**
	 * queries the metadata for columns of a specific table and populates the
	 * table with that information
	 * @throws SQLException 
	 */
    protected void populateTable(DBTable t) throws SQLException {
		List<String> pkCols = this.findPkColumns(t.getName());
		String lockColName = config.getTimestampColumn();
		DBColumn[] keys = new DBColumn[pkCols.size()];
		ResultSet rs = null;
		try {
			rs = dbMeta.getColumns(config.getDbCatalog(), config.getDbSchema(),	t.getName(), null);
	        int i=0;
			while (rs.next()) {
				DBTableColumn c = addColumn(t, rs, lockColName);
				if (c==null)
				    continue;
				// check if it is a KeyColumn
				if (pkCols.contains(c.getName()))
					keys[i++] = c;
			}
	        // Check whether all key columns have been set
	        for (i=0; i<keys.length; i++)
	            if (keys[i]==null){
	            	throw new ItemNotFoundException(pkCols.get(i));
	            }
	        if(keys.length > 0){
	        	t.setPrimaryKey(keys);
	        }
		} finally {
			close(rs);
		}
	}
	
	/**
	 * queries the metadata for columns of a specific table and populates the
	 * table with that information
	 * @throws SQLException 
	 */
    protected void populateView(InMemoryView v) throws SQLException {
		ResultSet rs = null;
		try {
			rs = dbMeta.getColumns(config.getDbCatalog(), config.getDbSchema(),
					v.getName(), null);
			while (rs.next()) {
				addColumn(v, rs);
			}
		} finally {
			close(rs);
		}
	}

	/**
	 * Returns a list of column names that define the primarykey of the given
	 * table.
	 * @throws SQLException 
	 */
    protected List<String> findPkColumns(String tableName) throws SQLException {
		List<String> cols = new ArrayList<String>();
		ResultSet rs = null;
		try {
			rs = dbMeta.getPrimaryKeys(config.getDbCatalog(), config
					.getDbSchema(), tableName);
			while (rs.next()) {
				cols.add(rs.getString("COLUMN_NAME"));
			}
		} finally {
			close(rs);
		}
		return cols;
	}

	/**
	 * Adds DBColumn object to the given DBTable. The DBColumn is created from
	 * the given ResultSet
	 */
    protected DBTableColumn addColumn(DBTable t, ResultSet rs, String lockColName)
			throws SQLException {
		
	    String name = rs.getString("COLUMN_NAME");
	    if (!isPopulateColumn(name)) {
            log.info("Ignoring column " + name);
            return null;
	    }
	    
		DataType empireType = getEmpireDataType(rs.getInt("DATA_TYPE"));
		double colSize = getColumnSize(empireType, rs.getInt("DATA_TYPE"), rs.getInt("COLUMN_SIZE"));

		// Timestamp column
        if (empireType==DataType.DATETIME && StringUtils.isNotEmpty(lockColName) && name.equalsIgnoreCase(lockColName))
        {
            empireType=DataType.TIMESTAMP;
            colSize=0;
        }

        // Number column
		if (empireType==DataType.DECIMAL || empireType==DataType.FLOAT)
		{	// decimal digits
			int decimalDig = rs.getInt("DECIMAL_DIGITS");
			if (decimalDig>0)
			{	// parse
				try {
					int intSize = rs.getInt("COLUMN_SIZE");
					colSize = Double.parseDouble(String.valueOf(intSize)+'.'+decimalDig);
				} catch(Exception e) {
					log.error("Failed to parse decimal digits for column "+name);
				}
			}
			// make integer?
			if (colSize<1.0d)
			{	// Turn into an integer
				empireType=DataType.INTEGER;
			}
		}
		
		// mandatory field?
		boolean required = false;
		String defaultValue = rs.getString("COLUMN_DEF");
		if (rs.getString("IS_NULLABLE").equalsIgnoreCase("NO"))
			required = true;
		
		// The following is a hack for MySQL which currently gets sent a string "CURRENT_TIMESTAMP" from the Empire-db dbms for MySQL.
		// This will avoid the dbms problem because CURRENT_TIMESTAMP in the db will just do the current datetime.
		// Essentially, Empire-db needs the concept of default values of one type that get mapped to another.
		// In this case, MySQL "CURRENT_TIMESTAMP" for Types.TIMESTAMP needs to emit from the Empire-db dbms the null value and not "CURRENT_TIMESTAMP".
		if(rs.getInt("DATA_TYPE") == Types.TIMESTAMP && defaultValue != null && defaultValue.equals("CURRENT_TIMESTAMP")){
			required = false; // It is in fact not required even though MySQL schema is required because it has a default value. Generally, should Empire-db emit (required && defaultValue != null) to truly determine if a column is required?
			defaultValue = null; // If null (and required per schema?) MySQL will apply internal default value.
		}
		
		// AUTOINC indicator is not in java.sql.Types but rather meta data from DatabaseMetaData.getColumns()
		// getEmpireDataType() above is not enough to support AUTOINC as it will only return DataType.INTEGER
		DataType originalType = empireType;
		ResultSetMetaData metaData = rs.getMetaData();
		int colCount = metaData.getColumnCount();
		String colName;
		for (int i = 1; i <= colCount; i++) {
			colName = metaData.getColumnName(i);
			// MySQL matches on IS_AUTOINCREMENT column.
			// SQL Server matches on TYPE_NAME column with identity somewhere in the string value.
			if ((colName.equalsIgnoreCase("IS_AUTOINCREMENT") && rs.getString(i).equalsIgnoreCase("YES")) ||
					(colName.equals("TYPE_NAME") && rs.getString(i).matches(".*(?i:identity).*"))){
				empireType = DataType.AUTOINC;
				
			}
		}
		
		// Move from the return statement below so we can add
		// some AUTOINC meta data to the column to be used by
		// the ParserUtil and ultimately the template.
		log.info("\tCOLUMN:\t" + name + " ("+empireType+")");
		DBTableColumn col = t.addColumn(name, empireType, colSize, required, defaultValue);
		
		// We still need to know the base data type for this AUTOINC
		// because the Record g/setters need to know this, right?
		// So, let's add it as meta data every time the column is AUTOINC
		// and reference it in the template.
		if(empireType.equals(DataType.AUTOINC))
			col.setAttribute("AutoIncDataType", originalType);
		return col;
		
	}
	
	protected double getColumnSize(DataType empireType, int dataType, int columnSize) {
		return columnSize;
	}

	/**
	 * Adds DBColumn object to the given DBTable. The DBColumn is created from
	 * the given ResultSet
	 */
	private DBViewColumn addColumn(InMemoryView v, ResultSet rs)
			throws SQLException {
		String name = rs.getString("COLUMN_NAME");
		DataType empireType = getEmpireDataType(rs.getInt("DATA_TYPE"));
		
		log.info("\tCOLUMN:\t" + name + " ("+empireType+")");
		return v.addCol(name, empireType);
	}

	/**
	 * converts a SQL DataType to a EmpireDataType
	 */
	private DataType getEmpireDataType(int sqlType) {
		DataType empireType = DataType.UNKNOWN;
		switch (sqlType) {
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
		case Types.BIGINT:
			empireType = DataType.INTEGER;
			break;
		case Types.VARCHAR:
		case Types.NVARCHAR:
			empireType = DataType.VARCHAR;
			break;
		case Types.DATE:
			empireType = DataType.DATE;
			break;
		case Types.TIMESTAMP:
		case Types.TIME:
			empireType = DataType.DATETIME;
			break;
		case Types.CHAR:
        case Types.NCHAR:
			empireType = DataType.CHAR;
			break;
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.REAL:
			empireType = DataType.FLOAT;
			break;
		case Types.DECIMAL:
		case Types.NUMERIC:
			empireType = DataType.DECIMAL;
			break;
		case Types.BIT:
		case Types.BOOLEAN:
			empireType = DataType.BOOL;
			break;
		case Types.CLOB:
		case Types.NCLOB:
		case Types.LONGVARCHAR:
		case Types.LONGNVARCHAR:
			empireType = DataType.CLOB;
			break;
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			empireType = DataType.BLOB;
			break;
		default:
			empireType = DataType.UNKNOWN;
			log.warn("SQL column type " + sqlType + " not supported.");
		}
		log.debug("Mapping date type " + String.valueOf(sqlType) + " to "
				+ empireType);
		return empireType;
	}

    /**
     * Closes a sql resultset and logs exceptions.
     * 
     * @param rs the resultset to close
     * @param log the logger instance to use for logging
     * @return true on succes
     */
    protected boolean close(ResultSet rs)
    {   try {
            if (rs != null)
                rs.close();
            return true;
        } catch (SQLException e) {
            log.error("The resultset could not be closed!", e);
            return false;
        }
    }

    /**
     * Closes a JDBC-Connection and logs exceptions.
     */
    protected void close(Connection conn)
    {   try  {
            log.info("Closing database connection");
            if (conn != null)
                conn.close();
        } catch (Exception e) {
            log.error("Error closing connection", e);
        }
    }
	
}
