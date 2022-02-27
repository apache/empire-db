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

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBTable;
import org.apache.empire.exceptions.ItemNotFoundException;

/**
 * Temporary class for HTML-generation of code and SQL
 * Will be removed again later
 * @author rainer
 */
public class HtmlGenUtil
{
    public static final String codeModelQuery = "// create command\r\n" + 
            "DBCommand cmd = context.createCommand()\r\n" + 
            "   .select  (BRAND.NAME, MODEL.CONFIG_NAME, MODEL.BASE_PRICE)\r\n" + 
            "   .select  (SALES.MODEL_ID.count(), SALES.PRICE.avg())\r\n" + 
            "   .select  (SALES.PRICE.avg().minus(MODEL.BASE_PRICE.avg()).round(2).as(\"DIFFERENCE\"))\r\n" + 
            "   .join    (MODEL.WMI, BRAND.WMI)\r\n" + 
            "   .joinLeft(MODEL.ID, SALES.MODEL_ID, SALES.YEAR.is(2021))  // only year 2021\r\n" + 
            "   .where   (MODEL.ENGINE_TYPE.in(EngineType.P, EngineType.H, EngineType.E)) // Petrol, Hybrid, Electric\r\n" + 
            "   .where   (MODEL.BASE_PRICE.isGreaterThan(30000))\r\n" + 
            "   .groupBy (BRAND.NAME, MODEL.CONFIG_NAME, MODEL.BASE_PRICE)\r\n" + 
            "   .having  (SALES.MODEL_ID.count().isGreaterThan(5))   // more than 5 sales\r\n" + 
            "   .orderBy (BRAND.NAME.desc(), MODEL.CONFIG_NAME.asc());\r\n" + 
            "     \r\n" + 
            "// Returns a list of Java beans (needs matching fields constructor or setter methods)           \r\n" + 
            "// This is just one of several options to obtain an process query results          \r\n" + 
            "List<QueryResult> list = context.getUtils().queryBeanList(cmd, QueryResult.class, null);\r\n" + 
            "log.info(\"queryBeanList returned {} items\", list.size());"; 
    
    public static final String codePriceUpdate = "// create command\r\n" + 
            "DBCommand cmd = context.createCommand()\r\n" + 
            "    // increase model base prices by 5%\r\n" + 
            "    .set  (MODEL.BASE_PRICE.to(MODEL.BASE_PRICE.multiplyWith(105).divideBy(100).round(0)))\r\n" + 
            "    .join (MODEL.WMI, BRAND.WMI)\r\n" + 
            "    // on all Volkswagen Tiguan with Diesel engine\r\n" + 
            "    .where(BRAND.NAME.is(\"Volkswagen\"))\r\n" + 
            "    .where(MODEL.NAME.is(\"Tiguan\").and(MODEL.ENGINE_TYPE.is(EngineType.D)));\r\n" + 
            "\r\n" + 
            "// execute Update statement\r\n" + 
            "int count = context.executeUpdate(cmd);\r\n" + 
            "log.info(\"{} models affected\", count);"; 

    public static final String codeCodegen = "// create the config\r\n" + 
            "CodeGenConfig config = new CodeGenConfig();\r\n" + 
            "// use API or load from file using \r\n" + 
            "config.setPackageName(\"com.mycompany.myproject.database\");\r\n" + 
            "config.set(...)\r\n" + 
            "// get the DBMS\r\n" + 
            "DBMSHandler dbms = new DBMSHandlerOracle();\r\n" + 
            "// get the JDBC-Connection\r\n" + 
            "Connection conn = getJDBCConnection(config);\r\n" + 
            "// Generate code from database\r\n" + 
            "CodeGenerator app = new CodeGenerator();\r\n" + 
            "app.generate(dbms, conn, config);\r\n";
    
