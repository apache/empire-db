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
 * An interface used by {@link EmpireTemplate} for processing a DBRecordData or
 * rows of a DBReader on a per-row basis.
 * 
 * 
 *
 * DbRecordCallbackHandler object is typically stateful: It keeps the result
 * state within the object, to be available for later inspection.
 *
 * If you need to map exactly one object to each row from a DBReader consider
 * using a {@link DbRecordDataMapper}.
 * 
 *
 */

public interface DBRecordCallbackHandler {

	/**
	 * Implementations must implement this method to process a DBRecordData.
	 *  
	 * @param record
	 */
	
	void processRow(DBRecordData record);

}
