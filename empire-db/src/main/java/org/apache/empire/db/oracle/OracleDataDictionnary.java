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
package org.apache.empire.db.oracle;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBView;


public class OracleDataDictionnary {
    
    /**
     * Immutable column info helper class
     */
    public class ColumnInfo {

        private final String dataType;
        private final int charLength;
        private final int dataLength;
        private final int dataPrecision;
        private final int dataScale;
        private final String nullable;
        
        public ColumnInfo(String dataType, int charLength, int dataLength, int dataPrecision, int dataScale, String nullable)
        {
            super();
            this.dataType = dataType;
            this.charLength = charLength;
            this.dataLength = dataLength;
            this.dataPrecision = dataPrecision;
            this.dataScale = dataScale;
            this.nullable = nullable;
        }

        public int getCharLength()
        {
            return charLength;
        }

        public int getDataLength()
        {
            return dataLength;
        }

        public int getDataPrecision()
        {
            return dataPrecision;
        }

        public int getDataScale()
        {
            return dataScale;
        }

        public String getDataType()
        {
            return dataType;
        }

        public String getNullable()
        {
            return nullable;
        }

    }
    
    protected static final Logger log = LoggerFactory.getLogger(OracleDataDictionnary.class);
    
    private final HashMap<String, HashMap<String, ColumnInfo>> dictionnary = new HashMap<String, HashMap<String, ColumnInfo>>();
    
    private Map<String, DataType[]> dataTypeMapping    = null;

    /**
     * Defines mapping of the Empire-db data types with the oracle data types.
     */
    public OracleDataDictionnary() {
        dataTypeMapping = new HashMap<String, DataType[]>();
        dataTypeMapping.put("VARCHAR2", new DataType[] { DataType.TEXT });
        dataTypeMapping.put("CHAR",     new DataType[] { DataType.CHAR, DataType.BOOL });
        dataTypeMapping.put("NUMBER",   new DataType[] { DataType.DECIMAL, DataType.DOUBLE, 
                                                         DataType.INTEGER, DataType.AUTOINC, DataType.BOOL });
        dataTypeMapping.put("DATE",     new DataType[] { DataType.DATE, DataType.DATETIME });
        dataTypeMapping.put("CLOB",     new DataType[] { DataType.CLOB });
        dataTypeMapping.put("BLOB",     new DataType[] { DataType.BLOB });
    }
    
    public HashMap<String, HashMap<String, ColumnInfo>> getDictionnary() {
        return dictionnary;
    }


    public void fillDataDictionnary(String tableName, String columnName, String dataType, int charLength, int dataLength, int dataPrecision, int dataScale, String nullable) {
        ColumnInfo colInfo = new ColumnInfo(dataType, charLength, dataLength, dataPrecision, dataScale, nullable);
        HashMap<String, ColumnInfo> columns = new HashMap<String, ColumnInfo>();
        
        if(dictionnary.containsKey(tableName)) {
            columns = dictionnary.get(tableName);
        }
        columns.put(columnName, colInfo);
        dictionnary.put(tableName, columns);
    }
    
    /**
     * Checks if the ORACLE datatype can be mapped with a DBTable data type.
     * 
     * @param dbDatatype ORACLE datatype
     * @param colDataType DBTableColumn datatype
     * @return true if the type can be mapped, false otherwise
     */
    private boolean checkMapping(String dbDatatype, DataType colDataType)
    {
        DataType[] colTypes = dataTypeMapping.get(dbDatatype);
        if(colTypes == null) 
        {
            log.warn("MAPPING NOT DEFINED FOR " + dbDatatype + " -> " + colDataType);
            return false;
        }
        for (int i = 0; i < colTypes.length; i++)
        {
            if (colTypes[i] == colDataType)
                return true;
        }
        return false;
    }
    
