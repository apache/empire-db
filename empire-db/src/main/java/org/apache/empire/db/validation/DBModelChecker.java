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
import java.sql.ResultSetMetaData;
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
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBModelChecker
{
    private static final Logger log = LoggerFactory.getLogger(DBModelChecker.class);

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
    protected final String schemaPattern;

    protected final String remoteName;
    protected DBDatabase remoteDb = null;  /* will be recreated on every call to checkModel */

    protected final Map<String, DBRowSet> tableMap = new HashMap<String, DBRowSet>();
    
    /**
     * Creates a new Model Checker
     * @param catalog
     * @param schemaPattern
     */
    public DBModelChecker(String catalog, String schemaPattern)
    {
        this.catalog = catalog;
        this.schemaPattern = schemaPattern;
        // set origin
        StringBuilder b = new StringBuilder();
        if (StringUtils.isNotEmpty(catalog))
            b.append(catalog);
        if (StringUtils.isNotEmpty(schemaPattern))
        {   if (b.length()>0)
                b.append(".");
            b.append(schemaPattern);
        }
        if (b.length()==0)
            b.append("[Unknown]");
        this.remoteName = b.toString();
    }

    /**
     * Returns the RemoteDatabase
     * Only available after parseModel() is called 
     * @return the remote Database
     */
    public DBDatabase getRemoteDatabase()
    {
        return remoteDb;
    }
    
    /**
     * Populates the remote database and compares it against the given database
     * @param db the Database to be checked
     * @param conn the connection for retrieving the remote database metadata
     * @param handler the handler that is called to handle inconsistencies
     */
    public void checkModel(DBDatabase db, Connection conn, DBModelErrorHandler handler)
    {
        // parse first
        parseModel(conn);
        // check database
        checkRemoteAgainst(db, handler);
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
     * @param dbMeta
     * @param dbSchema
     * @throws SQLException
     */
    protected int collectTablesAndViews(DatabaseMetaData dbMeta, String tablePattern)
        throws SQLException
    {
        tableMap.clear();
        ResultSet dbTables = dbMeta.getTables(catalog, schemaPattern, tablePattern, new String[] { "TABLE", "VIEW" });
        try {
            // ResultSet dbTables = dbMeta.getTables("PATOOL", "DBO", null, new String[] { "TABLE", "VIEW" });
            int count = 0;
            while (dbTables.next())
            {
                String tableName = dbTables.getString("TABLE_NAME");
                String tableType = dbTables.getString("TABLE_TYPE");
                if (isSystemTable(tableName, dbTables))
                {   // ignore system table
                    DBModelChecker.log.info("Ignoring system table " + tableName);
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
     */
    protected int collectColumns(DatabaseMetaData dbMeta)
            throws SQLException
    {
        int count = 0;
        for (DBRowSet t : getTables())
        {
            ResultSet dbColumns = dbMeta.getColumns(catalog, schemaPattern, t.getName(), null);
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
     */
    protected int collectColumns(DatabaseMetaData dbMeta, String tablePattern)
        throws SQLException
    {
        ResultSet dbColumns = dbMeta.getColumns(catalog, schemaPattern, tablePattern, null);
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
     * @param dbMeta
     * @param dbSchema
     * @throws SQLException
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
            ResultSet primaryKeys = dbMeta.getPrimaryKeys(catalog, schemaPattern, t.getName());
            try {
                while (primaryKeys.next())
                {
                    pkCols.add(primaryKeys.getString("COLUMN_NAME"));
                }
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
     * @throws SQLException
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
     * @throws SQLException
     */
    protected int collectForeignKeys(DatabaseMetaData dbMeta, String tablePattern)
        throws SQLException
    {
        ResultSet foreignKeys = dbMeta.getImportedKeys(catalog, schemaPattern, tablePattern);
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

    /**
     * Check the remote database against an existing model
     * @param db the database to check the remote against
     * @param handler
     */
    public void checkRemoteAgainst(DBDatabase db, DBModelErrorHandler handler)
    {
        if (this.remoteDb==null)
        {   // parseModel has not been called
            throw new ObjectNotValidException(this);
        }
        
        // check Tables
        for (DBTable table : db.getTables())
        {
            checkTable(table, handler);
        }

        // check Views
        for (DBView view : db.getViews())
        {
            checkView(view, handler);
        }
    }

    protected void checkTable(DBTable table, DBModelErrorHandler handler)
    {
        DBRowSet remoteTable = getTable(table.getName());
        if (remoteTable == null)
        {   // Not found
            handler.itemNotFound(table);
            return;
        }
        if (!(remoteTable instanceof DBTable))
        {   // Not a DBTable
            handler.objectTypeMismatch(remoteTable, table.getName(), DBTable.class);
            return;
        }
        // Check primary Key
        checkPrimaryKey(table, (DBTable)remoteTable, handler);
        // check foreign keys
        checkForeignKeys(table, (DBTable)remoteTable, handler);
        // Check Columns
        for (DBColumn column : table.getColumns())
        {
            DBColumn remoteColumn = remoteTable.getColumn(column.getName());
            if (remoteColumn == null)
            {
                handler.itemNotFound(column);
                continue;
            }
            checkColumn(column, remoteColumn, handler);
        }
    }

    protected void checkPrimaryKey(DBTable table, DBTable remoteTable, DBModelErrorHandler handler)
    {
        if (table.getPrimaryKey() == null)
        {
            // no primary key defined
            return;
        }

        if (remoteTable.getPrimaryKey() == null)
        {
            // primary key missing in DB
            handler.itemNotFound(table.getPrimaryKey());
            return;
        }

        DBColumn[] pk = table.getPrimaryKey().getColumns();
        DBColumn[] remotePk = remoteTable.getPrimaryKey().getColumns();

        pkColLoop: for (DBColumn pkCol : pk)
        {
            for (DBColumn remotePkCol : remotePk)
            {
                if (pkCol.getFullName().equalsIgnoreCase(remotePkCol.getFullName()))
                {
                    // found
                    continue pkColLoop;
                }
            }
            // PK-Column not found
            handler.primaryKeyColumnMissing(table.getPrimaryKey(), pkCol);
        }
    }

    protected void checkForeignKeys(DBTable table, DBTable remoteTable, DBModelErrorHandler handler)
    {
        if (table.getForeignKeyRelations().isEmpty())
        {
            // no foreign keys defined
            return;
        }

        List<DBRelation> relations = table.getForeignKeyRelations();
        List<DBRelation> remoteRelations = remoteTable.getForeignKeyRelations();

        for (DBRelation relation : relations)
        {
            referenceLoop: for (DBReference reference : relation.getReferences())
            {
                if (reference.getTargetColumn().getRowSet() instanceof DBTable)
                {
                    DBTable targetTable = (DBTable) reference.getTargetColumn().getRowSet();
                    DBTableColumn targetColumn = reference.getTargetColumn();
                    if (!targetTable.getPrimaryKey().contains(targetColumn))
                    {
                        DBModelChecker.log.info("The column "
                                                        + targetColumn.getName()
                                                        + " of foreign key {} is not a primary key of table {} and cant be checked because of a limitation in JDBC",
                                                relation.getName(), targetTable.getName());
                        continue;
                    }
                }

                for (DBRelation remoteRelation : remoteRelations)
                {
                    for (DBReference remoteReference : remoteRelation.getReferences())
                    {
                        if (reference.getSourceColumn().getFullName().equalsIgnoreCase(remoteReference.getSourceColumn().getFullName())
                            && reference.getTargetColumn().getFullName().equalsIgnoreCase(remoteReference.getTargetColumn().getFullName()))
                        {
                            // found
                            continue referenceLoop;
                        }
                    }

                }
                // Not found
                handler.itemNotFound(relation);
                break referenceLoop;
            }

        }

    }

    protected void checkView(DBView view, DBModelErrorHandler handler)
    {
        DBRowSet remoteView = getTable(view.getName());
        if (remoteView == null)
        {
            handler.itemNotFound(view);
            return;
        }
        if (!(remoteView instanceof DBView))
        {
            handler.objectTypeMismatch(remoteView, view.getName(), DBView.class);
        }
        for (DBColumn column : view.getColumns())
        {
            DBColumn remoteColumn = remoteView.getColumn(column.getName());
            if (remoteColumn == null)
            {
                handler.itemNotFound(column);
                continue;
            }
            // checkColumn(column, remoteColumn, handler);
            checkColumnType(column, remoteColumn, handler);
        }
    }

    protected void checkColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        switch (column.getDataType())
        {
            case UNKNOWN:
                checkUnknownColumn(column, remoteColumn, handler);
                break;
            case INTEGER:
                checkIntegerColumn(column, remoteColumn, handler);
                break;
            case AUTOINC:
                checkAutoIncColumn(column, remoteColumn, handler);
                break;
            case VARCHAR:
                checkTextColumn(column, remoteColumn, handler);
                break;
            case DATE:
            case DATETIME:
            case TIMESTAMP:
                checkDateColumn(column, remoteColumn, handler);
                break;
            case CHAR:
                checkCharColumn(column, remoteColumn, handler);
                break;
            case FLOAT:
                checkFloatColumn(column, remoteColumn, handler);
                break;
            case DECIMAL:
                checkDecimalColumn(column, remoteColumn, handler);
                break;
            case BOOL:
                checkBoolColumn(column, remoteColumn, handler);
                break;
            case CLOB:
                checkClobColumn(column, remoteColumn, handler);
                break;
            case BLOB:
                checkBlobColumn(column, remoteColumn, handler);
                break;
            case UNIQUEID:
                checkUniqueIdColumn(column, remoteColumn, handler);
                break;
            default:
                throw new RuntimeException("Invalid DataType " + column.getDataType());
        }

    }

    private void checkGenericColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkColumnType(column, remoteColumn, handler);
        checkColumnNullable(column, remoteColumn, handler);
        checkColumnSize(column, remoteColumn, handler);
    }

    protected void checkColumnType(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        if (column.getDataType() != remoteColumn.getDataType())
        {
            handler.columnTypeMismatch(column, remoteColumn.getDataType());
        }
    }

    protected void checkColumnNullable(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        if (column.isRequired() && !remoteColumn.isRequired())
        {
            handler.columnNullableMismatch(column, remoteColumn.isRequired());
        }
    }

    protected void checkColumnSize(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        int colSize = (int) column.getSize();
        if (colSize== 0)
        {   // When size is 0, don't check
            return; 
        }
        if (colSize != (int) remoteColumn.getSize())
        {
            handler.columnSizeMismatch(column, (int) remoteColumn.getSize(), 0);
        }
    }

    /** empire-db DataType-specific checker **/
    protected void checkUnknownColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkGenericColumn(column, remoteColumn, handler);
    }

    protected void checkIntegerColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkGenericColumn(column, remoteColumn, handler);
    }

    protected void checkAutoIncColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkColumnSize(column, remoteColumn, handler);
        checkColumnNullable(column, remoteColumn, handler);
    }

    protected void checkTextColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkGenericColumn(column, remoteColumn, handler);
    }

    protected void checkDateColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        // check nullable
        checkColumnNullable(column, remoteColumn, handler);

        // check type
        if (!remoteColumn.getDataType().isDate())
        {
            handler.columnTypeMismatch(column, remoteColumn.getDataType());
        }
    }

    protected void checkCharColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkGenericColumn(column, remoteColumn, handler);
    }

    protected void checkFloatColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkGenericColumn(column, remoteColumn, handler);
    }

    protected void checkDecimalColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkGenericColumn(column, remoteColumn, handler);

        // check scale
        if (column instanceof DBTableColumn && remoteColumn instanceof DBTableColumn)
        {
            DBTableColumn tableColumn = (DBTableColumn) column;
            DBTableColumn tableRemoteColumn = (DBTableColumn) remoteColumn;

            if (tableColumn.getDecimalScale() != tableRemoteColumn.getDecimalScale())
            {
                handler.columnSizeMismatch(column, (int) remoteColumn.getSize(), tableRemoteColumn.getDecimalScale());
            }
        }

    }

    protected void checkBoolColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        // Dont check size
        checkColumnType(column, remoteColumn, handler);
        checkColumnNullable(column, remoteColumn, handler);
    }

    protected void checkBlobColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        // Dont check size
        checkColumnType(column, remoteColumn, handler);
        checkColumnNullable(column, remoteColumn, handler);
    }

    protected void checkClobColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        // Dont check size
        checkColumnType(column, remoteColumn, handler);
        checkColumnNullable(column, remoteColumn, handler);
    }

    protected void checkUniqueIdColumn(DBColumn column, DBColumn remoteColumn, DBModelErrorHandler handler)
    {
        checkGenericColumn(column, remoteColumn, handler);
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
                    DBModelChecker.log.error("Failed to parse decimal digits for column " + name);
                }
            }
            // make integer?
            if (colSize < 1.0d)
            { // Turn into an integer
                empireType = DataType.INTEGER;
            }
        } 
        else if (empireType == DataType.INTEGER || empireType == DataType.CLOB || empireType == DataType.BLOB)
        {
            colSize = 0.0;
        }

        // mandatory field?
        boolean required = false;
        String defaultValue = rs.getString("COLUMN_DEF");
        if (rs.getString("IS_NULLABLE").equalsIgnoreCase("NO"))
        {
            required = true;
        }

        // The following is a hack for MySQL which currently gets sent a string "CURRENT_TIMESTAMP" from the Empire-db dbms for MySQL.
        // This will avoid the dbms problem because CURRENT_TIMESTAMP in the db will just do the current datetime.
        // Essentially, Empire-db needs the concept of default values of one type that get mapped to another.
        // In this case, MySQL "CURRENT_TIMESTAMP" for Types.TIMESTAMP needs to emit from the Empire-db dbms the null value and not "CURRENT_TIMESTAMP".
        if (rs.getInt("DATA_TYPE") == Types.TIMESTAMP && defaultValue != null && defaultValue.equals("CURRENT_TIMESTAMP"))
        {
            required = false; // It is in fact not required even though MySQL schema is required because it has a default value. Generally, should Empire-db emit (required && defaultValue != null) to truly determine if a column is required?
            defaultValue = null; // If null (and required per schema?) MySQL will apply internal default value.
        }

        // AUTOINC indicator is not in java.sql.Types but rather meta data from DatabaseMetaData.getColumns()
        // getEmpireDataType() above is not enough to support AUTOINC as it will only return DataType.INTEGER
        DataType originalType = empireType;
        ResultSetMetaData metaData = rs.getMetaData();
        int colCount = metaData.getColumnCount();
        String colName;
        for (int i = 1; i <= colCount; i++)
        {
            colName = metaData.getColumnName(i);
            // MySQL matches on IS_AUTOINCREMENT column.
            // SQL Server matches on TYPE_NAME column with identity somewhere in the string value.
            if ((colName.equalsIgnoreCase("IS_AUTOINCREMENT") && rs.getString(i).equalsIgnoreCase("YES"))
                || (colName.equals("TYPE_NAME") && rs.getString(i).matches(".*(?i:identity).*")))
            {
                empireType = DataType.AUTOINC;

            }
        }

        // Move from the return statement below so we can add
        // some AUTOINC meta data to the column to be used by
        // the ParserUtil and ultimately the template.
        //        DBModelChecker.log.info("\tCOLUMN:\t" + name + " (" + empireType + ")");
        DBColumn col;
        if (t instanceof DBTable)
        {
            col = ((DBTable)t).addColumn(name, empireType, colSize, required, defaultValue);
            // We still need to know the base data type for this AUTOINC
            // because the Record g/setters need to know this, right?
            // So, let's add it as meta data every time the column is AUTOINC
            // and reference it in the template.
            if (empireType.equals(DataType.AUTOINC))
            {
                col.setAttribute("AutoIncDataType", originalType);
            }
        }
        else if (t instanceof RemoteView)
        {
           col = ((RemoteView)t).addColumn(name, empireType, colSize, false);
        }
        else
        {   // Unknown type
            log.error("Unknown Object Type {}", t.getClass().getName());
            col = null;
        }
        // done
        return col;

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
                DBModelChecker.log.warn("SQL column type " + sqlType + " not supported.");
        }
        DBModelChecker.log.debug("Mapping date type " + String.valueOf(sqlType) + " to " + empireType);
        return empireType;
    }
}
