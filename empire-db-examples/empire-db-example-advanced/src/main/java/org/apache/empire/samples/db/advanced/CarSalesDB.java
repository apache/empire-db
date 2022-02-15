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
package org.apache.empire.samples.db.advanced;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.generic.TDatabase;
import org.apache.empire.db.generic.TTable;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.db.validation.DBModelErrorLogger;
import org.apache.empire.dbms.postgresql.DBMSHandlerPostgreSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <PRE>
 * This file contains the definition of the data model in Java.
 * The SampleDB data model consists of three tables and a foreign key relation.
 * The tables are defined as nested classes here, but you may put them in separate files if you want to.
 *
 * PLEASE NOTE THE NAMING CONVENTION:
 * Since all tables, views and columns are declared as "final" constants they are all in upper case.
 * We recommend using a prefix of T_ for tables and C_ for columns in order to keep them together
 * when listed in your IDE's code completion.
 * There is no need to stick to this convention but it makes life just another little bit easier.
 *
 * You may declare other database tables or views in the same way.
 * </PRE>
 */
public class CarSalesDB extends TDatabase<CarSalesDB>
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(CarSalesDB.class);

    /**
     * EngineType enum
     */
    public enum EngineType
    {
        P("Petrol"),
        D("Diese"),
        H("Hybrid"),
        E("Electric");
        
        private final String title;
        private EngineType(String title)
        {
            this.title = title;
        }
        @Override
        public String toString()
        {
            return title;
        }
    }

    /**
     * This class represents the Departments table.
     */
    public static class Brand extends TTable<CarSalesDB>
    {
        public final DBTableColumn ID;
        public final DBTableColumn NAME;
        public final DBTableColumn COUNTRY;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Brand(CarSalesDB db)
        {
            super("BRAND", db);
            // ID
            ID              = addColumn("ID",               DataType.AUTOINC,       0, true, "BRAND_ID_SEQUENCE"); // Optional Sequence for some DBMS (e.g. Oracle)
            NAME            = addColumn("NAME",             DataType.VARCHAR,      80, true);
            COUNTRY         = addColumn("COUNTRY",          DataType.VARCHAR,      80, false);
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.TIMESTAMP,     0, true);

            // Primary Key (automatically set due to AUTOINC column)
            // setPrimaryKey(ID);
        }
    }

    /**
     * This class represents the Employees table.
     */
    public static class Model extends TTable<CarSalesDB>
    {
        public final DBTableColumn ID;
        public final DBTableColumn NAME;
        public final DBTableColumn CONFIG_NAME;
        public final DBTableColumn BRAND_ID;
        public final DBTableColumn TRIM;
        public final DBTableColumn ENGINE_TYPE;
        public final DBTableColumn ENGINE_POWER;
        public final DBTableColumn BASE_PRICE;
        public final DBTableColumn UPDATE_TIMESTAMP;

        public Model(CarSalesDB db)
        {
            super("MODEL", db);
            
            // ID
            ID              = addColumn("ID",               DataType.AUTOINC,      0, true, "MODEL_ID_SEQUENCE");  // Optional Sequence name for some DBMS (e.g. Oracle)
            NAME            = addColumn("NAME",             DataType.VARCHAR,     20, true);
            CONFIG_NAME     = addColumn("CONFIGURATION",    DataType.VARCHAR,     40, true);
            BRAND_ID        = addColumn("BRAND_ID",         DataType.INTEGER,      0, true);
            TRIM            = addColumn("TRIM",             DataType.VARCHAR,     20, true);
            ENGINE_TYPE     = addColumn("ENGINE_TYPE",      DataType.CHAR,         1, true, EngineType.class);
            ENGINE_POWER    = addColumn("ENGINE_POWER",     DataType.DECIMAL,    4.0, true);
            BASE_PRICE      = addColumn("BASE_PRICE",       DataType.DECIMAL,    8.2, false);
            UPDATE_TIMESTAMP= addColumn("UPDATE_TIMESTAMP", DataType.TIMESTAMP,    0, true);
            
            // Primary Key (automatically set due to AUTOINC column)
            // setPrimaryKey(ID);
        }
    }

    /**
     * This class represents the Payments table.
     */
    public static class Sales extends TTable<CarSalesDB>
    {
        public final DBTableColumn MODEL_ID;
        public final DBTableColumn YEAR;
        public final DBTableColumn MONTH;
        public final DBTableColumn CAR_COLOR;
        public final DBTableColumn PRICE;

        public Sales(CarSalesDB db)
        {
            super("SALES", db);
            
            // ID
            MODEL_ID        = addColumn("MODEL_ID",         DataType.INTEGER,      0, true);
            YEAR            = addColumn("YEAR",             DataType.DECIMAL,    4.0, true);
            MONTH           = addColumn("MONTH",            DataType.DECIMAL,    2.0, true);
            CAR_COLOR       = addColumn("CAR_COLOR",        DataType.VARCHAR,     20, false);
            PRICE           = addColumn("PRICE",            DataType.DECIMAL,    8.2, true);

            // No primary key!
        }
    }
    
    // Declare all Tables and Views here
    public final Brand  BRAND = new Brand(this);
    public final Model  MODEL = new Model(this);
    public final Sales  SALES = new Sales(this);

    /**
     * Constructor of the SampleDB data model
     *
     * Put all foreign key relations here.
     */
    public CarSalesDB()
    {
        // Define Foreign-Key Relations
        addRelation( MODEL.BRAND_ID.referenceOn( BRAND.ID ));
        addRelation( SALES.MODEL_ID.referenceOn( MODEL.ID ));
    }
    
    @Override
    public void open(DBContext context)
    {
        // Enable prepared statements
        setPreparedStatementsEnabled(true);
        // Check exists
        if (checkExists(context))
        {   // attach to driver
            super.open(context);
            // yes, it exists, then check the model
            checkDataModel(context);
        } 
        else
        {   // PostgreSQL does not support DDL in transaction
            if(getDbms() instanceof DBMSHandlerPostgreSQL)
                setAutoCommit(context, true);
            // create the database
            createDatabase(context);
            // PostgreSQL does not support DDL in transaction
            if(getDbms() instanceof DBMSHandlerPostgreSQL)
                setAutoCommit(context, false);
            // attach to driver
            super.open(context);
            // populate 
            populate(context);
            // Commit
            context.commit();
        }
    }

    private void createDatabase(DBContext context)
    {
        // create DDL for Database Definition
        DBSQLScript script = new DBSQLScript(context);
        getCreateDDLScript(script);
        // Show DDL Statement
        log.info(script.toString());
        // Execute Script
        script.executeAll(false);
    }
    
    private void checkDataModel(DBContext context)
    {   try {
            DBModelChecker modelChecker = context.getDbms().createModelChecker(this);
            // Check data model   
            log.info("Checking DataModel for {} using {}", getClass().getSimpleName(), modelChecker.getClass().getSimpleName());
            // dbo schema
            DBModelErrorLogger logger = new DBModelErrorLogger();
            modelChecker.checkModel(this, context.getConnection(), logger);
            // show result
            log.info("Data model check done. Found {} errors and {} warnings.", logger.getErrorCount(), logger.getWarnCount());
        } catch(Exception e) {
            log.error("FATAL error when checking data model. Probably not properly implemented by DBMSHandler!");
        }
    }
    
    private void setAutoCommit(DBContext context, boolean enable)
    {   try {
            context.getConnection().setAutoCommit(enable);
        } catch (SQLException e) {
            log.error("Unable to set AutoCommit on Connection", e);
        }
    }
    
    private void populate(DBContext context)
    {
        DBRecord brand = new DBRecord(context, BRAND);
        brand.create().set(BRAND.NAME, "VW").set(BRAND.COUNTRY, "Germany").update();  long idVW = brand.getId();
        brand.create().set(BRAND.NAME, "Ford").set(BRAND.COUNTRY, "USA").update();    long idFord = brand.getId();
        brand.create().set(BRAND.NAME, "Tesla").set(BRAND.COUNTRY, "USA").update();   long idTesla = brand.getId();
        brand.create().set(BRAND.NAME, "Toyota").set(BRAND.COUNTRY, "Japan").update();long idToy = brand.getId();
        
        DBRecord model = new DBRecord(context, MODEL);
        // VW
        model.create().set(MODEL.BRAND_ID, idVW).set(MODEL.NAME, "Golf").set(MODEL.CONFIG_NAME, "Golf Style 1,5 l TSI").set(MODEL.TRIM, "Style").set(MODEL.ENGINE_TYPE, EngineType.P).set(MODEL.ENGINE_POWER, 130).set(MODEL.BASE_PRICE,30970).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idVW).set(MODEL.NAME, "Golf").set(MODEL.CONFIG_NAME, "Golf R-Line 2,0 l TSI 4MOTION").set(MODEL.TRIM, "R-Line").set(MODEL.ENGINE_TYPE, EngineType.P).set(MODEL.ENGINE_POWER, 190).set(MODEL.BASE_PRICE,38650).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idVW).set(MODEL.NAME, "Tiguan").set(MODEL.CONFIG_NAME, "Tiguan Life 1,5 l TSI").set(MODEL.TRIM, "Life").set(MODEL.ENGINE_TYPE, EngineType.P).set(MODEL.ENGINE_POWER, 150).set(MODEL.BASE_PRICE,32545).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idVW).set(MODEL.NAME, "Tiguan").set(MODEL.CONFIG_NAME, "Tiguan Elegance 2,0 l TDI SCR").set(MODEL.TRIM, "Elegance").set(MODEL.ENGINE_TYPE, EngineType.D).set(MODEL.ENGINE_POWER, 150).set(MODEL.BASE_PRICE,40845).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idVW).set(MODEL.NAME, "Tiguan").set(MODEL.CONFIG_NAME, "Tiguan R-Line 1,4 l eHybrid").set(MODEL.TRIM, "R-Line").set(MODEL.ENGINE_TYPE, EngineType.H).set(MODEL.ENGINE_POWER, 150).set(MODEL.BASE_PRICE,48090).update();generateRandomSales(model);
        // Tesla
        model.create().set(MODEL.BRAND_ID, idTesla).set(MODEL.NAME, "Model 3").set(MODEL.CONFIG_NAME, "Model 3 LR").set(MODEL.TRIM, "Long Range").set(MODEL.ENGINE_TYPE, EngineType.E).set(MODEL.ENGINE_POWER, 261).set(MODEL.BASE_PRICE,45940).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idTesla).set(MODEL.NAME, "Model 3").set(MODEL.CONFIG_NAME, "Model 3 Performance").set(MODEL.TRIM, "Performance").set(MODEL.ENGINE_TYPE, EngineType.E).set(MODEL.ENGINE_POWER, 487).set(MODEL.BASE_PRICE,53940).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idTesla).set(MODEL.NAME, "Model Y").set(MODEL.CONFIG_NAME, "Model Y LR").set(MODEL.TRIM, "Long Range").set(MODEL.ENGINE_TYPE, EngineType.E).set(MODEL.ENGINE_POWER, 345).set(MODEL.BASE_PRICE,53940).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idTesla).set(MODEL.NAME, "Model Y").set(MODEL.CONFIG_NAME, "Model Y Performance").set(MODEL.TRIM, "Performance").set(MODEL.ENGINE_TYPE, EngineType.E).set(MODEL.ENGINE_POWER, 450).set(MODEL.BASE_PRICE,58940).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idTesla).set(MODEL.NAME, "Model S").set(MODEL.CONFIG_NAME, "Model S Plaid").set(MODEL.TRIM, "Plaid").set(MODEL.ENGINE_TYPE, EngineType.E).set(MODEL.ENGINE_POWER, 1020).set(MODEL.BASE_PRICE,126990).update(); // no sales
        // Ford
        model.create().set(MODEL.BRAND_ID, idFord).set(MODEL.NAME, "Mustang").set(MODEL.CONFIG_NAME, "Mustang GT 5,0 l Ti-VCT V8").set(MODEL.TRIM, "GT").set(MODEL.ENGINE_TYPE, EngineType.P).set(MODEL.ENGINE_POWER, 449).set(MODEL.BASE_PRICE,54300).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idFord).set(MODEL.NAME, "Mustang").set(MODEL.CONFIG_NAME, "Mustang Mach1 5,0 l Ti-VCT V8").set(MODEL.TRIM, "Mach1").set(MODEL.ENGINE_TYPE, EngineType.P).set(MODEL.ENGINE_POWER, 460).set(MODEL.BASE_PRICE,62800).update();generateRandomSales(model);
        // Toyota
        model.create().set(MODEL.BRAND_ID, idToy).set(MODEL.NAME, "Prius").set(MODEL.CONFIG_NAME, "Prius Hybrid 1,8-l-VVT-i").set(MODEL.TRIM, "Basis").set(MODEL.ENGINE_TYPE, EngineType.H).set(MODEL.ENGINE_POWER, 122).set(MODEL.BASE_PRICE,38000).update();generateRandomSales(model);
        model.create().set(MODEL.BRAND_ID, idToy).set(MODEL.NAME, "Supra").set(MODEL.CONFIG_NAME, "GR Supra Pure 2,0 l Twin-Scroll Turbo").set(MODEL.TRIM, "Pure").set(MODEL.ENGINE_TYPE, EngineType.P).set(MODEL.ENGINE_POWER, 258).set(MODEL.BASE_PRICE,49290).update();generateRandomSales(model);
    }
    
    private void generateRandomSales(DBRecord model)
    {
        int baseYear = LocalDate.now().getYear()-3;
        BigDecimal price = model.getDecimal(MODEL.BASE_PRICE);
        if (ObjectUtils.isEmpty(price))
            return;
        DBRecord sale = new DBRecord(model.getContext(), SALES);
        for (int i = (int)(Math.random()*99)+5; i>0; i--)
        {
            int year  = (int)(Math.random()*3)+baseYear;
            int month = (int)(Math.random()*12)+1;
            BigDecimal variation = new BigDecimal((Math.random()*200) - 100.0);
            variation = variation.setScale(2, RoundingMode.HALF_UP);
            sale.create()
                .set(SALES.MODEL_ID, model.getId())
                .set(SALES.YEAR, year)
                .set(SALES.MONTH, month)
                .set(SALES.PRICE, price.add(variation))
                .update();
        }
    }
    
    public static class QueryResult 
    {
        private String brand;
        private String model;
        private BigDecimal basePrice;
        private int salesCount;
        private BigDecimal avgSalesPrice;
        private BigDecimal priceDifference;
        
        public QueryResult(String brand, String model, BigDecimal basePrice
                         , int salesCount, BigDecimal avgSalesPrice, BigDecimal priceDifference)
        {
            this.brand = brand;
            this.model = model;
            this.basePrice = basePrice;
            this.salesCount = salesCount;
            this.avgSalesPrice = avgSalesPrice;
            this.priceDifference = priceDifference;
        }
    }
    
    public void queryDemo(DBContext context)
    {
        /*
        DBCommand cmd = this.createCommand()
           .select(BRAND.NAME.as("BRAND"), MODEL.NAME.as("MODEL"), MODEL.BASE_PRICE.avg(), SALES.MODEL_ID.count(), SALES.PRICE.avg())
           .select(SALES.PRICE.avg().minus(MODEL.BASE_PRICE.avg()).round(2).as("DIFFERENCE"))
           .join(MODEL.BRAND_ID, BRAND.ID)
           .joinLeft(MODEL.ID, SALES.MODEL_ID, SALES.YEAR.is(2021))
           .where(MODEL.ENGINE_TYPE.in(EngineType.H, EngineType.E)) // Hybrid and Electric
           .where(MODEL.BASE_PRICE.isGreaterThan(30000))
           .groupBy(BRAND.NAME, MODEL.NAME)
           .having(SALES.MODEL_ID.count().isGreaterThan(10))
           .orderBy(BRAND.NAME.desc(), MODEL.NAME.asc());
        */
        DBCommand cmd = context.createCommand()
           .selectQualified(BRAND.NAME, MODEL.CONFIG_NAME) 
           .select  (MODEL.BASE_PRICE)
           .select  (SALES.MODEL_ID.count(), SALES.PRICE.avg())
           .select  (SALES.PRICE.avg().minus(MODEL.BASE_PRICE.avg()).round(2))
           .join    (MODEL.BRAND_ID, BRAND.ID)
           .joinLeft(MODEL.ID, SALES.MODEL_ID, SALES.YEAR.is(2021))  // only year 2021
           .where   (MODEL.ENGINE_TYPE.in(EngineType.P, EngineType.H, EngineType.E)) // Petrol, Hybrid, Electric
           .where   (MODEL.BASE_PRICE.isGreaterThan(30000))
           .groupBy (BRAND.NAME, MODEL.CONFIG_NAME, MODEL.BASE_PRICE)
           .having  (SALES.MODEL_ID.count().isGreaterThan(5))
           .orderBy (BRAND.NAME.desc(), MODEL.CONFIG_NAME.asc());
        
        /*
        List<DataListEntry> list = context.getUtils().queryDataList(cmd);
        for (DataListEntry dle : list)
        {
            System.out.println(dle.toString());
        }
        */
        DataListEntry entry = context.getUtils().queryDataEntry(cmd);
        for (int i=0; i<entry.getFieldCount(); i++)
            log.info("col {} -> {}", entry.getColumn(i).getName(), entry.getColumn(i).getBeanPropertyName());
     
        List<QueryResult> list = context.getUtils().queryBeanList(cmd, QueryResult.class, null);
        log.info("queryBeanList returnes {} items", list.size());
        
    }
    
    public void updateDemo(DBContext context)
    {
        
        DBCommand cmd = context.createCommand()
            .set  (MODEL.BASE_PRICE.to(55000))  // set the price-tag
            .join (MODEL.BRAND_ID, BRAND.ID)
            .where(BRAND.NAME.is("Tesla"))
            .where(MODEL.NAME.is("Model 3").and(MODEL.TRIM.is("Performance")));

        // and off you go...
        context.executeUpdate(cmd);
    }
    
}
