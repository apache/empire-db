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
public class FunctionExpressionTest
{
    /**
     * Test Column Function Expressions
     */
    @Test
    public void testFunctionExpression()
        throws Exception
    {
        final TestDatabase db = new TestDatabase();
        
        DBContext context = new DBContextStatic(new MockDriver(), null);
        db.open(context);
        
        TestDatabase.TestTable t = db.T_TEST;
        TestDatabase.TestTable t2 = t.clone("tclone");
        
        DBColumn NUMBER = t.C_NUMBER;
        DBColumnExpr TEST_COUNT = t.count();
        DBColumnExpr TEST_2_COUNT = t2.count();
        DBColumnExpr NUM_COUNT_1 = NUMBER.countDistinct();
        DBColumnExpr NUM_COUNT_2 = NUMBER.countDistinct();
        DBColumnExpr NUM_COUNT_3 = t2.C_NUMBER.countDistinct();
        DBColumnExpr NUM_AVG_1 = NUMBER.avg();
        DBColumnExpr NUM_AVG_2 = NUMBER.avg();
        DBColumnExpr NUM_AVG_3 = t2.C_NUMBER.avg();
        DBColumnExpr NUM_SUM_1 = NUMBER.sum();
        DBColumnExpr NUM_SUM_2 = NUMBER.sum();
        DBColumnExpr NUM_SUM_3 = t2.C_NUMBER.sum();
        
        String test_count = TEST_COUNT.getName();
        String name_count = NUM_COUNT_1.getName();
        String name_avg   = NUM_AVG_1.getName();
        String name_sum   = NUM_SUM_1.getName();
        Assert.assertEquals(test_count, "TESTTABLE_COUNT");
        Assert.assertEquals(name_count, "NUMBER_COUNT");
        Assert.assertEquals(name_avg, "NUMBER_AVG");
        Assert.assertEquals(name_sum, "NUMBER_SUM");

        DBCommand cmd = context.createCommand();
        cmd.select(NUMBER, TEST_COUNT, TEST_2_COUNT);
        cmd.select(NUM_COUNT_1, NUM_COUNT_2, NUM_COUNT_3);
        cmd.select(NUM_AVG_1, NUM_AVG_2, NUM_AVG_3);
        cmd.select(NUM_SUM_1, NUM_SUM_2, NUM_SUM_3);
        // System.out.println(cmd.getSelect());
        DBColumnExpr[] expr = cmd.getSelectExprList();
        Assert.assertEquals(expr.length, 9);
        // where
        cmd.where(NUM_AVG_1.isNot(0).or(NUM_SUM_1.isNot(0)).parenthesis());
        cmd.where(NUM_AVG_1.isNot(1).or(NUM_SUM_1.isNot(1)));
        cmd.where(NUM_COUNT_1.isMoreOrEqual(5));
        cmd.where(NUM_COUNT_2.isMoreOrEqual(3));
        cmd.where(NUM_COUNT_3.isMoreOrEqual(4));
        cmd.where(NUM_AVG_1.isNot(0));
        cmd.where(NUM_SUM_1.isLessOrEqual(25));
        cmd.where(NUM_SUM_2.isNotBetween(0, 5));
        cmd.where(NUM_SUM_3.isSmallerThan(55));
        // System.out.println(cmd.getSelect());
        List<DBCompareExpr> list = cmd.getWhereConstraints();
        Assert.assertEquals(list.size(), 6);

        // Query Record
        DBQuery q = new DBQuery(cmd);
        DBRecord record = new DBRecord(context, q);
        Assert.assertEquals(record.getFieldIndex(NUMBER), 0);
        Assert.assertEquals(record.getFieldIndex(TEST_COUNT), 1);
        Assert.assertEquals(record.getFieldIndex(TEST_2_COUNT), 2);
        Assert.assertEquals(record.getFieldIndex(NUM_COUNT_1), 3);
        Assert.assertEquals(record.getFieldIndex(NUM_COUNT_2), 3);
        Assert.assertEquals(record.getFieldIndex(NUM_COUNT_3), 4);
        Assert.assertEquals(record.getFieldIndex(NUM_AVG_1), 5);
        Assert.assertEquals(record.getFieldIndex(NUM_AVG_2), 5);
        Assert.assertEquals(record.getFieldIndex(NUM_AVG_3), 6);
        Assert.assertEquals(record.getFieldIndex(NUM_SUM_1), 7);
        Assert.assertEquals(record.getFieldIndex(NUM_SUM_2), 7);
        Assert.assertEquals(record.getFieldIndex(NUM_SUM_3), 8);

        // Reader 
        DBReader reader = new MyReader(context);
        reader.open(cmd);
        Assert.assertEquals(reader.getFieldIndex(NUMBER), 0);
        Assert.assertEquals(reader.getFieldIndex(TEST_COUNT), 1);
        Assert.assertEquals(reader.getFieldIndex(TEST_2_COUNT), 2);
        Assert.assertEquals(reader.getFieldIndex(NUM_COUNT_1), 3);
        Assert.assertEquals(reader.getFieldIndex(NUM_COUNT_2), 3);
        Assert.assertEquals(reader.getFieldIndex(NUM_COUNT_3), 4);
        Assert.assertEquals(reader.getFieldIndex(NUM_AVG_1), 5);
        Assert.assertEquals(reader.getFieldIndex(NUM_AVG_2), 5);
        Assert.assertEquals(reader.getFieldIndex(NUM_AVG_3), 6);
        Assert.assertEquals(reader.getFieldIndex(NUM_SUM_1), 7);
        Assert.assertEquals(reader.getFieldIndex(NUM_SUM_2), 7);
        Assert.assertEquals(reader.getFieldIndex(NUM_SUM_3), 8);
        reader.close();
        
        // DataListEntry
        DataListHead head = new DataListHead(cmd.getSelectExprList());
        Object[] values = new Object[head.getColumns().length];
        for (int i=0; i<values.length; i++)
            values[i] = head.getColumns()[i].getName();
        DataListEntry dle = new DataListEntry(head, values);
        Assert.assertEquals(dle.getFieldIndex(NUMBER), 0);
        Assert.assertEquals(dle.getFieldIndex(TEST_COUNT), 1);
        Assert.assertEquals(dle.getFieldIndex(TEST_2_COUNT), 2);
        Assert.assertEquals(dle.getFieldIndex(NUM_COUNT_1), 3);
        Assert.assertEquals(dle.getFieldIndex(NUM_COUNT_2), 3);
        Assert.assertEquals(dle.getFieldIndex(NUM_COUNT_3), 4);
        Assert.assertEquals(dle.getFieldIndex(NUM_AVG_1), 5);
        Assert.assertEquals(dle.getFieldIndex(NUM_AVG_2), 5);
        Assert.assertEquals(dle.getFieldIndex(NUM_AVG_3), 6);
        Assert.assertEquals(dle.getFieldIndex(NUM_SUM_1), 7);
        Assert.assertEquals(dle.getFieldIndex(NUM_SUM_2), 7);
        Assert.assertEquals(dle.getFieldIndex(NUM_SUM_3), 8);

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
                super("TESTTABLE", db);
                this.C_ID = addColumn("id", DataType.INTEGER, 0, true);
                this.C_TEXT = addColumn("TEXT", DataType.VARCHAR, 255, false);
                this.C_NUMBER = addColumn("NUMBER", DataType.DECIMAL, 10.2, false);
                setPrimaryKey(C_ID);
            }
        }
    }
}
