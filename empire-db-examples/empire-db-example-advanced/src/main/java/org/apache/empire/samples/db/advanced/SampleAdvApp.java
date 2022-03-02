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

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.Options;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.db.DBCmdParam;
import org.apache.empire.db.DBColumnExpr;
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
    
    // Shortcuts
    private final SampleAdvDB db = new SampleAdvDB();
    private SampleAdvDB.Employees T_EMP = db.T_EMPLOYEES;
    private SampleAdvDB.Departments T_DEP = db.T_DEPARTMENTS;
    private SampleAdvDB.EmployeeDepartmentHistory T_EDH = db.T_EMP_DEP_HIST;

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
        
        // do simple stuff
        simpleUpdateDemo();

        
        simpleQueryDemo();
        queryViewDemo();
        subqueryQueryDemo();
        paramQueryDemo();
        
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
        

        /*
        // STEP 5: Clear Database (Delete all records)
        System.out.println("*** Step 5: clearDatabase() ***");
        clearDatabase();

        // STEP 6: Insert Records
        // Insert Departments
        System.out.println("*** Step 6: inserting departments, employees and employee_department_history records ***");
        int idDevDep  = insertDepartment("Development", "ITTK");
        int idProdDep = insertDepartment("Production", "ITTK");
        int idSalDep  = insertDepartment("Sales", "ITTK");

        // Insert Employees
        int idEmp1 = insertEmployee("Peter", "Sharp", "M");
        int idEmp2 = insertEmployee("Fred", "Bloggs", "M");
        int idEmp3 = insertEmployee("Emma", "White", "F");
        
        // Insert History as batch
        DBSQLScript batch = new DBSQLScript(context);
        insertEmpDepHistory(batch, idEmp1,  idDevDep,  DateUtils.getDate(2007, 12,  1));            
        insertEmpDepHistory(batch, idEmp1,  idProdDep, DateUtils.getDate(2008,  9,  1));           
        insertEmpDepHistory(batch, idEmp1,  idSalDep,  DateUtils.getDate(2009,  5, 15));           

        insertEmpDepHistory(batch, idEmp2,  idSalDep,  DateUtils.getDate(2006,  3,  1));            
        insertEmpDepHistory(batch, idEmp2,  idDevDep,  DateUtils.getDate(2008, 11, 15));
        
        insertEmpDepHistory(batch, idEmp3,  idDevDep,  DateUtils.getDate(2006,  9, 15));            
        insertEmpDepHistory(batch, idEmp3,  idSalDep,  DateUtils.getDate(2007,  6,  1));           
        insertEmpDepHistory(batch, idEmp3,  idProdDep, DateUtils.getDate(2008,  7, 31));
        batch.executeBatch();
        
        // commit
        context.commit();
        
        // STEP 7: read from Employee_Info_View
        System.out.println("--------------------------------------------------------");
        System.out.println("*** read from EMPLOYEE_INFO_VIEW ***");
        DBCommand cmd = context.createCommand();
        cmd.select (db.V_EMPLOYEE_INFO.getColumns());
        cmd.orderBy(db.V_EMPLOYEE_INFO.C_NAME_AND_DEP);
        printQueryResults(cmd);

        // STEP 8: prepared Statement sample
        System.out.println("--------------------------------------------------------");
        System.out.println("*** commandParamsSample: shows how to use command parameters for the generation of prepared statements ***");
        commandParamsSample(idProdDep, idDevDep);

        // STEP 9: bulkReadRecords
        System.out.println("--------------------------------------------------------");
        System.out.println("*** bulkReadRecords: reads employee records into a hashmap, reads employee from hashmap and updates employee ***");
        HashMap<Integer, DBRecord> employeeMap = bulkReadRecords(conn);
        DBRecord rec = employeeMap.get(idEmp2);
        rec.set(db.T_EMPLOYEES.C_SALUTATION, "Mr.");
        rec.update();

        // STEP 10: bulkProcessRecords
        System.out.println("--------------------------------------------------------");
        System.out.println("*** bulkProcessRecords: creates a checksum for every employee in the employees table ***");
        bulkProcessRecords();

        // STEP 11: querySample
        System.out.println("--------------------------------------------------------");
        System.out.println("*** querySample: shows how to use DBQuery class for subqueries and multi table records ***");
        querySample(idEmp2);

        // STEP 12: ddlSample
        System.out.println("--------------------------------------------------------");
        System.out.println("*** ddlSample: shows how to add a column at runtime and update a record with the added column ***");
        if (db.getDbms() instanceof DBMSHandlerH2) {
        	log.info("As H2 does not support changing a table with a view defined we remove the view");
        	System.out.println("*** drop EMPLOYEE_INFO_VIEW ***");
        	DBSQLScript script = new DBSQLScript(context);
        	db.getDbms().getDDLScript(DDLActionType.DROP, db.V_EMPLOYEE_INFO, script);
        	script.executeAll();
        }
        ddlSample(idEmp2);
        if (db.getDbms() instanceof DBMSHandlerH2) {
        	log.info("And put back the view");
        	System.out.println("*** create EMPLOYEE_INFO_VIEW ***");
        	DBSQLScript script = new DBSQLScript(context);
        	db.getDbms().getDDLScript(DDLActionType.CREATE, db.V_EMPLOYEE_INFO, script);
        	script.executeAll();
        }

        // STEP 13: delete records
        System.out.println("--------------------------------------------------------");
        System.out.println("*** deleteRecordSample: shows how to delete records (with and without cascade) ***");
        deleteRecordSample(idEmp3, idSalDep);
        */
        
        // Done
        System.out.println("--------------------------------------------------------");
        System.out.println("DB Sample Advanced finished successfully.");
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
        model.insert(brandVW,   "Golf",     "Golf Style 1,5 l TSI",         "Style",         EngineType.P, 130, 30970d);
        model.insert(brandVW,   "Golf",     "Golf R-Line 2,0 l TSI 4MOTION","R-Line",        EngineType.P, 190, 38650d);
        model.insert(brandVW,   "Tiguan",   "Tiguan Life 1,5 l TSI",        "Life",          EngineType.P, 150, 32545d);
        model.insert(brandVW,   "Tiguan",   "Tiguan Elegance 2,0 l TDI SCR","Elegance",      EngineType.D, 150, 40845d);
        model.insert(brandVW,   "Tiguan",   "Tiguan R-Line 1,4 l eHybrid",  "R-Line",        EngineType.H, 150, 48090d);
        // Tesla
        model.insert(brandTesla,"Model 3",  "Model 3 LR",                   "Long Range",    EngineType.E, 261, 45940d);
        model.insert(brandTesla,"Model 3",  "Model 3 Performance",          "Performance",   EngineType.E, 487, 53940d);
        model.insert(brandTesla,"Model Y",  "Model Y LR",                   "Long Range",    EngineType.E, 345, 53940d);
        model.insert(brandTesla,"Model Y",  "Model Y Performance",          "Performance",   EngineType.E, 450, 58940d);
        model.insert(brandTesla,"Model S",  "Model S Plaid",                "Plaid",         EngineType.E, 1020,0d);
        // Ford
        model.insert(brandFord, "Mustang",  "Mustang GT 5,0 l Ti-VCT V8",           "GT",    EngineType.P, 449, 54300d);
        model.insert(brandFord, "Mustang",  "Mustang Mach1 5,0 l Ti-VCT V8",        "Mach1", EngineType.P, 460, 62800d);
        // Toyota
        model.insert(brandToy,  "Prius",    "Prius Hybrid 1,8-l-VVT-i",             "Basis", EngineType.H, 122, 38000d);    
        model.insert(brandToy,  "Supra",    "GR Supra Pure 2,0 l Twin-Scroll Turbo","Pure",  EngineType.P, 258, 49290d);
        
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
    
    public void simpleUpdateDemo()
    {
        // shortcuts (for convenience)
        CarSalesDB.Brand BRAND = carSales.BRAND;
        CarSalesDB.Model MODEL = carSales.MODEL;
        // create command
        /*
        DBCommand cmd = context.createCommand()
            .set  (MODEL.BASE_PRICE.to(55000))  // set the price-tag
            .join (MODEL.WMI, BRAND.WMI)
            .where(BRAND.NAME.is("Tesla"))
            .where(MODEL.NAME.is("Model 3").and(MODEL.TRIM.is("Performance")));
        */
        
        DBCommand sub = context.createCommand();
        sub.select(BRAND.WMI, BRAND.NAME);
        sub.where (BRAND.COUNTRY.is("Deutschland"));
        DBQuery qqry = new DBQuery(sub, "qt");

        // create command
        DBCommand cmd = context.createCommand()
            // increase model base prices by 5%
            .select(MODEL.BASE_PRICE.multiplyWith(105).divideBy(100).round(0))
            .set  (MODEL.BASE_PRICE.to(55225))
            .join (MODEL.WMI, BRAND.WMI) // , BRAND.NAME.is("Tesla")
            // .join (MODEL.WMI, qqry.column(BRAND.WMI), qqry.column(BRAND.NAME).like("V%"))
            // on all Volkswagen Tiguan with Diesel engine
            .where(BRAND.NAME.like("Volkswagen%"))
            .where(MODEL.NAME.is("Tiguan").and(MODEL.ENGINE_TYPE.is(EngineType.D)));
        

        String sql = cmd.getSelect();
        Object[] params = cmd.getParamValues();
        System.out.println(sql);
        System.out.println(StringUtils.arrayToString(params, "|"));
        
        // cmd.removeWhereConstraint(MODEL.NAME.is("Tiguan").and(MODEL.ENGINE_TYPE.is(EngineType.D)));
        // cmd.removeWhereConstraintOn(BRAND.NAME);
        sql = cmd.getUpdate();
        params = cmd.getParamValues();
        System.out.println(sql);
        System.out.println(StringUtils.arrayToString(params, "|"));
        
        // execute Update statement
        int count = context.executeUpdate(cmd);
        log.info("{} models affected", count);
        
        /*
         * Clone test
        DBCommand cl1 = cmd.clone();
        cl1.set(MODEL.BASE_PRICE.to(66000));  // set the price-tag
        cl1.where(BRAND.NAME.is("Foo"));
        log.info("cmd= {} params={}", cmd.getUpdate(), cmd.getParamValues());
        log.info("cmd= {} params={}", cl1.getUpdate(), cl1.getParamValues());
         */

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
            config.readProperties(dbms, "properties-"+provider, "dbmsHandlerProperites");

            // done
            return dbms;
            
        } catch (Exception e)
        {   // catch any checked exception and forward it
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * This function performs a query to select non-retired employees,<BR>
     * then it calculates a checksum for every record<BR>
     * and writes that checksum back to the database.<BR>
     * <P>
     * @param conn a connection to the database
     */
    private void bulkProcessRecords()
    {
        // Define the query
        DBCommand cmd = context.createCommand();
        // Define shortcuts for tables used - not necessary but convenient
        SampleAdvDB.Employees EMP = T_EMP;
        // Select required columns
        cmd.select(T_EMP.getColumns());
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));

        // Query Records and print output
        DBReader reader = new DBReader(context);
        try
        {
            // Open Reader
            System.out.println("Running Query:");
            System.out.println(cmd.getSelect());
            reader.open(cmd);
            // Print output
            DBRecord record = new DBRecord(context, EMP);
            // Disable rollback handling to improve performance
            record.setRollbackHandlingEnabled(false);
            while (reader.moveNext())
            {
                // Calculate sum
                int sum = 0;
                for (int i=0; i<reader.getFieldCount(); i++)
                    sum += calcCharSum(reader.getString(i));
                // Init updateable record
                reader.initRecord(record);
                // reader
                record.set(T_EMP.C_CHECKSUM, sum);
                record.update();
            }
            // Done
            context.commit();

        } finally
        {
            // always close Reader
            reader.close();
        }
    }
    
	private int calcCharSum(String value)
    {
        int sum = 0;
        if (value!=null)
        {	// calcCharSum
            int len = value.length();
            for (int i=0; i<len; i++)
                sum += value.charAt(i);
        }
        return sum;    
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
     * This function demonstrates the use of the DBQuery object.<BR>
     * First a DBQuery is used to define a subquery that gets the latest employee department history record.<BR>
     * This subquery is then used inside another query to list all employees with the current department.<BR>
     * <P>
     * In the second part, another DBQuery object is used to read a record that holds information from both 
     * the employee and department table. When the information is modified and the record's update method is
     * called, then both tables are updated.
     * <P>
     * @param conn
     * @param employeeId
     */
    private void querySample(int employeeId)
    {
        // Define the sub query
        DBCommand subCmd = context.createCommand();
        DBColumnExpr MAX_DATE_FROM = T_EDH.C_DATE_FROM.max().as(T_EDH.C_DATE_FROM);
        subCmd.select(T_EDH.C_EMPLOYEE_ID, MAX_DATE_FROM);
        subCmd.groupBy(T_EDH.C_EMPLOYEE_ID);
        DBQuery Q_MAX_DATE = new DBQuery(subCmd);

        // Define the query
        DBCommand cmd = context.createCommand();
        // Select required columns
        cmd.select(T_EMP.C_EMPLOYEE_ID, T_EMP.C_FULLNAME);
        cmd.select(T_EMP.C_GENDER, T_EMP.C_PHONE_NUMBER);
        cmd.select(T_DEP.C_DEPARTMENT_ID, T_DEP.C_NAME, T_DEP.C_BUSINESS_UNIT);
        cmd.select(T_EMP.C_UPDATE_TIMESTAMP, T_DEP.C_UPDATE_TIMESTAMP);
        // Set Joins
        cmd.join(T_EDH.C_EMPLOYEE_ID, Q_MAX_DATE.column(T_EDH.C_EMPLOYEE_ID),
                 T_EDH.C_DATE_FROM.is(Q_MAX_DATE.column(MAX_DATE_FROM)));
        cmd.join(T_EMP.C_EMPLOYEE_ID, T_EDH.C_EMPLOYEE_ID);
        cmd.join(T_DEP.C_DEPARTMENT_ID, T_EDH.C_DEPARTMENT_ID);
        // Set Constraints
        cmd.where(T_EMP.C_RETIRED.is(false));
        // Set Order
        cmd.orderBy(T_EMP.C_LASTNAME);
        cmd.orderBy(T_EMP.C_FIRSTNAME);

        // Query Records and print output
        printQueryResults(cmd);
        
        // Define an updateable query
        DBQuery Q_EMP_DEP = new DBQuery(cmd, T_EMP.C_EMPLOYEE_ID);
        DBRecord rec = new DBRecord(context, Q_EMP_DEP);
        // Modify and Update fields from both Employee and Department
        rec.read(employeeId)
           .set(T_EMP.C_PHONE_NUMBER, "0815-4711")
           .set(T_DEP.C_BUSINESS_UNIT, "AUTO")
           .update();
        // Successfully updated
        System.out.println("The employee has been sucessfully updated");
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
            .set(MODEL.SPECIFICATION     , "ID.4 Pro Performance 150 kW 77 kWh")
            .set(MODEL.TRIM            , "Pro")
            .set(MODEL.ENGINE_TYPE     , EngineType.E)
            .set(MODEL.ENGINE_POWER    , 204);
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
     */
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
    
    
}
