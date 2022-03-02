package org.apache.empire.samples.db.advanced.records;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.time.LocalDate;

import org.apache.empire.db.generic.TRecord;
import org.apache.empire.samples.db.advanced.SampleContext;
import org.apache.empire.samples.db.advanced.db.CarSalesDB;
import org.apache.empire.samples.db.advanced.db.CarSalesDB.EngineType;
import org.apache.empire.xml.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class ModelRecord extends TRecord<CarSalesDB.Model>
{
    private static final Logger log = LoggerFactory.getLogger(ModelRecord.class);

    private static final long serialVersionUID = 1L;

    public ModelRecord(SampleContext context)
    {
        super(context, context.getDatabase().MODEL);
    }
    
    public void insert(BrandRecord brand, String modelName, String configName, String trim
                       , EngineType engineType, int enginePower, double basePrice, LocalDate firstSale)
    {
        // T = RowSet (Table/View)
        create();
        set(T.WMI             , brand);
        set(T.NAME            , modelName);
        set(T.SPECIFICATION   , configName);
        set(T.TRIM            , trim);
        set(T.ENGINE_TYPE     , engineType);
        set(T.ENGINE_POWER    , enginePower);
        set(T.BASE_PRICE      , basePrice);
        set(T.FIRST_SALE      , firstSale);
        update();
    }
    
    @Override
    public void update()
    {
        if (isModified())
        {   // Record was modified
            // Clear and reset MODEL_BINARY and MODEL_XML
            set(T.MODEL_XML, null);
            set(T.MODEL_BINARY, null);
            // Reset
            set(T.MODEL_XML, getModelXml());
            set(T.MODEL_BINARY, getModelBinary());
        }
        super.update();
    }
    
    private String getModelXml()
    {
        Document modelDoc = this.getXmlDocument();
        StringWriter writer = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(writer, "utf-8");
        xmlWriter.print(modelDoc);
        return writer.toString();
    }
    
    private byte[] getModelBinary()
    {
        try
        {   // All fields, but without MODEL_BINARY
            Object[] fields = this.getFields();
            fields[getFieldIndex(T.MODEL_BINARY)]=null;
            // Serialize the record fields
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(fields);
            return baos.toByteArray();
        }
        catch (IOException e)
        {
            log.error("Unable to serialize the record fields", e);
            return null;
        }
    }
}
