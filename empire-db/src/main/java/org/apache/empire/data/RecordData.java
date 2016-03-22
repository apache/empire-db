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
package org.apache.empire.data;

import java.util.Collection;

/**
 * The RecordData interface provides methods for accessing data and context specific metadata.
 * <P>
 * The Record interface is implemented by the classes {@link org.apache.empire.db.DBReader}
 * and {@link org.apache.empire.db.DBRecord}.
 * <P>
 */
public interface RecordData
{
    /**
     * returns the number of field available
     * @return field count
     */
    int getFieldCount();

    /**
     * returns the index of the given column expression
     * Indexed operations provide better performance for bulk processing  
     * @param column the column for which to return the index
     * @return the field index of the given column
     */
    int getFieldIndex(ColumnExpr column);
    
    /**
     * returns the index of the column expression with the given name
     * @param column the name of the column for which to return the index
     * @return the field index of the given column
     */
    int getFieldIndex(String column);
    
    /**
     * returns the column expression for a given column
     * This is the reverse operation of getFieldIndex()
     * @param i field index of the column expression
     * @return the column expression object or null if the index is out of range
     */
    ColumnExpr getColumnExpr(int i);

    /**
     * returns the value of the field at the given index position 
     * Indexed operations provide better performance for bulk processing compared to getValue(ColumnExpr)  
     * @param index the field index for which to return the value
     * @return the record value for the given field
     */
    Object getValue(int index);
    
    /**
     * returns the record value for a particular column 
     * @param column the column for which to return the value
     * @return the record value for the given column
     */
    Object getValue(ColumnExpr column);
    
    /**
     * checks if the field at the given index position contains no value (null) 
     * Indexed operations provide better performance for bulk processing compared to isNull(ColumnExpr)  
     * @param index the field index
     * @return true if the field value is null or false otherwise
     */
    boolean isNull(int index);

    /**
     * checks if the record contains no value (null) for the given column  
     * @param column the column
     * @return true if the value for the column is null or false otherwise
     */
    boolean isNull(ColumnExpr column);

    // ------- Java Bean Support -------

    /**
     * copies all field values into a static Java Bean.
     * <P>
     * In order to map column names to property names 
     * the property name is detected by ColumnExpr.getBeanPropertyName()     
     * @param bean the Java Bean for which to set the properties
     * @param ignoreList list of columns to skip (optional)
     */
    int setBeanProperties(Object bean, Collection<ColumnExpr> ignoreList);

    /**
     * copies all field values into a static Java Bean.
     * <P>
     * In order to map column names to property names 
     * the property name is detected by ColumnExpr.getBeanPropertyName()     
     * @param bean the Java Bean for which to set the properties
     */
    int setBeanProperties(Object bean);

}
