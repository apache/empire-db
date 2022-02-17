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

import org.apache.empire.commons.Options;
import org.apache.empire.exceptions.InvalidArgumentException;


/**
 * The record interface provides methods used for updating data.
 * <P>
 * A object implementing the record interface is essentially a so called "dynamic bean".
 * <P>
 * This interface inherits from RecordData which provides further data access methods.
 * <P>
 * The Record interface is implemented by the class {@link org.apache.empire.db.DBRecordBase}
 */
public interface Record extends RecordData
{
    /**
     * varArgs to Array
     * @param parts
     * @return
     */
    public static Object[] key(Object... values)
    {
        if (values.length==0)
            throw new InvalidArgumentException("values", values);
        // check values
        for (int i=0; i<values.length; i++) {
            // Replace record with key
            if (values[i] instanceof Record)
                values[i]=((Record)values[i]).getKey();
            // Replace key with value
            if (values[i] instanceof Object[]) {   
               Object[] key = (Object[])values[i];
               if (key.length!=1)
                   throw new InvalidArgumentException("values", values[i]);
               values[i]=key[0];
            }
        }
        return values;
    }

    /**
     * returns true if the record is valid.
     * @return true if the record is valid
     */
    boolean isValid();

    /**
     * returns true if this record is readOnly.
     * @return true if this record is readOnly
     */
    boolean isReadOnly();

    /**
     * returns true if the record has been modified.
     * @return true if the record has been modified or false otherwise
     */
    boolean isModified();

    /**
     * returns true if this record is a new record.
     * @return true if this record is a new record
     */
    boolean isNew();

    /**
     * Overridden to change return type from ColumnExpr to Column
     */
    @Override
    Column getColumn(int index);
    
    /**
     * returns an array of key columns which uniquely identify the record.
     * @return the array of key columns if any
     */
    Column[] getKeyColumns();    
    
    /**
     * returns an array of key values which uniquely identify the record.
     * @return the key value array
     */
    Object[] getKey();    
    
    /**
     * Returns the entity this Record belongs to 
     * @return the entity
     */
    EntityType getEntityType();

    /**
     * returns true if the field is visible to the client.
     * @param column the column to check for visibility
     * @return true if the field is visible to the client
     */
    boolean isFieldVisible(Column column);

    /**
     * returns true if the field is read-only.
     * @param column the requested column
     * @return true if the field is read-only
     */
    boolean isFieldReadOnly(Column column);

    /**
     * returns true if the field is required.
     * @param column the requested column
     * @return true if the field is required
     */
    boolean isFieldRequired(Column column);

    /**
     * returns the Options list for the given record field.
     * @param column the column to check for visibility
     * @return an options collection containing all allowed field values
     */
    Options getFieldOptions(Column column);


    /**
     * sets a record value based on the field index.
     * @param i index of the field for which to set the value
     * @param value the new field value
     */
    void setValue(int i, Object value);

    /**
     * sets a record value based on a column.
     * @param column the requested column
     * @param value the new record value for the given column
     */
    Record set(Column column, Object value);

    /**
     * Validates a value before it is set in the record.
     * By default, this method simply calls column.validate()
     * @param column the column
     * @param value the value to validate
     * @return the value
     */
    Object validateValue(Column column, Object value);
    
    /**
     * checks whether or not the field for the given column has been modified since it has been loaded.
     * @param column the requested column
     * @return Returns true if a column has been modified
     */
    boolean wasModified(Column column);

    // ------- Java Bean Support -------

    /**
     * sets all record values from a particular bean.
     * <P>
     * The bean must provide corresponding getter functions for all desired column.
     * <P>
     * In order to map column names to property names 
     * the property name is detected by ColumnExpr.getBeanPropertyName()     
     * <P>
     * @param bean the Java Bean from which to read the value from
     * @param ignoreList list of column to ignore
     * @return the number of fields that have been set    
     */
    int setRecordValues(Object bean, Collection<Column> ignoreList);

    /**
     * sets all record values from a particular bean.
     * <P>
     * The bean must provide corresponding getter functions for all desired column.
     * <P>
     * @param bean the Java Bean from which to read the value from
     * @return the number of fields that have been set    
     */
    int setRecordValues(Object bean);

}
