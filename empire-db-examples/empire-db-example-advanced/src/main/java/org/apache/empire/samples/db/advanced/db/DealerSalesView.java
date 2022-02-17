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
    public final DBViewColumn SALE_YEAR;
    public final DBViewColumn SALE_COUNT;
    public final DBViewColumn DEALERSHIP_TYPE;

    public DealerSalesView(CarSalesDB db)
    {
        super("DEALER_SALES_VIEW", db);
        // add columns
        DEALER_NAME    = addColumn("DEALER_NAME",     db.DEALER.COMPANY_NAME);
        DEALER_COUNTRY = addColumn("DEALER_COUNTRY",  db.DEALER.COUNTRY);
        BRAND_SOLD     = addColumn("BRAND_SOLD",      db.BRAND.NAME);
        DEALERSHIP_TYPE= addColumn("DEALERSHIP_TYPE", db.DEALER_BRANDS.DEALERSHIP_TYPE);
        SALE_YEAR      = addColumn("SALE_YEAR",       db.SALES.YEAR);
        SALE_COUNT     = addColumn("SALE_COUNT",      DataType.INTEGER);
    }

    @Override
    public DBCommandExpr createCommand()
    {
        DBCommand cmd = newCommand();
        // select
        cmd.select(DB.DEALER.COMPANY_NAME, DB.DEALER.COUNTRY);
        cmd.select(DB.BRAND.NAME, DB.DEALER_BRANDS.DEALERSHIP_TYPE);
        cmd.select(DB.SALES.YEAR, DB.SALES.count());
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
