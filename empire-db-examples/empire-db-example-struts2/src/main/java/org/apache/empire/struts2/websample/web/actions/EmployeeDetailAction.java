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
package org.apache.empire.struts2.websample.web.actions;

import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBTable;
import org.apache.empire.struts2.actionsupport.RecordActionSupport;
import org.apache.empire.struts2.actionsupport.SessionPersistence;
import org.apache.empire.struts2.websample.db.records.EmployeeRecord;
import org.apache.empire.struts2.websample.web.actiontypes.DetailAction;
import org.apache.struts2.interceptor.NoParameters;


@SuppressWarnings("serial")
/**
 * EmployeeDetailAction
 * <p>
 * This class provides form processing functions for an Employee record.<br>
 * The class uses a RecordActionSupport object which does most of the work.<br>
 * For multi-record forms it is possible to have several RecordActionSupport members.<br>
 * In this case each must be given a differnt property name however (see RecordActionSupport overloads).
 * </p>
 */
public class EmployeeDetailAction extends DetailAction
    implements NoParameters // set this to provide custom parameter handling
{
    /**
     * Action mappings
     */

    protected RecordActionSupport recordSupport = null;

    // ------- Action Construction -------
    
    public EmployeeDetailAction() {
        // Init Record Support Object
        DBTable table = getDatabase().T_EMPLOYEES;
        DBRecord record = new EmployeeRecord(this);
        // create a support Object
        recordSupport = new RecordActionSupport(this, table, record, SessionPersistence.Key);
    }

    // ------- Action Properties -------
    
    public EmployeeRecord getEmployee()
    {
        return (EmployeeRecord) recordSupport.getRecord();
    }

    // ------- Action Methods -------

    @Override
    public String doCreate() {
        // Create Record
        if (!recordSupport.createRecord()) {
            setActionError(recordSupport);
            return RETURN;
        }
        // Done
        return INPUT;
    }

    @Override
    public String doLoad() {
        // Load Record
        if (!recordSupport.loadRecord()) {
            setActionError(recordSupport);
            return RETURN;
        }
        // Set Edit Mode
        return INPUT;
    }

    @Override
    public String doSave() {
        // Load Form Data into record
        if (!recordSupport.loadFormData()) {
            if (recordSupport.hasError())
                setActionError(recordSupport);
            return INPUT;
        }
        // Now save the record
        if (!recordSupport.saveChanges()) {
            setActionError(recordSupport);
            return INPUT;
        }
        // Erfolg
        return RETURN;
    }

    @Override
    public String doDelete() {
        // Delete Record
        if (!recordSupport.deleteRecord()) {
            setActionError(recordSupport);
            return INPUT;
        }
        // Erfolg
        return RETURN;
    }

}
