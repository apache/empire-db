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
package org.apache.empire.samples.db.advanced.db;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.generic.TView;
import org.apache.empire.samples.db.advanced.db.CarSalesDB.DealershipType;

public class DealerSalesView extends TView<CarSalesDB>
{
    public final DBViewColumn DEALER_NAME;
    public final DBViewColumn DEALER_COUNTRY;
    public final DBViewColumn BRAND_SOLD;
    public final DBViewColumn DEALERSHIP_TYPE;
    public final DBViewColumn SALE_YEAR;
    public final DBViewColumn SALE_COUNT;
    public final DBViewColumn TURNOVER;

    public DealerSalesView(CarSalesDB db)
    {
        super("DEALER_SALES_VIEW", db);
        // add columns
        DEALER_NAME     = addColumn("DEALER_NAME",     db.DEALER.COMPANY_NAME);
        DEALER_COUNTRY  = addColumn("DEALER_COUNTRY",  db.DEALER.COUNTRY);
        BRAND_SOLD      = addColumn("BRAND_SOLD",      db.BRAND.NAME);
        DEALERSHIP_TYPE = addColumn("DEALERSHIP_TYPE", db.DEALER_BRANDS.DEALERSHIP_TYPE);
        SALE_YEAR       = addColumn("SALE_YEAR",       db.SALES.YEAR);
        SALE_COUNT      = addColumn("SALE_COUNT",      DataType.INTEGER);
        TURNOVER        = addColumn("TURNOVER",        DataType.DECIMAL);
    }

    @Override
    public DBCommandExpr createCommand()
    {
        DBCommand cmd = newCommand();
        // select
        cmd.select(DB.DEALER.COMPANY_NAME, DB.DEALER.COUNTRY);
        cmd.select(DB.BRAND.NAME, DB.DEALER_BRANDS.DEALERSHIP_TYPE);
        cmd.select(DB.SALES.YEAR, DB.SALES.count());
        cmd.select(DB.SALES.PRICE.sum().as(TURNOVER));
        // joins
        cmd.join (DB.DEALER.ID, DB.SALES.DEALER_ID);
        cmd.join (DB.SALES.MODEL_ID, DB.MODEL.ID);
        cmd.join (DB.MODEL.WMI, DB.BRAND.WMI);
        cmd.joinLeft(DB.DEALER.ID, DB.DEALER_BRANDS.DEALER_ID,
                     DB.DEALER_BRANDS.WMI.is(DB.MODEL.WMI), 
                     DB.DEALER_BRANDS.DEALERSHIP_TYPE.isNot(DealershipType.U));
        // aggregation
        cmd.groupBy(DB.DEALER.COMPANY_NAME, DB.DEALER.COUNTRY);
        cmd.groupBy(DB.BRAND.NAME, DB.DEALER_BRANDS.DEALERSHIP_TYPE);
        cmd.groupBy(DB.SALES.YEAR);
        // done
        return cmd;
    }

}
