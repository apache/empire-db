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
        // RS = RowSet
        create();
        set(RS.WMI             , brand);
        set(RS.NAME            , modelName);
        set(RS.CONFIG_NAME     , configName);
        set(RS.TRIM            , trim);
        set(RS.ENGINE_TYPE     , engineType);
        set(RS.ENGINE_POWER    , enginePower);
        set(RS.BASE_PRICE      , basePrice);
        update();
    }
}
