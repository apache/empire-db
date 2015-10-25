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

import org.apache.empire.db.DBRecordData;

/**
 * Interface used by {@link EmpireTemplate} for mapping
 * {@link org.apache.empire.db.DBRecordData} to an Object.
 * 
 * Typically it can be used to extract data from a DBReader, but without
 * iterating over it, it is handled by EmpireTemplate.
 * 
 * This class is the Empire equivalent of Spring's
 * {@link org.springframework.jdbc.core.RowMapper}.
 * 
 */

public interface DBRecordMapper<K> {

	/**
	 * Implementations must implement this method to map data in a DBRecordData.
	 * 
	 * @param record
	 *            the DBRecordData to map
	 * @param rowNum
	 *            the number of the current row
	 * @return the result object
	 */

	public abstract K mapRecord(DBRecordData record, int rowNum);

}
