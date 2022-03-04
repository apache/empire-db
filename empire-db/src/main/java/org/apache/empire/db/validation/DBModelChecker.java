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
import java.util.List;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBRelation.DBReference;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.exceptions.ObjectNotValidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBModelChecker
{
    private static final Logger log = LoggerFactory.getLogger(DBModelChecker.class);
    
    protected final DBModelParser modelParser;

    protected DBDatabase remoteDb = null; 
            
    /**
     * Creates a new Model Checker
     * @param catalog
     * @param schemaPattern
     */
    public DBModelChecker(DBModelParser modelParser)
    {
        this.modelParser = modelParser;
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
        modelParser.parseModel(conn);
        // set remote
        this.remoteDb = modelParser.getDatabase();
        // check database
        checkRemoteAgainst(db, handler);
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
        DBTable remoteTable = remoteDb.getTable(table.getName()); // getTable(table.getName());
        if (remoteTable == null)
        {   // Check if it is a view instead
            if (remoteDb.getView(table.getName())!=null)
                handler.objectTypeMismatch(remoteTable, table.getName(), DBTable.class);
            else
                handler.itemNotFound(table);
            return;
        }
        // Check primary Key
        checkPrimaryKey(table, remoteTable, handler);
        // check foreign keys
        checkForeignKeys(table, remoteTable, handler);
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

    protected void checkView(DBView view, DBModelErrorHandler handler)
    {
        DBView remoteView = remoteDb.getView(view.getName());
        if (remoteView == null)
        {   // Check if it is a table instead
            if (remoteDb.getTable(view.getName())!=null)
                handler.objectTypeMismatch(remoteView, view.getName(), DBView.class);
            else
                handler.itemNotFound(view);
            return;
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
            case TIME:
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

}
