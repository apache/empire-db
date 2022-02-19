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
package org.apache.empire.jsf2.websample.web.pages;

import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.db.DBCommand;
import org.apache.empire.jsf2.pageelements.RecordPageElement;
import org.apache.empire.jsf2.pages.PageOutcome;
import org.apache.empire.jsf2.websample.db.SampleDB;
import org.apache.empire.jsf2.websample.db.records.EmployeeRecord;
import org.apache.empire.jsf2.websample.web.SampleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmployeeDetailPage extends SamplePage
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    private static final Logger               log               = LoggerFactory.getLogger(EmployeeDetailPage.class);

    private String                            idParam;

    private RecordPageElement<EmployeeRecord> employee;

    private int                               activeTab         = 0;
    
    private List<DataListEntry>               payments;
    
    public EmployeeDetailPage()
    {
        log.trace("EmployeeDetailPage created");

        EmployeeRecord emplRec = new EmployeeRecord(getSampleContext());
        employee = new RecordPageElement<EmployeeRecord>(this, emplRec.getTable(), emplRec);
    }

    public String getIdParam()
    {
        return this.idParam;
    }

    public void setIdParam(String idParam)
    {
        log.info("EmployeeDetailPage idParam = {}.", idParam);
        this.idParam = idParam;
    }

    public RecordPageElement<EmployeeRecord> getEmployee()
    {
        return employee;
    }

    public EmployeeRecord getEmployeeRecord()
    {
        return employee.getRecord();
    }

    public List<DataListEntry> getPayments()
    {
        return payments;
    }

    public int getActiveTab()
    {
        return activeTab;
    }

    public void setActiveTab(int activeTab)
    {
        this.activeTab = activeTab;
    }

    @Override
    public void doInit()
    { // Notify Elements
        if (!employee.getRecord().isValid())
        {
            employee.reloadRecord();
            // Load payment data
            loadPaymentData();
        }
    }

    public void doLoad()
    {
        log.info("EmployeeDetailPage Loading entryId {}.", this.idParam);
        // load the record
        this.employee.loadRecord(this.idParam);
        // Load payment data
        loadPaymentData();
    }

    public void doCreate()
    {
        // use create(null) to defer primaryKey generation
        getEmployeeRecord().create(null);
        doRefresh();
    }
    
    public PageOutcome doSave()
    {
        getEmployeeRecord().update();
        
        /* test transaction 
        SampleDB db = this.getDatabase();
        if (getEmployeeRecord().isNull(db.T_EMPLOYEES.PHONE_NUMBER))
            throw new MiscellaneousErrorException("Phone number must not be empty!");
        */  
        
        return getParentOutcome(true);
    }
    
    public void doTestSerialization(ActionEvent ae)
    {
        /* test serialization */
        EmployeeRecord before = getEmployeeRecord();
        EmployeeRecord after  = ClassUtils.testSerialization(EmployeeRecord.class, before);
        
        addInfoMessage("!employeeDetail_testMessage");
        addInfoMessage("Rowset is {0}",  (after.getRowSet() ==before.getRowSet())  ? "Same" : "Different");
        addInfoMessage("Context is {0}", (after.getContext()==before.getContext()) ? "Same" : "Different");
        after.read(before.getKey());

        addInfoMessage("!global_status", after.isValid() ? "!global_valid" : "!global_invalid");
    }

    public PageOutcome doDelete()
    {
        getEmployeeRecord().delete();
        return getParentOutcome(true);
    }
    
    public PageOutcome doCancel()
    {
        return getParentOutcome(true);
    }

    public void onTabChanged(int newPage)
    {
        log.debug("onTabChanged " + newPage);
    }
    
    private void loadPaymentData()
    {
        SampleContext context = getSampleContext();
        SampleDB db = this.getDatabase();
        SampleDB.TPayments PAY = db.PAYMENTS;
        
        DBCommand cmd = context.createCommand();
        cmd.select(PAY.YEAR, PAY.MONTH, PAY.AMOUNT);
        cmd.where(PAY.EMPLOYEE_ID.is(employee.getRecord().getIdentity()));
        cmd.orderBy(PAY.YEAR.desc(), PAY.MONTH.desc());

        this.payments = context.getUtils().queryDataList(cmd); 
        log.info("{} payments have been loaded", this.payments.size());
    }
    
}