    /*
     * Checks a DBTableColumn definition. The set attributes must fint to the overgiven db attributes.
     * 
     * @param col Column to check
     * @param dbDataType Datatype of db column
     * @param dbDataLength Datalength of db column
     * @param dbDataPrecision Data precision of db column
     * @param dbDataScale Data scale of db column
     * @param dbRequired Is nullable of the db column
     * @return true if the column definition fits, false otherwise

    private boolean checkColumnDefinition(DBColumn col, String dbDataType, int dbDataLength, int dbDataPrecision,
                                          int dbDataScale, boolean dbRequired)
    {
        // FUTURE find a way to check the precision for numbers

        boolean result = true;
        DataType colDataType = col.getDataType();
        int size = (int) col.getSize();

        // check if the column data type can be mapped with the db column data type
        if (checkMapping(dbDataType, colDataType) == false)
        {
            log.warn("WRONG DATATYPE \t\t\t\t\t\t: [" + col.getRowSet().getName() + "][" + col.getName() + "] -> DB : ["
                           + dbDataType + "]" + "[" + dbDataLength + "]");
            result = false;
        }

        // check if the column is required and if the column is defined as required
        if (dbRequired && col.isRequired() == false)
        {
            log.warn("COLUMN IS REQUIRED \t\t\t\t\t: [" + col.getRowSet().getName() + "][" + col.getName() + "]");
            result = false;
        } 
        else if (dbRequired == false && col.isRequired() == true)
        {
            log.warn("COLUMN IS NOT REQUIRED \t\t\t\t: [" + col.getRowSet().getName() + "][" + col.getName() + "]");
            result = false;
        }

        // check the data length if the column is a varchar2
        if (dbDataType.equals("VARCHAR2") && (dbDataLength != size))
        {
            log.warn("WRONG COLUMN SIZE \t\t\t\t\t: [" + col.getRowSet().getName() + "][" + col.getName() + "] -> DB : ["
                           + dbDataType + "][" + dbDataLength + "]");
            result = false;
        }

        return result;
    }
     */
    
    private void checkColumn(DBColumn column, ColumnInfo colInfo)
    {
        if(checkMapping(colInfo.getDataType(), column.getDataType()) == false) {
            log.warn("WRONG DATA TYPE: \t" + column.getFullName() + " is set to " + column.getDataType() +
                     " instead of " + colInfo.getDataType());
        }
        if(colInfo.getDataType().equals("VARCHAR2") && (column.getSize() != colInfo.getCharLength())) {
            log.warn("WRONG COLUMN SIZE: \t" + column.getFullName() + " is set to " + (int)column.getSize() +
                     " instead of " + colInfo.getCharLength()); 
        }    
        if(column.isRequired() != colInfo.getNullable().equals("N")) {
            log.warn("WRONG NULLABLE FLAG: \t" + column.getFullName() + " is set to " + column.isRequired() +
                     " instead of " + colInfo.getNullable().equals("N")); 
        }
    }
    
    public void checkDBTableDefinition(List<DBTable> dbTables) {
        // go through all tables defined in the Java code
        for(DBTable currentTable: dbTables) {
            String dbTableName = currentTable.getName();
            // check if the table name is in the data dictionnary
            if(this.dictionnary.containsKey(dbTableName)) {
                // go through all columns of the table
                for(DBColumn currentColumn : currentTable.getColumns()) {
                    Collection<String> dictColumns = this.dictionnary.get(dbTableName).keySet();
                    // check if the column name is in the data dictionnary
                    if(dictColumns.contains(currentColumn.getName())) {
                        // go through all columns of the table in the data dictionnary
                        for(String dictColumn : dictColumns) {
                            // compare the columnnames
                            if(currentColumn.getName().equals(dictColumn)) {
                                ColumnInfo colInfo = this.dictionnary.get(currentTable.getName()).get(dictColumn);
                                checkColumn(currentColumn, colInfo);
                            }
                        }
                    }
                    else {
                        log.warn("MISSING COLUMN: \t" + currentColumn.getFullName() + " does not exist in database"); 
                    }
                }
            }
            else {
                log.warn("MISSING TABLE: \t" + currentTable.getName() + " does not exist in database"); 
            }
        }
    }
    
    public void checkDBViewDefinition(List<DBView> dbViews) {
//      go through all views defined in the Java code
        for(DBView currentView: dbViews) {
            String dbviewName = currentView.getName();
            // check if the view name is in the data dictionnary
            if(this.dictionnary.containsKey(dbviewName)) {
                // go through all columns of the table
                for(DBColumn currentColumn : currentView.getColumns()) {
                    Collection<String> dictColumns = this.dictionnary.get(dbviewName).keySet();
                    // check if the column name is in the data dictionnary
                    if(dictColumns.contains(currentColumn.getName())) {
                        // go through all columns of the view in the data dictionnary
                        for(String dictColumn : dictColumns) {
                            // compare the columnnames                           
                            if(currentColumn.getName().equals(dictColumn)) {
                                ColumnInfo colInfo = this.dictionnary.get(currentView.getName()).get(dictColumn);
                                if(checkMapping(colInfo.getDataType(), currentColumn.getDataType()) == false) {
                                    log.warn("WRONG DATA TYPE: \t" + currentColumn.getFullName() + " is set to " + currentColumn.getDataType() +
                                             " instead of " + colInfo.getDataType());
                                }
                            }
                        }
                    }
                    else {
                        log.warn("MISSING COLUMN: \t" + currentColumn.getFullName() + " does not exist in database."); 
                    }
                }
            }
            else {
                log.warn("MISSING VIEW: \t" + currentView.getName() + " does not exist in database."); 
            }
        }
    }
    
}
