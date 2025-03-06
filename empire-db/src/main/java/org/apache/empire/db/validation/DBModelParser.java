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
package org.apache.empire.db.validation;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBRelation.DBReference;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBModelParser
{
    protected static final Logger log = LoggerFactory.getLogger(DBModelParser.class);

    /**
     * The remote Database
     * This will be populated by the ModelChecker
     */
    private static class RemoteDatabase extends DBDatabase
    {
        /*
         * Will be dynamically populated
         */
    }
    
    private static class RemoteView extends DBView
    {
        public RemoteView(String name, DBDatabase db)
        {
            super(name, db);
        }

        public DBColumn addColumn(String columnName, DataType dataType, double size, boolean dummy)
        {
            return super.addColumn(columnName, dataType, size);
        }
        
        @Override
        public DBCommandExpr createCommand()
        {
            throw new NotSupportedException(this, "createCommand");
        }
    }

    protected final String catalog;
    protected final String schema;

    protected final String remoteName;
    protected DBDatabase remoteDb = null;  /* will be recreated on every call to checkModel */

    protected final Map<String, DBRowSet> tableMap = new HashMap<String, DBRowSet>();
    
    private String standardIdentityColumnName  = null;
    private String standardTimestampColumnName = null;
    
    /**
     * Creates a new Model Checker
     * @param catalog the catalog
     * @param schema the schema
     */
    public DBModelParser(String catalog, String schema)
    {
        this.catalog = catalog;
        this.schema = schema;
        // set origin
        StringBuilder b = new StringBuilder();
        if (StringUtils.isNotEmpty(catalog))
            b.append(catalog);
        if (StringUtils.isNotEmpty(schema))
        {   if (b.length()>0)
                b.append(".");
            b.append(schema);
        }
        if (b.length()==0)
            b.append("[Unknown]");
        this.remoteName = b.toString();
    }

    public String getCatalog()
    {
        return catalog;
    }

    public String getSchema()
    {
        return schema;
    }

    public void setStandardIdentityColumnName(String standardIdentityColumnName)
    {
        this.standardIdentityColumnName = standardIdentityColumnName;
    }

    public void setStandardTimestampColumnName(String standardTimestampColumnName)
    {
        this.standardTimestampColumnName = standardTimestampColumnName;
    }

    /**
     * Returns the RemoteDatabase
     * Only available after parseModel() is called 
     * @return the remote Database
     */
    public DBDatabase getDatabase()
    {
        return remoteDb;
    }
    
    /**
     * This method is used to parse the populate the remote database
     * @param conn the connection for retrieving the remote database metadata
     */
    public void parseModel(Connection conn)
    {
        try
        {   // create remote db instance
            remoteDb = createRemoteDatabase();
            // populate
            DatabaseMetaData dbMeta = conn.getMetaData();
            populateRemoteDatabase(dbMeta);
        }
        catch (SQLException e)
        {
            log.error("checkModel failed for {}", remoteName);
            throw new InternalException(e);
        }
        finally 
        {   // cleanup
            tableMap.clear();            
        }
    }

    protected void populateRemoteDatabase(DatabaseMetaData dbMeta)
        throws SQLException
    {
        // collect tables & views
        int count = collectTablesAndViews(dbMeta, null);
        log.info("{} tables and views added for schema \"{}\"", count, remoteName);

        // Collect all columns
        count = collectColumns(dbMeta);
        log.info("{} columns added for schema \"{}\"", count, remoteName);

        // Collect PKs
        count = collectPrimaryKeys(dbMeta);
        log.info("{} primary keys added for schema \"{}\"", count, remoteName);

        // Collect FKs
        count = collectForeignKeys(dbMeta);
        log.info("{} foreign keys added for schema \"{}\"", count, remoteName);
    }

    /**
     * Checks if the tableName belongs to a system or hidden table
     * @param tableName the table name
     * @param tableMeta the table meta
     * @return true if the table is hidden or false otherwise
     */
    protected boolean isSystemTable(String tableName, ResultSet tableMeta)
    {   // system tables containing a '$' symbol (required for Oracle!)
        return (tableName.indexOf('$') >= 0);
    }
    
    /**
     * collects table and view information from database meta data
     * @param dbMeta the database meta data
     * @param tablePattern the table pattern
     * @throws SQLException thrown if a database access error occurs
     * @return the table and view count
     */
    protected int collectTablesAndViews(DatabaseMetaData dbMeta, String tablePattern)
        throws SQLException
    {
        tableMap.clear();
        ResultSet dbTables = dbMeta.getTables(catalog, schema, tablePattern, new String[] { "TABLE", "VIEW" });
        try {
            // ResultSet dbTables = dbMeta.getTables("PATOOL", "DBO", null, new String[] { "TABLE", "VIEW" });
            int count = 0;
            while (dbTables.next())
            {
                String tableName = dbTables.getString("TABLE_NAME");
                String tableType = dbTables.getString("TABLE_TYPE");
                if (isSystemTable(tableName, dbTables))
                {   // ignore system table
                    DBModelParser.log.info("Ignoring system table " + tableName);
                    continue;
                }
                if ("VIEW".equalsIgnoreCase(tableType))
                    addView(tableName);
                else
                    addTable(tableName);
                count++;
            }
            return count;
        } finally {
            dbTables.close();
        }
    }

    /**
     * collects column information from database meta data for each table
     * @param dbMeta the database meta data
     * @throws SQLException thrown if a database access error occurs
     * @return the column count
     */
    protected int collectColumns(DatabaseMetaData dbMeta)
            throws SQLException
    {
        int count = 0;
        for (DBRowSet t : getTables())
        {
            ResultSet dbColumns = dbMeta.getColumns(catalog, schema, t.getName(), null);
            try {
                while (dbColumns.next())
                {   // add the column
                    addColumn(t, dbColumns);
                    count++;
                }
            } finally {
                dbColumns.close();
            }
        }
        return count;
    }

    /**
     * collects column information from database meta data for whole schema
     * @param dbMeta the database meta data
     * @param tablePattern the table pattern
     * @throws SQLException thrown if a database access error occurs
     * @return the column count
     */
    protected int collectColumns(DatabaseMetaData dbMeta, String tablePattern)
        throws SQLException
    {
        ResultSet dbColumns = dbMeta.getColumns(catalog, schema, tablePattern, null);
        try {
            int count = 0;
            while (dbColumns.next())
            {
                String tableName = dbColumns.getString("TABLE_NAME");
                DBRowSet t = getTable(tableName);
                if (t == null)
                {   log.error("Table not found: {}", tableName);
                    continue;
                }
                addColumn(t, dbColumns);
                count++;
            }
            return count;
        } finally {
            dbColumns.close();
        }
    }
    
    /**
     * collects primary key information from database meta data
     * @param dbMeta the database meta data
     * @throws SQLException thrown if a database access error occurs
     * @return the primary key count
     */
    protected int collectPrimaryKeys(DatabaseMetaData dbMeta)
        throws SQLException
    {
        int count = 0;
        for (DBRowSet rs : getTables())
        {
            if (!(rs instanceof DBTable))
                continue; // not a table
            // read pk
            DBTable t = (DBTable)rs;
            List<String> pkCols = new ArrayList<String>();
            ResultSet primaryKeys = dbMeta.getPrimaryKeys(catalog, schema, t.getName());
            
            try {
                // get column names (in order)
                while (primaryKeys.next())
                {
                    String columnName = primaryKeys.getString("COLUMN_NAME");
                    int keyIndex = primaryKeys.getInt("KEY_SEQ")-1;
                    if (keyIndex<pkCols.size())
                    {   // insert to previously allocated position
                        pkCols.set(keyIndex, columnName);
                        continue;
                    }
                    else if (keyIndex>pkCols.size())
                    {   // alloc placeholders
                        while(keyIndex>pkCols.size())
                            pkCols.add(null); // placeholder
                    }
                    // append
                    pkCols.add(columnName);
                }
                // resolve columns from names
                if (pkCols.size() > 0)
                {
                    DBColumn[] keys = new DBColumn[pkCols.size()];
                    for (int i = 0; i < keys.length; i++)
                    {
                        keys[i] = t.getColumn(pkCols.get(i).toUpperCase());
                    }
                    t.setPrimaryKey(keys);
                    count++;
                }
            } finally {
                primaryKeys.close();
            }
        }
        return count;
    }

    /**
     * collects foreign key information from database meta data
     * @param dbMeta the database meta data
     * @throws SQLException thrown if a database access error occurs
     * @return the foreign key count
     */
    protected int collectForeignKeys(DatabaseMetaData dbMeta)
            throws SQLException
    {
        int count = 0;
        for (DBRowSet t : getTables())
        {
            if (t instanceof DBTable)
                count += collectForeignKeys(dbMeta, t.getName());
        }
        return count;
    }
    
    /**
     * collects foreign key information from database meta data
     * @param dbMeta the database meta data
     * @param tablePattern the table pattern
     * @throws SQLException thrown if a database access error occurs
     * @return the foreign key count
     */
    protected int collectForeignKeys(DatabaseMetaData dbMeta, String tablePattern)
        throws SQLException
    {
        ResultSet foreignKeys = dbMeta.getImportedKeys(catalog, schema, tablePattern);
        try {
            int count = 0;
            while (foreignKeys.next())
            {
                String fkTable = foreignKeys.getString("FKTABLE_NAME");
                String fkColumn = foreignKeys.getString("FKCOLUMN_NAME");

                String pkTable = foreignKeys.getString("PKTABLE_NAME");
                String pkColumn = foreignKeys.getString("PKCOLUMN_NAME");

                String fkName = foreignKeys.getString("FK_NAME");

                DBTableColumn c1 = (DBTableColumn) getTable(fkTable).getColumn(fkColumn.toUpperCase());
                DBTableColumn c2 = (DBTableColumn) getTable(pkTable).getColumn(pkColumn.toUpperCase());

                DBRelation relation = this.remoteDb.getRelation(fkName);
                if (relation == null)
                {
                    addRelation(fkName, c1.referenceOn(c2));
                    count++;
                }
                else
                {   // get existing references
                    DBReference[] refs = relation.getReferences();
                    // remove old
                    this.remoteDb.removeRelation(relation);
                    DBReference[] newRefs = new DBReference[refs.length + 1];
                    // copy existing
                    DBReference newRef = new DBReference(c1, c2);
                    for (int i = 0; i < refs.length; i++)
                    {
                        newRefs[i] = refs[i];
                    }
                    newRefs[newRefs.length - 1] = newRef;
                    addRelation(fkName, newRefs);
                }
            }
            return count;
        } finally {
            foreignKeys.close();
        }
    }

    /*
     * internal methods
     */
    protected final Collection<DBRowSet> getTables()
    {
        return this.tableMap.values();
    }
    
    protected final DBRowSet getTable(String tableName)
    {
        return this.tableMap.get(tableName.toUpperCase());
    }

    protected DBDatabase createRemoteDatabase()
    {
        return new RemoteDatabase();
    }
    
    protected void addTable(String tableName)
    {
        this.tableMap.put(tableName.toUpperCase(), new DBTable(tableName, this.remoteDb));
    }
    
    protected void addView(String viewName)
    {
        this.tableMap.put(viewName.toUpperCase(), new RemoteView(viewName, this.remoteDb));
    }
    
    protected void addRelation(String relName, DBReference... references)
    {
        this.remoteDb.addRelation(relName, references);
    }
    
    protected DBColumn addColumn(DBRowSet t, ResultSet rs)
        throws SQLException
    {
        String name = rs.getString("COLUMN_NAME");
        DataType empireType = getEmpireDataType(rs.getInt("DATA_TYPE"));

        // get Size
        double colSize = getColumnSize(empireType, rs);

        // mandatory field?
        boolean required = isColumnRequired(rs);
        Object defaultValue = getColumnDefault(rs);

        // Now add the column to table / view
        DBColumn col;
        if (t instanceof DBTable)
        {   // check Identity and Timestamp
            boolean timestampColumn = false;
            if (empireType==DataType.INTEGER && isIdentityColumn(rs))
                empireType= DataType.AUTOINC;
            if (empireType.isDate() && (timestampColumn=isTimestampColumn(rs)))
                empireType= DataType.TIMESTAMP;
            // Add Column
            col = ((DBTable)t).addColumn(name, empireType, colSize, required, defaultValue);
            // Set Timestamp
            if (empireType==DataType.AUTOINC)
                ((DBTable) t).setPrimaryKey(col);
            if (timestampColumn)
                t.setTimestampColumn(col);
            // info
            log.debug("Added table column {}.{} of type {}", t.getName(), name, empireType);
        }
        else if (t instanceof DBView)
        {
            col = ((RemoteView)t).addColumn(name, empireType, colSize, false);
            log.debug("Added view column {}.{} of type {}", t.getName(), name, empireType);
        }
        else
        {   // Unknown type
            log.error("Unknown Object Type {}", t.getClass().getName());
            col = null;
        }
        // done
        return col;
    }
    
    protected double getColumnSize(DataType empireType, ResultSet rs)
        throws SQLException
    {
        double colSize = rs.getInt("COLUMN_SIZE");
        if (empireType == DataType.DECIMAL || empireType == DataType.FLOAT)
        { // decimal digits
            int decimalDig = rs.getInt("DECIMAL_DIGITS");
            if (decimalDig > 0)
            {   try
                {   // concat and parse
                    int intSize = rs.getInt("COLUMN_SIZE");
                    colSize = Double.parseDouble(String.valueOf(intSize) + '.' + decimalDig);
                }
                catch (Exception e)
                {
                    String name = rs.getString("COLUMN_NAME");
                    DBModelParser.log.error("Failed to parse decimal digits for column " + name);
                }
            }
            // make integer?
            if (colSize < 1.0d)
            { // Turn into an integer
                empireType = DataType.INTEGER;
            }
        } 
        else if (empireType.isDate())
        {
            colSize = 0.0;
        }
        else if (empireType == DataType.INTEGER || empireType == DataType.CLOB || empireType == DataType.BLOB)
        {
            colSize = 0.0;
        }
        return colSize;
    }
    
    protected boolean isColumnRequired(ResultSet rs)
        throws SQLException
    {
        return rs.getString("IS_NULLABLE").equalsIgnoreCase("NO");
    }
    
    protected Object getColumnDefault(ResultSet rs)
        throws SQLException
    {
        return rs.getString("COLUMN_DEF");
    }
    
    protected boolean isIdentityColumn(ResultSet rs)
    {   try {
            return (standardIdentityColumnName!=null &&
                    standardIdentityColumnName.equalsIgnoreCase(rs.getString("COLUMN_NAME")));
        } catch(SQLException e) {
            return false;
        }
    }
    
    protected boolean isTimestampColumn(ResultSet rs)
    {   try {
            return (standardTimestampColumnName!=null &&
                    standardTimestampColumnName.equalsIgnoreCase(rs.getString("COLUMN_NAME")));
        } catch(SQLException e) {
            return false;
        }
    }

    protected DataType getEmpireDataType(int sqlType)
    {
        DataType empireType = DataType.UNKNOWN;
        switch (sqlType)
        {
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
            case Types.TIME:
                empireType = DataType.TIME;
                break;
            case Types.TIMESTAMP:
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
                DBModelParser.log.warn("SQL column type " + sqlType + " not supported.");
        }
        DBModelParser.log.debug("Mapping date type " + String.valueOf(sqlType) + " to " + empireType);
        return empireType;
    }
}
