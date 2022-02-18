package org.apache.empire.samples.db.advanced.records;

import org.apache.empire.db.generic.TRecord;
import org.apache.empire.samples.db.advanced.SampleContext;
import org.apache.empire.samples.db.advanced.db.CarSalesDB;

public class DealerRecord extends TRecord<CarSalesDB.Dealer>
{
    private static final long serialVersionUID = 1L;

    public DealerRecord(SampleContext context)
    {
        super(context, context.getDatabase().DEALER);
    }
    
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
}
