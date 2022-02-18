package org.apache.empire.samples.db.advanced.records;

import org.apache.empire.db.generic.TRecord;
import org.apache.empire.samples.db.advanced.SampleContext;
import org.apache.empire.samples.db.advanced.db.CarSalesDB;
import org.apache.empire.samples.db.advanced.db.CarSalesDB.EngineType;

public class ModelRecord extends TRecord<CarSalesDB.Model>
{
    private static final long serialVersionUID = 1L;

    public ModelRecord(SampleContext context)
    {
        super(context, context.getDatabase().MODEL);
    }
    
    public void insert(BrandRecord brand, String modelName, String configName, String trim, EngineType engineType, int enginePower, double basePrice)
    {
        // T = RowSet (Table/View)
        create();
        set(T.WMI             , brand);
        set(T.NAME            , modelName);
        set(T.CONFIG_NAME     , configName);
        set(T.TRIM            , trim);
        set(T.ENGINE_TYPE     , engineType);
        set(T.ENGINE_POWER    , enginePower);
        set(T.BASE_PRICE      , basePrice);
        update();
    }
}
