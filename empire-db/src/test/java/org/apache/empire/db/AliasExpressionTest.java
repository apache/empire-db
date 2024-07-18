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

package org.apache.empire.db;

import java.util.List;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.data.list.DataListHead;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.expr.compare.DBCompareExpr;
import org.junit.Test;

import junit.framework.Assert;

/**
 * @author rainer
 * @since 23.01.22
 */
public class AliasExpressionTest
{
    /**
     * Test Alias Expression
     */
    @Test
    public void testAliasExpression()
        throws Exception
    {
        final TestDatabase db = new TestDatabase();
        
        DBContext context = new DBContextStatic(new MockDriver(), null);
        db.open(context);
        
        TestDatabase.TestTable t = db.T_TEST;
        DBColumnExpr ALIAS_1 = t.C_TEXT.as("ALIAS_1");
        DBColumnExpr ALIAS_2 = t.C_TEXT.as("ALIAS_2");
        DBColumnExpr ALIAS_2_NEW = t.C_TEXT.as("ALIAS_2");
        DBColumnExpr ALIAS_2_NUM = t.C_NUMBER.as("ALIAS_2");
        DBColumnExpr ALIAS_X = t.C_TEXT.as("ALIAS_X");
        DBColumnExpr AMOUNT = t.C_NUMBER.as("AMOUNT");

        // Table Record
        DBRecord record = new DBRecord(context, t);
        int textIndex = record.getFieldIndex(t.C_TEXT);
        Assert.assertEquals(record.getFieldIndex(ALIAS_1), textIndex);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2), textIndex);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2_NEW), textIndex);
        Assert.assertEquals(record.getFieldIndex(ALIAS_X), textIndex);
        int numberIndex = record.getFieldIndex(t.C_NUMBER);
        Assert.assertEquals(record.getFieldIndex(AMOUNT), numberIndex);
        
        /*
         * (updated 2024-07-18 EMPIREDB-434)
         */
        DBCommand cmd = context.createCommand();
         /* Don't select ALIAS_X */
        cmd.select(t.C_ID.qualified(), t.C_ID.as("ID"), t.C_ID, ALIAS_1);  // three with single "ID"
        cmd.select(t.C_TEXT, t.C_NUMBER.as(t.C_TEXT.name), t.C_TEXT.as(t.C_TEXT.name), t.C_TEXT); // only the last one
        cmd.select(ALIAS_2_NUM, ALIAS_2, ALIAS_2_NEW);    // only the last one
        cmd.select(AMOUNT, t.C_NUMBER); 
        cmd.select(db.getValueExpr(DateUtils.getDateNow(), DataType.DATE).as(t.C_DATE)); // date as constant
        DBColumnExpr[] expr = cmd.getSelectExprList();
        // Hint: ALIAS_2_NEU is not a separate column
        Assert.assertEquals(expr.length, 8);
        String exprNames = StringUtils.arrayToString(expr, StringUtils.LIST_TEMPLATE);
        Assert.assertEquals(exprNames, "[testtable_id|testtable.id|ALIAS_1|testtable.TEXT|ALIAS_2|AMOUNT|testtable.NUMBER|DATE]");
        // where
        cmd.where(ALIAS_1.isNot("Foo1"));
        cmd.where(ALIAS_2.isNot("Foo2"));
        cmd.where(ALIAS_2_NEW.isNot("Foo3"));
        cmd.where(ALIAS_X.isNot("Foo4"));
        // System.out.println(cmd.getSelect());
        List<DBCompareExpr> list = cmd.getWhereConstraints();
        Assert.assertEquals(list.size(), 1);

        // Query Record
        DBQuery q = new DBQuery(cmd);
        record = new DBRecord(context, q);
        Assert.assertEquals(record.getFieldIndex("testtable_id"), 0);
        Assert.assertEquals(record.getFieldIndex(t.C_ID), 1);
        Assert.assertEquals(record.getFieldIndex(ALIAS_1), 2);
        Assert.assertEquals(record.getFieldIndex(t.C_TEXT), 3);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2), 4);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2_NEW), 4);
        Assert.assertEquals(record.getFieldIndex(ALIAS_X), 3);
        Assert.assertEquals(record.getFieldIndex(AMOUNT), 5);
        Assert.assertEquals(record.getFieldIndex(t.C_NUMBER), 6);
        Assert.assertEquals(record.getFieldIndex(t.C_DATE), 7);

        // Reader 
        DBReader reader = new MyReader(context);
        reader.open(cmd);
        Assert.assertEquals(reader.getFieldIndex("testtable_id"), 0);
        Assert.assertEquals(reader.getFieldIndex(t.C_ID), 1);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_1), 2);
        Assert.assertEquals(reader.getFieldIndex(t.C_TEXT), 3);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_2), 4);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_2_NEW), 4);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_X), 3);
        Assert.assertEquals(reader.getFieldIndex(AMOUNT), 5);
        Assert.assertEquals(reader.getFieldIndex(t.C_NUMBER), 6);
        Assert.assertEquals(reader.getFieldIndex(t.C_DATE), 7);
        reader.close();
        
        // DataListEntry
        DataListHead head = new DataListHead(cmd.getSelectExprList());
        Object[] values = new Object[head.getColumns().length];
        for (int i=0; i<values.length; i++)
            values[i] = head.getColumns()[i].getName();
        DataListEntry dle = new DataListEntry(head, values);
        Assert.assertEquals(dle.getFieldIndex("testtable_id"), 0);
        Assert.assertEquals(dle.getFieldIndex(t.C_ID), 1);
        Assert.assertEquals(dle.getFieldIndex(ALIAS_1), 2);
        Assert.assertEquals(dle.getFieldIndex(t.C_TEXT), 3);
        Assert.assertEquals(dle.getFieldIndex(ALIAS_2), 4);
        Assert.assertEquals(dle.getFieldIndex(ALIAS_2_NEW), 4);
        Assert.assertEquals(dle.getFieldIndex(ALIAS_X), 3);
        Assert.assertEquals(dle.getFieldIndex(AMOUNT), 5);
        Assert.assertEquals(dle.getFieldIndex(t.C_NUMBER), 6);
        Assert.assertEquals(dle.getFieldIndex(t.C_DATE), 7);

        // done
    }
    
    static class MyReader extends DBReader
    {
        public MyReader(DBContext context)
        {
            super(context, false);
        }
        
        @Override
        public void open(DBCommandExpr cmd, boolean scrollable)
        {
            super.init(cmd.getDatabase(), cmd.getSelectExprList(), null);
        }
    }
    
    /**
     * Databae
     */
    static class TestDatabase extends DBDatabase
    {
        public final TestTable T_TEST = new TestTable(this);
        static class TestTable extends DBTable
        {
            public final DBTableColumn C_ID;
            public final DBTableColumn C_TEXT;
            public final DBTableColumn C_NUMBER;
            public final DBTableColumn C_DATE;

            TestTable(DBDatabase db)
            {
                super("testtable", db);
                this.C_ID = addColumn("id", DataType.INTEGER, 0, true);
                this.C_TEXT = addColumn("TEXT", DataType.VARCHAR, 255, false);
                this.C_NUMBER = addColumn("NUMBER", DataType.DECIMAL, 10.2, false);
                this.C_DATE = addColumn("DATE", DataType.DATE, 0, false);
                setPrimaryKey(C_ID);
            }
        }
    }
}