    public static final Object[] codeRecordReadLiterals = new Object[] { 55, 2021, 12, "Anna", "Smith" };
    public static final String codeRecordRead="DBRecord employee = new DBRecord(context, db.EMPLOYEES);\r\n" + 
            "SampleDB.Employees EMP = db.EMPLOYEES;\r\n" + 
            "// read record with identity column primary key\r\n" + 
            "employee.read(55);\r\n" + 
            "// read record with multiple column primary key \r\n" + 
            "payment.read(DBRecord.key(55, 2021, 12));\r\n" + 
            "// read with constraints \r\n" + 
            "employee.read(EMP.FIRST_NAME.is(\"Anna\").and(EMP.LAST_NAME.is(\"Smith\")));\r\n" + 
            "// read record identified by a subquery\r\n" + 
            "DBCommand sel = context.createCommand();\r\n" + 
            "sel.select(db.PAYMENTS.EMPLOYEE_ID);\r\n" + 
            "sel.where(/* some constraints */);\r\n" + 
            "employee.read(EMP.ID.is(sel));\r\n" + 
            "// read record partially with only 3 columns\r\n" + 
            "employee.read(DBRecord.key(55), PartialMode.INCLUDE, EMP.FIRST_NAME, EMP.LAST_NAME, EMP.SALARY);\r\n";
    
    public static String codeToHtml(DBDatabase db, String code, Object... literals)
    {
        code = prepareHtml(code);
        // comment
        code = replaceFragment(code, "/*", "*/", "<span class=\"comment\">", "</span>");
        code = replaceFragment(code, "//", "\r\n", "<span class=\"comment\">", "</span>");
        // replace literals
        for (int i=0; i<literals.length; i++)
        {
            String literal;
            if (literals[i] instanceof String)
                literal = "\""+((String)literals[i])+"\"";
            else
                literal = String.valueOf(literals[i]);
            code = replaceWord(code, literal, false, "<span class=\"literal\">", "</span>");
        }
        // null
        code = replaceWord(code, "null", false, "<span class=\"literal\">", "</span>");
        code = replaceWord(code, "new",  false, "<span class=\"keyword\">", "</span>");
        code = replaceWord(code, ".class", true, "<span class=\"keyword\">", "</span>");
        // types
        String[] types = new String[] { "int ", "CarSalesDB", "Connection ", "DBUtils", "DBCommand", "DBRecord", "PartialMode", "EngineType", "long ", "String ", "List<", "QueryResult" };
        for (int i=0; i<types.length; i++)
            code = replaceWord(code, types[i], true, "<span class=\"type\">", "</span>");
        // variables
        String[] variables = new String[] { "db_XXX", "context", "cmd", "record", "utils", "employee", "list", "result"};
        for (int i=0; i<variables.length; i++)
            code = replaceWord(code, variables[i], false, "<span class=\"var\">", "</span>");
        // Tables and columns
        if (db!=null)
        {   for (DBTable t : db.getTables())
            {
                String table = t.getName();
                code = replaceWord(code, table, false, "<span class=\"obj\">", "</span>");
                code = replaceWord(code, table.substring(0,3)+"_XXX", false, "<span class=\"obj\">", "</span>");
                for (DBColumn c : t.getColumns())
                {
                    code = replaceWord(code, "."+c.getName(), true, "<span class=\"field\">", "</span>");
                }
            }
        }
        // functions
        code = replaceFunction(code, '.', '(', "<span class=\"func\">", "</span>");
        // shorten
        return code.replace("_XXX", "");
    }
    
    public static String sqlToHtml(DBDatabase db, String sql, Object... literals)
    {
        sql = prepareHtml(sql);
        sql = sql.replace("N'", "'");
        /*
        UPDATE t2
        SET BASE_PRICE=round(t2.BASE_PRICE*105/100,0)
        FROM MODEL t2 INNER JOIN BRAND t1 ON t1.WMI = t2.WMI
        WHERE t1.NAME='Volkswagen' AND t2.NAME='Tiguan' AND t2.ENGINE_TYPE='D'
        */
        String[] words = new String[] { "SELECT ", "UPDATE ", "INSERT ", "SET" , "FROM ", "WHERE ", "GROUP BY ", "HAVING ", "ORDER BY", " IN ", " ON ", " AND ", " INNER JOIN ", " LEFT JOIN ", " RIGHT_JOIN " };
        for (int i=0; i<words.length; i++)
            sql = replaceWord(sql, words[i], true, "<span class=\"word\">", "</span>");
        for (DBTable t : db.getTables())
            sql = replaceWord(sql, t.getAlias(), false, "<span class=\"alias\">", "</span>");
        // functions
        String[] func = new String[] { "count(", "round(", "avg(" };
        for (int i=0; i<func.length; i++)
            sql = replaceWord(sql, func[i], true, "<span class=\"func\">", "</span>");
        // finally literals
        for (int i=0; i<literals.length; i++)
        {
            String literal;
            if (literals[i] instanceof String)
                literal = "'"+((String)literals[i])+"'";
            else
                literal = String.valueOf(literals[i]);
            sql = replaceWord(sql, literal, false, "<span class=\"param\"><span class=\"literal\">", "</span></span>");
        }
        // done
        return sql;
    }
    
