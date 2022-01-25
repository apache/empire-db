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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.dbms.DBMSHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author rainer
 * @since 23.01.22
 */
public class SerializeTest
{
    private static final MockDriver dbms = new MockDriver();

    private static final DBContext staticContext = new DBContextStatic(dbms, null);
    
    private static DBContext getStaticContext()
    {
        return staticContext;
    }

    private static Connection getDBConnection()
    {
        return null; /* No real Connection used */ 
    }

    /**
     * Test Standard DBRecord with Serializable Context
     */
    @Test
    public void testSerializationWithSerializableContext()
        throws Exception
    {
        final TestDatabase db = new TestDatabase();
        
        DBContext context = new SerializableContext(dbms, null);
        db.open(context);
        
        DBRecord rec1Original = new DBRecord(context, db.T_TEST);
        rec1Original.create(DBRecord.key(1));
        DBRecord rec1Serial = ClassUtils.testSerialization(DBRecord.class, rec1Original);
        
        Assert.assertTrue(rec1Serial.isValid());
        Assert.assertNotNull(rec1Serial.getContext());
        Assert.assertNotNull(rec1Serial.getContext().getDbms());
        Assert.assertNotSame(rec1Original.getContext(), rec1Serial.getContext());
    }
    
    /**
     * Test Serializable Record with Static Context
     */
    @Test
    public void testSerializationWithSerializableRecord()
        throws Exception
    {
        final TestDatabase db = new TestDatabase();
        
        DBContext context = getStaticContext();
        db.open(context);
        
        DBRecord rec2Original = new SerializableRecord(context, db.T_TEST);
        rec2Original.create(DBRecord.key(1));
        DBRecord rec2Serial = ClassUtils.testSerialization(DBRecord.class, rec2Original);

        Assert.assertTrue(rec2Serial.isValid());
        Assert.assertNotNull(rec2Serial.getContext());
        Assert.assertNotNull(rec2Serial.getContext().getDbms());
        Assert.assertEquals(rec2Original.getContext(), rec2Serial.getContext());
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

            TestTable(DBDatabase db)
            {
                super("testtable", db);
                this.C_ID = addColumn("id", DataType.INTEGER, 0, true);
                this.C_TEXT = addColumn("text", DataType.VARCHAR, 255, false);
                setPrimaryKey(C_ID);
            }
        }
    }
    
    /**
     * SerializableContext
     */
    static class SerializableContext extends DBContextBase implements Serializable
    {
        private static final long serialVersionUID = 1L;
        
        private final transient DBMSHandler dbms;
        private final transient Connection conn;
        
        /**
         * Custom deserialization for transient fields.
         */
        private void readObject(ObjectInputStream strm) throws IOException, ClassNotFoundException 
        {   // Driver
            ClassUtils.setPrivateFieldValue(SerializableContext.class, this, "dbms", SerializeTest.dbms);
            // Context
            ClassUtils.setPrivateFieldValue(SerializableContext.class, this, "conn", getDBConnection());
            // read the rest
            strm.defaultReadObject();
        }
        
        
        public SerializableContext(DBMSHandler dbms, Connection conn)
        {
            this.dbms = dbms;
            this.conn = conn;
        }

        @Override
        public DBMSHandler getDbms()
        {
            return dbms;
        }
    
        @Override
        public boolean isRollbackHandlingEnabled()
        {
            return false;
        }
    
        @Override
        protected Connection getConnection(boolean create)
        {
            return conn;
        }
    
        @Override
        protected DBRollbackManager getRollbackManager(boolean create)
        {
            return null;
        }
    }
    
    /**
     * SerializableRecord
     */
    static class SerializableRecord extends DBRecord
    {
        private static final long serialVersionUID = 1L;
        
        public SerializableRecord(DBContext context, DBRowSet rowset)
        {
            super(context, rowset);
        }
    
        @Override
        protected void writeContext(ObjectOutputStream strm) throws IOException
        {
            /* Nothing */
        }
    
        @Override
        protected DBContext readContext(ObjectInputStream strm)  throws IOException, ClassNotFoundException
        {
            return getStaticContext();
        }
    }
}
