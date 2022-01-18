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

import org.apache.empire.commons.Options;

/**
 * The column interface provides methods for accessing metadata that is relevant for managing
 * and displaying data available through the RecordData interface.
 */
public interface ColumnExpr
{

    /**
     * Returns the column's data type.
     * @see DataType
     * @return the column's data type
     */
    DataType getDataType();

    /**
     * Returns the physical column name.
     * @return the physical column name
     */
    String getName();

    /**
     * Returns the column's display title.
     * @return the column's display title
     */
    String getTitle();

    /**
     * Returns the column's control type used for displaying and entering data.
     * @return the column's control type used for displaying and entering data
     */
    String getControlType();

    /**
     * Returns futher metadata attributes.
     * @param name the name of the attribute
     * @return futher metadata attributes
     */
    Object getAttribute(String name);

    /**
     * Returns an option set with possible column values and their
     * corresponding display text.
     * @return option set with possible column values and their corresponding display text
     */
    Options getOptions();

    /**
     * Returns the name of a Java bean property to which this column is mapped.
     * @return the name of a Java bean property to which this column is mapped
     */
    String getBeanPropertyName();

    /**
     * Returns the underlying source column (if any).
     * If an expression is based not based on a particutlar column this function returns null.
     * @return the column on which this expression is based or null if not applicable.
     */
    Column getSourceColumn();

}
