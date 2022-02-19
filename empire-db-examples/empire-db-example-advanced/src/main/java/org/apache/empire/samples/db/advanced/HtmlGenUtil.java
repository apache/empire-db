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
            "log.info(\"queryBeanList returnes {} items\", list.size());"; 
    
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

    
    public static String codeToHtml(DBDatabase db, String code, Number... literals)
    {
        code = prepareHtml(code);
        // String Literals must go first
        code = replaceFragment(code, '"', '"', true, "<span class=\"literal\">", "</span>", null);
        // other literals
        for (int i=0; i<literals.length; i++)
            code = replaceWord(code, literals[i].toString(), ' ', "<span class=\"literal\">", "</span>");
        // null
        code = replaceWord(code, "null", ' ', "<span class=\"literal\">", "</span>");
        // types
        String[] types = new String[] { "CarSalesDB", "DBCommand", "QueryResult", "EngineType", "int ", "long ", "String " };
        for (int i=0; i<types.length; i++)
            code = replaceWord(code, types[i], ' ', "<span class=\"type\">", "</span>");
        // Tables and columns
        for (DBTable t : db.getTables())
        {
            code = replaceWord(code, t.getName(), ' ', "<span class=\"obj\">", "</span>");
            for (DBColumn c : t.getColumns())
            {
                code = replaceWord(code, "."+c.getName(), '.', "<span class=\"var\">", "</span>");
            }
        }
        // functions
        code = replaceFragment(code, '.', '(', false, "<span class=\"func\">", "</span>", new char[] { 'a', 'z' });
        // literals
        return replaceComment(code, "<span class=\"comment\">", "</span>"); 
    }
    
    public static String sqlToHtml(DBDatabase db, String sql, Number... literals)
    {
        sql = prepareHtml(sql);
        /*
        UPDATE t2
        SET BASE_PRICE=round(t2.BASE_PRICE*105/100,0)
        FROM MODEL t2 INNER JOIN BRAND t1 ON t1.WMI = t2.WMI
        WHERE t1.NAME='Volkswagen' AND t2.NAME='Tiguan' AND t2.ENGINE_TYPE='D'
        */
        String[] words = new String[] { "SELECT ", "UPDATE ", "INSERT ", "SET" , "FROM ", "WHERE ", "GROUP BY ", "HAVING ", "ORDER BY", " IN ", " ON ", " AND ", " INNER JOIN ", " LEFT JOIN ", " RIGHT_JOIN " };
        for (int i=0; i<words.length; i++)
            sql = replaceWord(sql, words[i], ' ', "<span class=\"word\">", "</span>");
        for (DBTable t : db.getTables())
            sql = replaceWord(sql, t.getAlias(), ' ', "<span class=\"alias\">", "</span>");
        // functions
        String[] func = new String[] { "count", "round", "avg" };
        for (int i=0; i<func.length; i++)
            sql = replaceWord(sql, func[i], ' ', "<span class=\"func\">", "</span>");
        // finally literals
        for (int i=0; i<literals.length; i++)
            sql = replaceWord(sql, literals[i].toString(), ' ', "<span class=\"literal\">", "</span>");
        // String literals
        sql = sql.replace("N'", "'");
        return replaceFragment(sql, '\'', '\'', true, "<span class=\"param\"><span class=\"literal\">", "</span></span>", null);
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
    
    public static String replaceWord(String str, String word, char intro, String htmlBeg, String htmlEnd) 
    {
        // not present
        if (str.indexOf(word)<0)
            return str;
        // replace
        String wtrim = word.trim();
        if (wtrim.charAt(0)==intro)
            wtrim = wtrim.substring(1);
        StringBuilder s = new StringBuilder();
        int i = 0;
        int p = 0;
        while ((i=str.indexOf(word, p))>=0)
        {
            if (word.charAt(0)==intro)
                i++;
            s.append(str.substring(p, i));
            s.append(htmlBeg);
            s.append(wtrim);
            s.append(htmlEnd);
            // next
            p = i + wtrim.length();
        }
        s.append(str.substring(p));
        return s.toString();
    }
    
    public static String replaceFragment(String str, char beg, char end, boolean include, String htmlBeg, String htmlEnd, char[] nextRange) 
    {
        StringBuilder s = new StringBuilder();
        int i = 0;
        int p = 0;
        while ((i=str.indexOf(beg, p))>=0)
        {
            // function special
            if (nextRange!=null)
            {   // check range of next char
                char next = str.charAt(i+1);
                if (next<nextRange[0] || next>nextRange[1])
                {   // ignore
                    s.append(str.substring(p, ++i));
                    p = i;
                    continue;
                }
            }
            int j = str.indexOf(end, ++i);
            if (!include)
            {   // remove whitespace
                while(str.charAt(j-1)==' ') j--;
            }
            int o = (include) ? 1 : 0; 
            s.append(str.substring(p, i-o));
            s.append(htmlBeg);
            s.append(str.substring(i-o, j+o));
            s.append(htmlEnd);
            // next
            p = j + o;
        }
        s.append(str.substring(p));
        return s.toString();
    }
    
    public static String replaceComment(String str, String htmlBeg, String htmlEnd) 
    {
        StringBuilder s = new StringBuilder();
        String beg = "//";
        String end ="\r\n";
        int i = 0;
        int p = 0;
        while ((i=str.indexOf(beg, p))>=0)
        {
            int j = str.indexOf(end, i+1);
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
    
    /*
    public static String replaceSqlWord(String sql, String word, String htmlBeg, String htmlEnd) 
    {
        // not present
        if (sql.indexOf(word)<0)
            return sql;
        // replace
        String wtrim = word.trim();
        StringBuilder s = new StringBuilder();
        int i = 0;
        int p = 0;
        while ((i=sql.indexOf(word, p))>=0)
        {
            if (word.charAt(0)==' ')
                i++;
            s.append(sql.substring(p, i));
            s.append(htmlBeg);
            s.append(wtrim);
            s.append(htmlEnd);
            // next
            p = i + wtrim.length();
        }
        s.append(sql.substring(p));
        return s.toString();
    }
    */
    
    /*
    public static String replaceSqlStringLiteral(String sql, String htmlBeg, String htmlEnd) 
    {
        StringBuilder s = new StringBuilder();
        int i = 0;
        int p = 0;
        while ((i=sql.indexOf('\'', p))>=0)
        {
            int j = sql.indexOf('\'', ++i);
            s.append(sql.substring(p, i-1));
            s.append(htmlBeg);
            s.append(sql.substring(i-1, j+1));
            s.append(htmlEnd);
            // next
            p = j + 1;
        }
        s.append(sql.substring(p));
        return s.toString();
    }
    */

}
