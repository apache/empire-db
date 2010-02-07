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
package org.apache.empire.db.codegen;

import static org.junit.Assert.assertEquals;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBTableColumn;
import org.junit.Test;
import org.mockito.Mockito;

public class WriterServiceTest {

	@Test
	public void testGetTableClassName() {
		CodeGenConfig config = new CodeGenConfig();
		WriterService service = new WriterService(config);
		assertEquals("TestTable", service.getTableClassName("test_table"));
		assertEquals("TestTable", service.getTableClassName("TEST_TABLE"));
		assertEquals("TestTable", service.getTableClassName("TestTable"));
		// TODO is this correct?
		assertEquals("TESTTABLE", service.getTableClassName("TESTTABLE"));
	}
	
	@Test
	public void testGetAccessorName(){
		CodeGenConfig config = new CodeGenConfig();
		WriterService service = new WriterService(config);
		
		DBTableColumn col = Mockito.mock(DBTableColumn.class);
		Mockito.when(col.getDataType()).thenReturn(DataType.INTEGER);
		Mockito.when(col.getName()).thenReturn("name");
		assertEquals("getName",service.getAccessorName(col));
		
		DBTableColumn col2 = Mockito.mock(DBTableColumn.class);
		Mockito.when(col2.getDataType()).thenReturn(DataType.BOOL);
		Mockito.when(col2.getName()).thenReturn("name");
		assertEquals("isName",service.getAccessorName(col2));
	}
	
	@Test
	public void testGetMutatorName(){
		CodeGenConfig config = new CodeGenConfig();
		WriterService service = new WriterService(config);
		
		DBTableColumn col = Mockito.mock(DBTableColumn.class);
		Mockito.when(col.getDataType()).thenReturn(DataType.DECIMAL);
		Mockito.when(col.getName()).thenReturn("name");
		assertEquals("setName", service.getMutatorName(col));		
	}

}
