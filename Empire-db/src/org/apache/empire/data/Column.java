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

/**
 * The column interface provides methods for accessing metadata that is only relevant for updateing records.
 * <P>
 * This interface inherits from ColumnExpr which provides futher metadata.
 * <P>
 */
public interface Column extends ColumnExpr
{

    /**
     * Returns the maximum size a value for this column is allowed to have.
     * <P>
     * For the data type DECIMAL the size defines the scale and precision of the value.
     * <P>
     * @return Returns the maximum size a value for this column is allowed to have.
     */
    double getSize();

    /**
     * Returns whether or not the value for this column must be
     * supplied (i.e. it is mandatory) or not.
     * <P>
     * @return Returns true if the value for this column must be supplied
     */
    boolean isRequired();

    /**
     * Returns true if the values for this column are generally
     * read only (like i.e. for sequence generated values).
     * <P>
     * @return Returns true if the values for this column are generally read-only
     */
    boolean isReadOnly();

}
