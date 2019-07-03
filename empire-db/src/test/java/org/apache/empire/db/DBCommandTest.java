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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.empire.data.DataType;
import org.junit.Test;

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
		
		List<DBColumn> list = new ArrayList<DBColumn>();
		DBCommand command = mock.createCommand();
	    command.select(list);
	    assertNull(command.getSelectExprList());
	    
		List<DBColumnExpr> list2 = new ArrayList<DBColumnExpr>();
		list2.add(mock.TABLE.COL1);
		list2.add(mock.TABLE.COL3);
		DBCommand command2 = mock.createCommand();
		command2.select(list2);
		assertEquals(2, command2.getSelectExprList().length);
		
        DBCommand command3 = mock.createCommand();
        command3.select (mock.TABLE.getColumns());
        command3.groupBy(mock.TABLE.COL1, mock.TABLE.COL2, mock.TABLE.COL2);
        assertEquals(3, command3.getSelectExprList().length);
        assertEquals(2, command3.groupBy.size());
	}
	
	private static class MockDB extends DBDatabase{
        
	    private static final long serialVersionUID = 1L;
	    
	    private final MockDBTable TABLE = new MockDBTable(this);

	}
	
    private static class MockDBTable extends DBTable {
        
        private static final long serialVersionUID = 1L;
        
        private final DBColumn COL1; 
        private final DBColumn COL2; 
        private final DBColumn COL3; 
        
        public MockDBTable(MockDB db) {
            super("MOCK_TABLE", db);
            // add Columns
            COL1 = addColumn("COL1", DataType.INTEGER,   0, true);
            COL2 = addColumn("COL2", DataType.VARCHAR,  20, true);
            COL3 = addColumn("COL3", DataType.DATETIME,  0, false);
        }
    }

}
