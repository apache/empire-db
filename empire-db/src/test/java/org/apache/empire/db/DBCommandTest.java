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

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.empire.data.DataType;
import org.junit.Test;
import org.w3c.dom.Element;

/**
 * @author francisdb
 *
 */
public class DBCommandTest
{

	@Test
	public void testDBCommand()
	{
		MockDB mock = new MockDB();
		mock.open(new MockDriver(), null);
        DBColumn col = new MockDBColumn(null, "test");
        DBColumn col2 = new MockDBColumn(null, "test2");
        DBColumn col3 = new MockDBColumn(null, "test3");
		
		List<DBColumn> list = new ArrayList<DBColumn>();
		DBCommand command = mock.createCommand();
	    command.select(list);
	    assertNull(command.getSelectExprList());
	    
		List<DBColumnExpr> list2 = new ArrayList<DBColumnExpr>();
		list2.add(col);
		list2.add(col2);
		DBCommand command2 = mock.createCommand();
		command2.select(list2);
		assertEquals(2, command2.getSelectExprList().length);
		
        DBCommand command3 = mock.createCommand();
        command3.select(col, col2, col3);
        command3.groupBy(col, col2, col2);
        assertEquals(3, command3.getSelectExprList().length);
        assertEquals(2, command3.groupBy.size());
	}
	
	private class MockDB extends DBDatabase{
		
	}
	
	private class MockDriver extends DBDatabaseDriver{

        @Override
        public DBCommand createCommand(DBDatabase db)
        {
            return new MockCommand(db);
        }

        @Override
        public String getConvertPhrase(DataType destType, DataType srcType, Object format)
        {
            return null;
        }

        @Override
        public Object getNextSequenceValue(DBDatabase db, String SeqName, int minValue, Connection conn)
        {
            return null;
        }

        @Override
        public String getSQLPhrase(int phrase)
        {
            return null;
        }

        @Override
        public boolean isSupported(DBDriverFeature type)
        {
            return false;
        }
	    
	}
	
	private class MockCommand extends DBCommand{

        protected MockCommand(DBDatabase db)
        {
            super(db);
        }
	    
	}
	
	private class MockDBColumn extends DBColumn{

	    public MockDBColumn(DBRowSet rowSet, String name)
        {
            super(rowSet, name);
        }
	    
        @Override
        public Element addXml(Element parent, long flags)
        {
            return null;
        }

        @Override
        public boolean checkValue(Object value)
        {
            return false;
        }

        @Override
        public double getSize()
        {
            return 0;
        }

        @Override
        public boolean isReadOnly()
        {
            return false;
        }

        @Override
        public boolean isRequired()
        {
            return false;
        }

        @Override
        public DataType getDataType()
        {
            return null;
        }
	    
	}

}
