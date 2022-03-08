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
package org.apache.empire.samples.db.advanced.records;

import java.util.List;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.generic.TRecord;
import org.apache.empire.samples.db.advanced.SampleContext;
import org.apache.empire.samples.db.advanced.db.CarSalesDB;

public class DealerRecord extends TRecord<SampleContext, CarSalesDB.Dealer>
{
    private static final long serialVersionUID = 1L;
    
    private List<BrandRecord> brandList = null; // Lazy initialization

    public DealerRecord(SampleContext context)
    {
        super(context, context.getDatabase().DEALER);
    }
    
    /**
     * Create a new Dealer 
     * @param companyName
     * @param city
     * @param country
     * @return the DealerRecord itself (returned to allow instruction chaining)
     */
    public DealerRecord insert(String companyName, String city, String country)
    {
        // T = RowSet (Table/View)
        create();
        set(T.COMPANY_NAME, companyName);
        set(T.CITY,         city);
        set(T.COUNTRY,      country);
        update();
        return this;
    }

    /**
     * Get the Brand names as a comma separated String
     * @return the brand names 
     */
    public String getBrands()
    {
        DBCommand cmd = CTX.createCommand()
                .select(T.DB.BRAND.NAME.stringAgg(", "))
                .join(T.DB.DEALER_BRANDS.WMI, T.DB.BRAND.WMI)
                .where(T.DB.DEALER_BRANDS.DEALER_ID.is(getIdentity()));
        return CTX.getUtils().querySingleString(cmd);
    }
    
    /**
     * Get the dealers Brands as a list of BrandRecords
     * @return the BrandRecords 
     */
    public List<BrandRecord> getBrandList()
    {
        if (brandList == null)
        {   // init brand list
            DBCommand cmd = CTX.createCommand()
                    .join(T.DB.DEALER_BRANDS.WMI, T.DB.BRAND.WMI)
                    .where(T.DB.DEALER_BRANDS.DEALER_ID.is(getIdentity()));
            brandList = CTX.getUtils().queryRecordList(cmd, T.DB.BRAND, BrandRecord.class);
        }
        return brandList;
    }
}
