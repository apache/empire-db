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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.db.DBCmdParam;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBQuery;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.exceptions.ConstraintViolationException;
import org.apache.empire.db.exceptions.RecordUpdateFailedException;
import org.apache.empire.db.exceptions.StatementFailedException;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.samples.db.advanced.db.CarSalesDB;
import org.apache.empire.samples.db.advanced.db.CarSalesDB.DealerRating;
import org.apache.empire.samples.db.advanced.db.CarSalesDB.DealershipType;
import org.apache.empire.samples.db.advanced.db.CarSalesDB.EngineType;
import org.apache.empire.samples.db.advanced.db.DealerSalesView;
import org.apache.empire.samples.db.advanced.records.BrandRecord;
import org.apache.empire.samples.db.advanced.records.DealerRecord;
import org.apache.empire.samples.db.advanced.records.ModelRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SampleAdvApp 
{
    // Logger
    private static final Logger log = LoggerFactory.getLogger(SampleAdvApp.class);
    
    private static SampleAdvConfig config = new SampleAdvConfig();


    private final CarSalesDB carSales = new CarSalesDB();
    
    private SampleContext context;

    /**
     * <PRE>
     * This is the entry point of the Empire-DB Sample Application
     * Please check the config.xml configuration file for Database and Connection settings.
     * </PRE>
     * @param args arguments
     */
    public static void main(String[] args)
    {
        // Init Configuration
        config.init((args.length > 0 ? args[0] : "config.xml" ));
        // create the app
        SampleAdvApp app = new SampleAdvApp();
        try
        {   // Run
            log.info("Running DB Sample...");
            app.run();
            // Done
            log.info("DB Sample finished successfully.");
        } catch (Exception e) {
            // Error
            log.error("Running SampleApp failed with Exception" + e.toString(), e);
            if (app.context!=null)
                app.context.rollback();
        } finally {
            if (app.context!=null)
                app.context.discard();
        }
    }

    /**
     * This method runs all the example code
     */
    public void run()
    {
        // STEP 1: Get a JDBC Connection
        System.out.println("*** Step 1: getJDBCConnection() ***");
        Connection conn = getJDBCConnection();

        // STEP 2: Choose a dbms
        System.out.println("*** Step 2: getDatabaseProvider() ***");
        DBMSHandler dbms = getDBMSHandler(config.getDatabaseProvider());
        
        // STEP 2.2: Create a Context
        context = new SampleContext(carSales, dbms, conn);
        // set optional context features
        context.setPreparedStatementsEnabled(true);
        context.setRollbackHandlingEnabled(false);

        // STEP 3: Open Database (and create if not existing)
        System.out.println("*** Step 3: openDatabase() ***");
        // db.open(context);
        carSales.open(context);
        if (carSales.wasCreated())
        {   // newly created
            populateDatabase();            
            insertSalesUsingBatch();
            context.commit();
        }
        
        // DQL-Demos
        simpleQueryDemo();
        queryViewDemo();
        subqueryQueryDemo();
        paramQueryDemo();

        // DML-Demos
        modelPriceIncrease();
        modelPriceDecrease();
        dealerRatingUpdate();
        
        // Remember RollbackHandling
        boolean prevRBHandling = context.isRollbackHandlingEnabled(); 
        try {
            // commit pending operations (just to be sure)
            context.commit();
            // run transaction demo
            transactionDemo();
        } finally {
            context.setRollbackHandlingEnabled(prevRBHandling);
        }
        
        cascadeDeleteDemo();
        
        ddlDemo("Beadles Volkswagen", "www.group1auto.co.uk", "https://www.group1auto.co.uk/volkswagen/locations/beadles-volkswagen-dartford");
        
        // Done
        log.info("DB Sample Advanced finished successfully.");
    }

    public void populateDatabase()
    {
        // Add some brands
        BrandRecord brandVW    = (new BrandRecord(context)).insert("WVW", "Volkswagen", "Germany");
        BrandRecord brandFord  = (new BrandRecord(context)).insert("1F",  "Ford",       "USA");
        BrandRecord brandTesla = (new BrandRecord(context)).insert("5YJ", "Tesla",      "USA");
        BrandRecord brandToy   = (new BrandRecord(context)).insert("JT",  "Toyota",     "Japan");
        
        // Add some models
        ModelRecord model = new ModelRecord(context);
        model.insert(brandVW,   "Golf",     "Golf Style 1,5 l TSI",         "Style",         EngineType.P, 130, 30970d, LocalDate.of(2019, 10, 24));
        model.insert(brandVW,   "Golf",     "Golf R-Line 2,0 l TSI 4MOTION","R-Line",        EngineType.P, 190, 38650d, LocalDate.of(2019, 10, 24));
        model.insert(brandVW,   "Tiguan",   "Tiguan Life 1,5 l TSI",        "Life",          EngineType.P, 150, 32545d, LocalDate.of(2016, 01, 01));
        model.insert(brandVW,   "Tiguan",   "Tiguan Elegance 2,0 l TDI SCR","Elegance",      EngineType.D, 150, 40845d, LocalDate.of(2016, 01, 01));
        model.insert(brandVW,   "Tiguan",   "Tiguan R-Line 1,4 l eHybrid",  "R-Line",        EngineType.H, 150, 48090d, LocalDate.of(2016, 01, 01));
        model.insert(brandVW,   "Mulitvan", "Multivan 6.1 Highline 2.0TDI", "Highline",      EngineType.D, 204, 84269d, LocalDate.of(2019, 10, 21));
        // Tesla
        model.insert(brandTesla,"Model 3",  "Model 3 LR",                   "Long Range",    EngineType.E, 261, 45940d, LocalDate.of(2017, 07, 01));
        model.insert(brandTesla,"Model 3",  "Model 3 Performance",          "Performance",   EngineType.E, 487, 53940d, LocalDate.of(2017, 07, 01));
        model.insert(brandTesla,"Model Y",  "Model Y LR",                   "Long Range",    EngineType.E, 345, 53940d, LocalDate.of(2020, 03, 13));
        model.insert(brandTesla,"Model Y",  "Model Y Performance",          "Performance",   EngineType.E, 450, 58940d, LocalDate.of(2020, 03, 13));
        model.insert(brandTesla,"Model S",  "Model S Plaid",                "Plaid",         EngineType.E, 1020,    0d, LocalDate.of(2021, 12, 01));
        // Ford
        model.insert(brandFord, "Mustang",  "Mustang GT 5,0 l Ti-VCT V8",           "GT",    EngineType.P, 449, 54300d, LocalDate.of(2017, 01, 01));
        model.insert(brandFord, "Mustang",  "Mustang Mach1 5,0 l Ti-VCT V8",        "Mach1", EngineType.P, 460, 62800d, LocalDate.of(2020, 10, 16));
        // Toyota
        model.insert(brandToy,  "Prius",    "Prius Hybrid 1,8-l-VVT-i",             "Basis", EngineType.H, 122, 38000d, LocalDate.of(2017, 01, 01));    
        model.insert(brandToy,  "Supra",    "GR Supra Pure 2,0 l Twin-Scroll Turbo","Pure",  EngineType.P, 258, 49290d, LocalDate.of(2020, 03, 31));
        
        // Add some dealers
        DealerRecord dealerDE = (new DealerRecord(context)).insert("Autozentrum Schmitz",      "Munich",       "Germany");
        DealerRecord dealerUK = (new DealerRecord(context)).insert("Beadles Volkswagen",       "Dartford",     "United Kingdom");
        DealerRecord dealerUS = (new DealerRecord(context)).insert("Auto Nation",              "Los Angeles",  "USA");
        DealerRecord dealerFR = (new DealerRecord(context)).insert("Vauban Motors Argenteuil", "Paris",        "France");
        DealerRecord dealerIT = (new DealerRecord(context)).insert("Autorigoldi S.p.A.",       "Milan",        "Italy");
        
        // Add brands to dealers
        CarSalesDB.DealerBrands DB = carSales.DEALER_BRANDS;
        DBRecord rec = new DBRecord(context, DB);
        rec.create(DBRecord.key(dealerDE, brandTesla)).set(DB.DEALERSHIP_TYPE, DealershipType.B).update();
        rec.create(DBRecord.key(dealerDE, brandVW))   .set(DB.DEALERSHIP_TYPE, DealershipType.U).update();
        rec.create(DBRecord.key(dealerUK, brandVW))   .set(DB.DEALERSHIP_TYPE, DealershipType.B).update();
        rec.create(DBRecord.key(dealerUK, brandFord)) .set(DB.DEALERSHIP_TYPE, DealershipType.I).update();
        rec.create(DBRecord.key(dealerUS, brandFord)) .set(DB.DEALERSHIP_TYPE, DealershipType.B).update();
        rec.create(DBRecord.key(dealerUS, brandTesla)).set(DB.DEALERSHIP_TYPE, DealershipType.I).update();
        rec.create(DBRecord.key(dealerFR, brandToy))  .set(DB.DEALERSHIP_TYPE, DealershipType.B).update();
        rec.create(DBRecord.key(dealerIT, brandToy))  .set(DB.DEALERSHIP_TYPE, DealershipType.G).update();
        rec.create(DBRecord.key(dealerIT, brandVW))   .set(DB.DEALERSHIP_TYPE, DealershipType.B).update();
        
    }
    
    private void insertSalesUsingBatch()
    {
        CarSalesDB db = this.carSales;
        DBSQLScript batch = new DBSQLScript(context);
        // get list of all dealers
        DBCommand cmd = context.createCommand();
        cmd.select(db.DEALER.ID);
        List<Long> dealerIdList = context.getUtils().querySimpleList(Long.class, cmd);
        // get all models
        cmd.clear();
        cmd.select(db.MODEL.ID, db.MODEL.SPECIFICATION, db.MODEL.BASE_PRICE);  // select the ones we need (optional)
        // no constraints on model
        List<DataListEntry> modelList = context.getUtils().queryDataList(cmd);
        for (DataListEntry model : modelList)
        {
            int count = generateRandomSales(batch, model, dealerIdList);
            log.info("{} Sales added for Model {}", count, model.getString(db.MODEL.SPECIFICATION));
        }
        // execute the batch
        int count = batch.executeBatch();
        log.info("{} Sales added with batch", count);
    }
    
    private int generateRandomSales(DBSQLScript batch, DataListEntry model, List<Long> dealerIdList)
    {
        CarSalesDB db = this.carSales;
        BigDecimal price = model.getDecimal(db.MODEL.BASE_PRICE);
        if (ObjectUtils.isEmpty(price))
            return 0;
        int count = 0;
        int baseYear = LocalDate.now().getYear()-3;
        DBCommand cmd = context.createCommand();
        for (int i = (int)(Math.random()*99)+25; i>0; i--)
        {
            int dealIdx = (int)(Math.random()*dealerIdList.size());
            int year  = (int)(Math.random()*3)+baseYear;
            int month = (int)(Math.random()*12)+1;
            BigDecimal variation = new BigDecimal((Math.random()*200) - 100.0);
            variation = variation.setScale(2, RoundingMode.HALF_UP);
            cmd.set(db.SALES.MODEL_ID.to(model.getRecordId(db.MODEL)))
               .set(db.SALES.DEALER_ID.to(dealerIdList.get(dealIdx)))
               .set(db.SALES.YEAR.to(year))
               .set(db.SALES.MONTH.to(month))
               .set(db.SALES.PRICE.to(price.add(variation)));
            // add to batch
            batch.addInsert(cmd);
            count++;
        }
        return count;
    }
    
    /**
     * The result Bean for the query Example
     * @author rainer
     */
    public static class QueryResult implements Serializable
    {
        private static final long serialVersionUID = 1L;
        
        private String brand;
        private String model;
        private BigDecimal basePrice;
        private int salesCount;
        private BigDecimal avgSalesPrice;
        private BigDecimal priceDifference;

        /**
         * Fields constructor for QueryResult
         * Must match the SELECT phrase and will be used by queryBeanList()
         * @param brand
         * @param model
         * @param basePrice
         * @param salesCount
         * @param avgSalesPrice
         * @param priceDifference
         */
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

        public String getBrand()
        {
            return brand;
        }

        public String getModel()
        {
            return model;
        }

        public BigDecimal getBasePrice()
        {
            return basePrice;
        }

        public int getSalesCount()
        {
            return salesCount;
        }

        public BigDecimal getAvgSalesPrice()
        {
            return avgSalesPrice;
        }

        public BigDecimal getPriceDifference()
        {
            return priceDifference;
        }
    }

    public void subqueryQueryDemo()
    {
        // shortcuts (for convenience)
        CarSalesDB.Model MODEL = carSales.MODEL;
        CarSalesDB.Sales SALES = carSales.SALES;

        // create command
        // DBColumnExpr SALES_COUNT = SALES.count();
        DBCommand sub = context.createCommand()
           .selectQualified(SALES.MODEL_ID, SALES.count())
           .groupBy(SALES.MODEL_ID);
        DBQuery q = new DBQuery(sub);
        
        // create command
        DBCommand cmd = context.createCommand()
           .select(MODEL.SPECIFICATION, q.column(SALES.count()))
           .join(MODEL.ID, q.column(SALES.MODEL_ID));
        
        List<DataListEntry> list = context.getUtils().queryDataList(cmd);
        for (DataListEntry dle : list)
        {
            System.out.println(dle.toString());
        }
        
    }
    
    public void simpleQueryDemo()
    {
        // shortcuts (for convenience)
        CarSalesDB.Brand BRAND = carSales.BRAND;
        CarSalesDB.Model MODEL = carSales.MODEL;
        CarSalesDB.Sales SALES = carSales.SALES;

        // .selectQualified(BRAND.NAME, MODEL.CONFIG_NAME) 
        
        // create a command
        DBCommand cmd = context.createCommand()
           .select  (BRAND.NAME, MODEL.SPECIFICATION, MODEL.BASE_PRICE)
           .select  (SALES.MODEL_ID.count(), SALES.PRICE.avg())
           .select  (SALES.PRICE.avg().minus(MODEL.BASE_PRICE.avg()).round(2).as("DIFFERENCE"))
           .join    (MODEL.WMI, BRAND.WMI)
           .joinLeft(MODEL.ID, SALES.MODEL_ID, SALES.YEAR.is(2021))  // only year 2021
           .where   (MODEL.ENGINE_TYPE.in(EngineType.P, EngineType.H, EngineType.E)) // Petrol, Hybrid, Electric
           .where   (MODEL.BASE_PRICE.isGreaterThan(30000))
           .groupBy (BRAND.NAME, MODEL.SPECIFICATION, MODEL.BASE_PRICE)
           .having  (SALES.MODEL_ID.count().isGreaterThan(5))   // more than 5 sales
           .orderBy (BRAND.NAME.desc(), MODEL.SPECIFICATION.asc());
             
        // Return a list of Java beans (needs matching fields constructor or setter methods)           
        // This is just one of several options to obtain an process query results          
        List<QueryResult> list = context.getUtils().queryBeanList(cmd, QueryResult.class, null);
        log.info("queryBeanList returnes {} items", list.size());
        
        /*
        List<DataListEntry> list = context.getUtils().queryDataList(cmd);
        for (DataListEntry dle : list)
        {
            System.out.println(dle.toString());
        }
        */
        /*
        DataListEntry entry = context.getUtils().queryDataEntry(cmd);
        for (int i=0; i<entry.getFieldCount(); i++)
            log.info("col {} -> {}", entry.getColumn(i).getName(), entry.getColumn(i).getBeanPropertyName());
        */    
    }

    public void paramQueryDemo()
    {
        // shortcuts (for convenience)
        CarSalesDB.Brand BRAND = carSales.BRAND;
        CarSalesDB.Model MODEL = carSales.MODEL;
        
        // create command
        DBCommand cmd = context.createCommand();
        // create params
        DBCmdParam brandParam = cmd.addParam();
        DBCmdParam engineTypeParam = cmd.addParam();
                
        // create the command
        cmd.select  (BRAND.NAME, MODEL.SPECIFICATION, MODEL.BASE_PRICE, MODEL.ENGINE_TYPE, MODEL.ENGINE_POWER)
           .join    (MODEL.WMI, BRAND.WMI)
           .where   (BRAND.NAME.is(brandParam))
           .where   (MODEL.ENGINE_TYPE.is(engineTypeParam))
           .orderBy (BRAND.NAME.desc(), MODEL.SPECIFICATION.asc());

        // set the params
        brandParam.setValue("Tesla");
        engineTypeParam.setValue(EngineType.E);
        // and run
        List<DataListEntry> list = context.getUtils().queryDataList(cmd);
        for (DataListEntry dle : list)
            System.out.println(dle.toString());
        
        // change params
        brandParam.setValue("Volkswagen");
        engineTypeParam.setValue(EngineType.P);
        // and run again
        list = context.getUtils().queryDataList(cmd);
        for (DataListEntry dle : list)
            System.out.println(dle.toString());
        
    }
    
    public void modelPriceIncrease()
    {
        // shortcuts (for convenience)
        CarSalesDB.Brand BRAND = carSales.BRAND;
        CarSalesDB.Model MODEL = carSales.MODEL;
        // create command

        // Sales-Info
        String salesInfo = "Price update "+DateUtils.formatDate(LocalDate.now(), Locale.US);
        // create command
        DBCommand cmd = context.createCommand()
            // increase model base prices by 5%
            .select(BRAND.NAME, MODEL.SPECIFICATION,  MODEL.BASE_PRICE, MODEL.BASE_PRICE.multiplyWith(105).divideBy(100).round(2).as("NEW_PRICE"))
            // set update fields
            .set   (MODEL.BASE_PRICE.to(MODEL.BASE_PRICE.multiplyWith(105).divideBy(100).round(2)))
            .set   (MODEL.SALES_INFO.to(salesInfo))
            // join with BRANDS
            .join  (MODEL.WMI, BRAND.WMI) // , BRAND.NAME.is("Tesla")
            // on all Volkswagen with Diesel engine
            .where(BRAND.NAME.upper().like("VOLKSWAGEN"))
            .where(MODEL.ENGINE_TYPE.is(EngineType.D)); // (MODEL.NAME.is("Tiguan").and(
        
        // Preview the change
        for (DataListEntry item : context.getUtils().queryDataList(cmd))
            System.out.println(item.toString());

        /*
        // cmd.removeWhereConstraint(MODEL.NAME.is("Tiguan").and(MODEL.ENGINE_TYPE.is(EngineType.D)));
        // cmd.removeWhereConstraintOn(BRAND.NAME);
        sql = cmd.getUpdate();
        params = cmd.getParamValues();
        System.out.println(sql);
        System.out.println(StringUtils.arrayToString(params, "|"));
        */
        
        // Execute the change
        int count = context.executeUpdate(cmd);
        log.info("{} models affected", count);
        context.commit();
    }
    
    public void modelPriceDecrease()
    {
        // shortcuts (for convenience)
        CarSalesDB.Brand BRAND = carSales.BRAND;
        CarSalesDB.Model MODEL = carSales.MODEL;
        // create command
        DBCommand cmd = context.createCommand()
            .select(MODEL.getColumns())
            .join (MODEL.WMI, BRAND.WMI)
            .where(BRAND.NAME.upper().like("VOLKSWAGEN"))
            .where(MODEL.ENGINE_TYPE.is(EngineType.D)); // (MODEL.NAME.is("Tiguan").and(

        // Use DBReader to process query result
        DBReader reader = new DBReader(context);
        try
        {   // Open Reader
            log.info("running modelPriceDecrease for cmd {}", cmd.getSelect());
            reader.open(cmd);
            // Print output
            ModelRecord record = new ModelRecord(context);
            // Disable rollback handling to improve performance
            record.setRollbackHandlingEnabled(false);
            BigDecimal factor = (new BigDecimal(1.05d)).setScale(2, RoundingMode.HALF_UP);
            while (reader.moveNext())
            {   // Init updateable record
                reader.initRecord(record);
                // Calculate Base-Price
                BigDecimal oldPrice = reader.getDecimal(MODEL.BASE_PRICE);
                BigDecimal newPrice = oldPrice.divide(factor, 2, RoundingMode.HALF_UP);
                record.set(MODEL.BASE_PRICE, newPrice);
                // update
                record.update();
            }
            // Done
            context.commit();
        } finally {
            // always close Reader
            reader.close();
        }
    }

    private void dealerRatingUpdate()
    {
        CarSalesDB.Dealer DEALER = carSales.DEALER;
        // Clear all dealer ratings
        DBCommand cmd = context.createCommand()
                .set(DEALER.RATING.to(DealerRating.X))
                .where(DEALER.RATING.isNot(null));
        context.executeUpdate(cmd);
        context.commit();
        
        // Subquery to find TOP 3 dealers
        CarSalesDB.Sales SALES = carSales.SALES;
        DBCommand qryCmd = context.createCommand()
            .select(SALES.DEALER_ID)
            .where(SALES.YEAR.is(LocalDate.now().getYear()-1))
            .groupBy(SALES.DEALER_ID)
            .orderBy(SALES.PRICE.sum().desc())
            .limitRows(3);
        DBQuery qryTop = new DBQuery(qryCmd, "qtop");
        
        // Dealer-Query
        cmd.clear();
        cmd.join(DEALER.ID, qryTop.column(SALES.DEALER_ID));
        
        int index = 0;
        List<DealerRecord> list = context.getUtils().queryRecordList(cmd, DEALER, DealerRecord.class);
        for (DealerRecord dealer : list)
        {
             DealerRating oldRating = dealer.getEnum(DEALER.RATING);
             DealerRating newRating = DealerRating.values()[index++];
             log.info("Dealer \"{}\" rating changed from {} to {}", dealer.getString(DEALER.COMPANY_NAME), oldRating, newRating);
             dealer.set(DEALER.RATING, newRating);
             dealer.update();
        }
        context.commit();
    }
    
    private void queryViewDemo()
    {
        // query all
        /*
        DBCommand cmd = context.createCommand()
           .select  (DSV.getColumns())
           .orderBy (DSV.SALE_YEAR, DSV.DEALER_COUNTRY);
        List<DataListEntry> list = context.getUtils().queryDataList(cmd);
        for (DataListEntry dle : list)
        {
            System.out.println(dle.toString());
        }
        */
        
        // shortcuts (for convenience)
        DealerSalesView DSV = carSales.DEALER_SALES_VIEW;

        DBCommand cmd = context.createCommand();
        // Detect if Brand sold is the Dealer's Brand
        DBCompareExpr IS_DEALER_BRAND = DSV.DEALERSHIP_TYPE.in(DealershipType.B, DealershipType.F);
        // select
        cmd.select(DSV.SALE_YEAR, DSV.DEALER_NAME, DSV.DEALER_COUNTRY);
        cmd.select(DSV.SALE_COUNT.sum().as("TOTAL_SALES"));
        cmd.select(carSales.caseWhen(IS_DEALER_BRAND, DSV.TURNOVER, 0).sum().as("TURNOVER_BRAND"));
        cmd.select(DSV.TURNOVER.sum().as("TURNOVER_TOTAL"));
        // group
        cmd.groupBy(DSV.SALE_YEAR, DSV.DEALER_NAME, DSV.DEALER_COUNTRY);
        // order
        cmd.orderBy(DSV.SALE_YEAR, DSV.DEALER_NAME);

        // query and print result
        List<DataListEntry> list = context.getUtils().queryDataList(cmd);
        for (DataListEntry dle : list)
        {
            System.out.println(dle.toString());
        }
    }
    
    /**
     * <PRE>
     * Opens and returns a JDBC-Connection.
     * JDBC url, user and password for the connection are obtained from the SampleConfig bean
     * Please use the config.xml file to change connection params.
     * </PRE>
     */
    private static Connection getJDBCConnection()
    {
        // Establish a new database connection
        Connection conn = null;
        try
        {
            // Connect to the database
            String jdbcDriverClass = config.getJdbcClass();
            log.info("Creating JDBC-Driver of type \"{}\"", jdbcDriverClass);
            Class.forName(jdbcDriverClass).newInstance();

            log.info("Connecting to Database'" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
            conn = DriverManager.getConnection(config.getJdbcURL(), config.getJdbcUser(), config.getJdbcPwd());
            log.info("Connected successfully");
            // set the AutoCommit to false this session. You must commit
            // explicitly now
            conn.setAutoCommit(false);
            log.info("AutoCommit is " + conn.getAutoCommit());

        } catch (Exception e)
        {
            log.error("Failed to connect to '" + config.getJdbcURL() + "' / User=" + config.getJdbcUser());
            log.error(e.toString());
            throw new RuntimeException(e);
        }
        return conn;
    }

    /**
     * Creates an Empire-db DatabaseDriver for the given provider and applies dbms specific configuration 
     */
    private static DBMSHandler getDBMSHandler(String provider)
    {
        try
        {   // Get Driver Class Name
            String dbmsHandlerClass = config.getDbmsHandlerClass();
            if (StringUtils.isEmpty(dbmsHandlerClass))
                throw new RuntimeException("Configuration error: Element 'dbmsHandlerClass' not found in node 'properties-"+provider+"'");

            // Create dbms
            DBMSHandler dbms = (DBMSHandler) Class.forName(dbmsHandlerClass).newInstance();

            // Configure dbms
            config.readProperties(dbms, "properties-"+provider, "dbmsHandlerProperties");

            // done
            return dbms;
            
        } catch (Exception e)
        {   // catch any checked exception and forward it
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * This method demonstrates how to add, modify and delete a database column.<BR>
     * This function demonstrates the use of the {@link DBMSHandler#getDDLScript(org.apache.empire.db.DDLActionType, org.apache.empire.db.DBObject, DBSQLScript)}<BR>
     * 
     */
    private void ddlDemo(String dealerName, String websiteDomain, String websiteUrl)
    {
        // create shortcuts
        DBMSHandler dbms = context.getDbms();
        CarSalesDB.Dealer DEALER = carSales.DEALER;

        // First, add a new column to the Table object
        DBTableColumn WEBSITE = DEALER.addColumn("WEBSITE", DataType.VARCHAR, 20, false);

        // Now create the corresponding DDL statement 
        log.info("Create new column named WEBSITE as varchar(20) for the DEALER table");
        DBSQLScript script = new DBSQLScript(context);
        dbms.getDDLScript(DDLActionType.CREATE, WEBSITE, script);
        script.executeAll();
        
        // Now load a record from that table and set the value for the website
        log.info("Set the value for the WEBSITE field for dealer \"{}\"", dealerName);
        DealerRecord rec = new DealerRecord(context);
        rec.read(DEALER.COMPANY_NAME.is(dealerName));
        rec.set (WEBSITE, websiteDomain);
        rec.update();
        
        // Now extend the size of the field from 40 to 80 characters
        log.info("Extend the size of column WEBSITE from 20 to 80 characters");
        WEBSITE.setSize(80); 
        script.clear();
        dbms.getDDLScript(DDLActionType.ALTER, WEBSITE, script);
        script.executeAll();

        // Now set a longer value for the record
        log.info("Change the value for the WEBSITE to a longer string \"{}\"", websiteUrl);
        rec.set(WEBSITE, websiteUrl);
        rec.update();

        // Finally, drop the column again
        log.info("Drop the WEBSITE column from the DEALER table:");
        script.clear();
        dbms.getDDLScript(DDLActionType.DROP, WEBSITE, script);
        script.executeAll();
    }

    /**
     * testTransactionCreate
     * @param context
     * @param idDep
     */
    private void transactionDemo()
    {
        // Set RollbackHandlingEnabled to false if you want to play with fire!
        // Note: You must set the RollbackHandling flag before creating a Record instance 
        context.setRollbackHandlingEnabled(true);

        // User a model record
        CarSalesDB.Model MODEL = carSales.MODEL;
        ModelRecord model = new ModelRecord(context);
        
        /*
         * Part 1
         */
        
        // Insert a new model
        log.info("Insert a new Model");
        model.create()
            .set(MODEL.WMI             , "WVW")  // = Volkswagen
            .set(MODEL.NAME            , "ID.4")
            .set(MODEL.SPECIFICATION   , "ID.4 Pro Performance 150 kW 77 kWh")
            .set(MODEL.TRIM            , "Pro")
            .set(MODEL.ENGINE_TYPE     , EngineType.E)
            .set(MODEL.ENGINE_POWER    , 204)
            .set(MODEL.FIRST_SALE      , LocalDate.of(2021, 04, 04));
        // State and timestampe before and after insert
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        model.update();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));

        // Update even once again without committing
        model.set(MODEL.BASE_PRICE, 44915);
        model.update();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        // now, do the rollback
        log.info("Performing rollback");
        context.rollback();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        log.info("Update Model again, even though its not in the database because of the rollback, hence Model will be inserted again.");
        log.info("This will fail, if Rollback handling is not enabled!");
        try {
            model.set(MODEL.BASE_PRICE, 44900); // Only to mark the record as modified if RollbackHandling is disabled!
            model.update();
        } catch(RecordUpdateFailedException e) {
            log.error("Model update failed since rollback handling was not enabled for this context!", e);
            return; // the rest will fail too
        }
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));

        /*
         * Part 2
         */
        
        // Delete the model
        model.delete();
        log.debug("Record state={}", model.getState());
        
        log.info("Performing rollback again");
        context.rollback();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));

        // Final update
        log.info("Update Model again, even though a delete was executed but was not committed.");
        log.info("This will once again lead to an insert as the insert was still not committed");
        log.info("This will also fail, if Rollback handling is not enabled!");
        try {
            model.update();
        } catch(RecordUpdateFailedException e) {
            log.error("Model update failed since rollback handling was not enabled for this context!", e);
            return; // the rest will fail too
        }
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        // Finally commit
        log.info("Finally commit the transaction in order to finalize the Model insert");
        context.commit();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        /*
         * Part 3
         */
        
        // Update example with two consecutive updates
        log.info("Fist update will change the timestamp");
        model.set(MODEL.BASE_PRICE, 44000);
        model.update();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        log.info("Second update will change the timestamp again");
        model.set(MODEL.BASE_PRICE, 42660);
        model.update();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        log.info("Performing rollback for the previous two updates");
        context.rollback();
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        log.info("Update Model again, even though the previous two updates have been rolled back");
        log.info("This will again fail, if Rollback handling is not enabled!");
        try {
            model.update();
        } catch(RecordUpdateFailedException e) {
            log.error("Model update failed since rollback handling was not enabled for this context!", e);
        }
        log.debug("Record state={}, key={}, Timestamp={}", model.getState(), model.getKey(), model.get(MODEL.UPDATE_TIMESTAMP));
        
        // But now delete the model again, so we can rerun the example later
        model.delete();
        context.commit();
    }
   
    /**
     * This function demonstrates cascaded deletes.
     * See DBRelation.setOnDeleteAction()
     *  
     * @param idEmployee the id of the employee to delete
     * @param idDepartment the id of the department to delete
     * @param conn the connection
     */
    private void cascadeDeleteDemo()
    {
        // Read a brand record
        BrandRecord brand = new BrandRecord(context);
        brand.read(carSales.BRAND.WMI.is("WVW")); // = Volkswagen
        
        // Read dealer record
        DealerRecord dealer = new DealerRecord(context);
        dealer.read(carSales.DEALER.COMPANY_NAME.is("Autorigoldi S.p.A."));

        try {
            // This delete is designed to succeed since cascaded deletes are enabled for this relation.
            // see DEALER_BRANDS and SALES foreign key definition in CarSalesDB:
            //   DEALER_ID = addForeignKey("DEALER_ID", db.DEALER,  true,  true );  // Delete Cascade on
            log.info("Deleting DEALER expecting to succeed since relationship to DEALER_BRANDS and SALES is cascading");
            dealer.delete();

            // This delete is designed to succeed since cascaded deletes are enabled for this relation.
            log.info("Deleting BRAND expecting to fail since relationship to DEALER_BRANDS and MODEL is NOT cascading");
            brand.delete();
            
        } catch(ConstraintViolationException e) {
            log.info("Delete operation failed due to existing depending records.");
        } catch(StatementFailedException e) {
            // Oops, the driver threw a SQLException instead
            log.info("Delete operation failed with SQLException {}", e.getMessage());
        } finally {
            // we don't really want to do this
            context.rollback();
        }
    }

    /**
     * This functions prints the results of a query which is performed using the supplied command
     * @param cmd the command to be used for performing the query
     * @param conn the connection
    private void printQueryResults(DBCommand cmd)
    {
        // Query Records and print output
        DBReader reader = new DBReader(context);
        try
        {   // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            reader.open(cmd);
            // Print column titles 
            System.out.println("---------------------------------");
            int count = reader.getFieldCount();
            for (int i=0; i<count; i++)
            {   // Print all column names
                DBColumnExpr c = reader.getColumn(i);
                if (i>0)
                    System.out.print("\t");
                System.out.print(c.getName());
            }
            // Print output
            System.out.println("");
            // Text-Output by iterating through all records.
            while (reader.moveNext())
            {
                for (int i=0; i<count; i++)
                {   // Print all field values
                    if (i>0)
                        System.out.print("\t");
                    // Check if conversion is necessary
                    DBColumnExpr c = reader.getColumn(i);
                    Options opt = c.getOptions();
                    if (opt!=null)
                    {   // Option Lookup
                        System.out.print(opt.get(reader.getValue(i)));
                    }
                    else
                    {   // Print String
                        System.out.print(reader.getString(i));
                    }
                }
                System.out.println("");
            }

        } finally
        {   // always close Reader
            reader.close();
        }
    }
     */
    
    
}
