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
        DBColumnExpr ALIAS_2_NEU = t.C_TEXT.as("ALIAS_2");
        DBColumnExpr ALIAS_X = t.C_TEXT.as("ALIAS_X");
        DBColumnExpr AMOUNT = t.C_NUMBER.as("AMOUNT");

        // Table Record
        DBRecord record = new DBRecord(context, t);
        int textIndex = record.getFieldIndex(t.C_TEXT);
        Assert.assertEquals(record.getFieldIndex(ALIAS_1), textIndex);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2), textIndex);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2_NEU), textIndex);
        Assert.assertEquals(record.getFieldIndex(ALIAS_X), textIndex);
        int numberIndex = record.getFieldIndex(t.C_NUMBER);
        Assert.assertEquals(record.getFieldIndex(AMOUNT), numberIndex);
        
        DBCommand cmd = context.createCommand();
        cmd.select(ALIAS_1, t.C_TEXT, ALIAS_2, ALIAS_2_NEU, AMOUNT); /* Don't select ALIAS_X */
        DBColumnExpr[] expr = cmd.getSelectExprList();
        // Hint: ALIAS_2_NEU is not a separate column
        Assert.assertEquals(expr.length, 4);
        // where
        cmd.where(ALIAS_1.isNot("Foo1"));
        cmd.where(ALIAS_2.isNot("Foo2"));
        cmd.where(ALIAS_2_NEU.isNot("Foo3"));
        cmd.where(ALIAS_X.isNot("Foo4"));
        // System.out.println(cmd.getSelect());
        List<DBCompareExpr> list = cmd.getWhereConstraints();
        Assert.assertEquals(list.size(), 1);

        // Query Record
        DBQuery q = new DBQuery(cmd);
        record = new DBRecord(context, q);
        Assert.assertEquals(record.getFieldIndex(t.C_TEXT), 1);
        Assert.assertEquals(record.getFieldIndex(ALIAS_1), 0);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2), 2);
        Assert.assertEquals(record.getFieldIndex(ALIAS_2_NEU), 2);
        Assert.assertEquals(record.getFieldIndex(ALIAS_X), 1);
        Assert.assertEquals(record.getFieldIndex(AMOUNT), 3);
        Assert.assertEquals(record.getFieldIndex(t.C_NUMBER), 3);

        // Reader 
        DBReader reader = new MyReader(context);
        reader.open(cmd);
        Assert.assertEquals(reader.getFieldIndex(t.C_TEXT), 1);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_1), 0);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_2), 2);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_2_NEU), 2);
        Assert.assertEquals(reader.getFieldIndex(ALIAS_X), 1);
        Assert.assertEquals(reader.getFieldIndex(AMOUNT), 3);
        Assert.assertEquals(reader.getFieldIndex(t.C_NUMBER), 3);
        reader.close();
        
        // DataListEntry
        DataListHead head = new DataListHead(cmd.getSelectExprList());
        Object[] values = new Object[head.getColumns().length];
        for (int i=0; i<values.length; i++)
            values[i] = head.getColumns()[i].getName();
        DataListEntry dle = new DataListEntry(head, values);
        Assert.assertEquals(dle.getString(t.C_TEXT), values[1]);
        Assert.assertEquals(dle.getString(ALIAS_1), values[0]);
        Assert.assertEquals(dle.getString(ALIAS_2), values[2]);
        Assert.assertEquals(dle.getString(ALIAS_2_NEU), values[2]);
        Assert.assertEquals(dle.getString(ALIAS_X), values[1]);
        Assert.assertEquals(dle.getString(AMOUNT), values[3]);
        Assert.assertEquals(dle.getString(t.C_NUMBER), values[3]);

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

            TestTable(DBDatabase db)
            {
                super("testtable", db);
                this.C_ID = addColumn("id", DataType.INTEGER, 0, true);
                this.C_TEXT = addColumn("TEXT", DataType.VARCHAR, 255, false);
                this.C_NUMBER = addColumn("NUMBER", DataType.DECIMAL, 10.2, false);
                setPrimaryKey(C_ID);
            }
        }
    }
}
