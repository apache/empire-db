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

import org.apache.empire.data.DataType;
import org.apache.empire.data.list.DataListEntry;
import org.apache.empire.data.list.DataListHead;
import org.apache.empire.db.context.DBContextStatic;
import org.junit.Test;

import junit.framework.Assert;

/**
 * @author rainer
 * @since 23.01.22
 */
public class CoalesceExpressionTest
{
    /**
     * Test Standard DBRecord with Serializable Context
     */
    @Test
    public void testCoalesceExpression()
        throws Exception
    {
        final TestDatabase db = new TestDatabase();
        
        DBContext context = new DBContextStatic(new MockDriver(), null);
        db.open(context);
        
        TestDatabase.TestTable t = db.T_TEST;
        DBColumnExpr COALESCE_1 = t.C_TEXT.coalesce("COALESCE_1");
        DBColumnExpr COALESCE_2 = t.C_TEXT.coalesce("COALESCE_2");
        DBColumnExpr AMOUNT = t.C_NUMBER.coalesce(5);
        
        String propName = t.C_TEXT.getBeanPropertyName();
        Assert.assertEquals(COALESCE_1.getBeanPropertyName(), propName);
        propName = t.C_NUMBER.getBeanPropertyName();
        Assert.assertEquals(AMOUNT.getBeanPropertyName(), propName);

        // Table Record
        DBRecord record = new DBRecord(context, t);
        int textIndex = record.getFieldIndex(t.C_TEXT);
        Assert.assertEquals(record.getFieldIndex(COALESCE_1), textIndex);
        Assert.assertEquals(record.getFieldIndex(COALESCE_2), textIndex);
        int numberIndex = record.getFieldIndex(t.C_NUMBER);
        Assert.assertEquals(record.getFieldIndex(AMOUNT), numberIndex);
        
        DBCommand cmd = context.createCommand();
        cmd.select(COALESCE_1, t.C_TEXT, COALESCE_2, AMOUNT); /* Don't select COALESCE_2 */
        DBColumnExpr[] expr = cmd.getSelectExprList();
        // Hint: COALESCE_2_NEU is not a separate column
        Assert.assertEquals(expr.length, 3);

        // Query Record
        DBQuery q = new DBQuery(cmd);
        record = new DBRecord(context, q);
        Assert.assertEquals(record.getFieldIndex(t.C_TEXT), 1);
        Assert.assertEquals(record.getFieldIndex(COALESCE_1), 0);
        Assert.assertEquals(record.getFieldIndex(COALESCE_2), 0);
        Assert.assertEquals(record.getFieldIndex(AMOUNT), 2);
        Assert.assertEquals(record.getFieldIndex(t.C_NUMBER), 2);

        // Reader 
        DBReader reader = new MyReader(context);
        reader.open(cmd);
        Assert.assertEquals(reader.getFieldIndex(t.C_TEXT), 1);
        Assert.assertEquals(reader.getFieldIndex(COALESCE_1), 0);
        Assert.assertEquals(reader.getFieldIndex(COALESCE_2), 0);
        Assert.assertEquals(reader.getFieldIndex(AMOUNT), 2);
        Assert.assertEquals(reader.getFieldIndex(t.C_NUMBER), 2);
        reader.close();
        
        // DataListEntry
        DataListHead head = new DataListHead(cmd.getSelectExprList());
        Object[] values = new Object[head.getColumns().length];
        for (int i=0; i<values.length; i++)
            values[i] = head.getColumns()[i].getName();
        DataListEntry dle = new DataListEntry(head, 0, values);
        Assert.assertEquals(dle.getString(t.C_TEXT), values[1]);
        Assert.assertEquals(dle.getString(COALESCE_1), values[0]);
        Assert.assertEquals(dle.getString(COALESCE_2), values[0]);
        Assert.assertEquals(dle.getString(AMOUNT), values[2]);
        Assert.assertEquals(dle.getString(t.C_NUMBER), values[2]);

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