    /*
     * helpers
     */

    public static String prepareHtml(String str) 
    {
        str = StringUtils.replace(str, "\t", "    ");
        str = StringUtils.replace(str, "&", "&amp;");
        str = StringUtils.replace(str, "<", "&lt;");
        return StringUtils.replace(str, ">", "&gt;");
    }

    public static boolean isCommentLine(String str, int i) 
    {
        for (;i>0;i--)
        {
            char c = str.charAt(i);
            if (c=='\n')
                break;
            if (c=='/' && str.charAt(i-1)=='/')
                return true;
        }
        return false;
    }

    public static char specialChar(char c) 
    {
        if ((c>='a' && c<='z') || (c>='A' && c<='Z'))
            return 0;
        return c;
    }
    
    public static String replaceWord(String str, String word, boolean special, String htmlBeg, String htmlEnd) 
    {
        char intro = (special ? specialChar(word.charAt(0)) : 0); 
        char extro = (special ? specialChar(word.charAt(word.length()-1)) : 0); 
        // not present
        if (str.indexOf(word)<0)
            return str;
        // replace
        String wtrim = word;
        if (intro>0)
            wtrim = wtrim.substring(1);
        if (extro>0)
            wtrim = wtrim.substring(0, wtrim.length()-1);
        StringBuilder s = new StringBuilder();
        int i = 0;
        int p = 0;
        while ((i=str.indexOf(word, p))>=0)
        {
            if (isCommentLine(str, i))
            {   // comment: ignore
                i += word.length();
                s.append(str.substring(p, i));
                p = i;
                continue;
            }
            s.append(str.substring(p, (intro>0) ? ++i : i));
            s.append(htmlBeg);
            s.append(wtrim);
            s.append(htmlEnd);
            // next
            p = i + wtrim.length();
        }
        s.append(str.substring(p));
        return s.toString();
    }
    
    public static String replaceFunction(String str, char beg, char end, String htmlBeg, String htmlEnd) 
    {
        StringBuilder s = new StringBuilder();
        int i = 0;
        int p = 0;
        while ((i=str.indexOf(beg, p))>=0)
        {
            // function special
            int j = ++i;
            while(true)
            {
                char c = str.charAt(j);
                boolean ok = (c>='a' && c<='z') || (c>='A' && c<='Z');
                if (!ok)
                    break;
                j++;
            }
            // skip whitespace
            int k = j;
            while (str.charAt(k)==' ')
                k++;
            // check end
            if (str.charAt(k)!=end)
            {   // not found, something else
                s.append(str.substring(p, j));
                p = j;
                continue;
            }
            s.append(str.substring(p, i));
            s.append(htmlBeg);
            s.append(str.substring(i, j));
            s.append(htmlEnd);
            // next
            p = j;
        }
        s.append(str.substring(p));
        return s.toString();
    }
    
    public static String replaceFragment(String str, String beg, String end, String htmlBeg, String htmlEnd) 
    {
        StringBuilder s = new StringBuilder();
        int i = 0;
        int p = 0;
        while ((i=str.indexOf(beg, p))>=0)
        {
            int j = str.indexOf(end, i+1);
            if (j<0)
                throw new ItemNotFoundException(end);
            if (!end.equals("\r\n"))
                j+= end.length();
            s.append(str.substring(p, i));
            s.append(htmlBeg);
            s.append(str.substring(i, j));
            s.append(htmlEnd);
            // next
            p = j;
        }
        s.append(str.substring(p));
        return s.toString();
    }

}
