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
package org.apache.empire.samples.db.beans;

import java.math.BigDecimal;

import org.apache.empire.db.DBContext;
import org.apache.empire.db.list.DataBean;
import org.apache.empire.samples.db.SampleDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Payment implements DataBean<SampleDB>
{
    private static final Logger log = LoggerFactory.getLogger(Payment.class);
    
    private long        employeeId;
    private BigDecimal  year;
    private BigDecimal  month;
    private BigDecimal  amount;
    
    private Employee    employee;
    
    public Payment(long employeeId, BigDecimal year, BigDecimal month, BigDecimal amount)
    {
        super();
        this.employeeId = employeeId;
        this.year = year;
        this.month = month;
        this.amount = amount;
    }

    public long getEmployeeId()
    {
        return employeeId;
    }

    public BigDecimal getYear()
    {
        return year;
    }

    public BigDecimal getMonth()
    {
        return month;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public Employee getEmployee()
    {
        return employee;
    }

    @Override
    public void initialize(SampleDB db, DBContext context, int rownum, Object parent)
    {
        if (parent instanceof Employee)
            this.employee = (Employee)parent;
        else
            log.warn("Employee Entity has not been provided.");
    }

}
