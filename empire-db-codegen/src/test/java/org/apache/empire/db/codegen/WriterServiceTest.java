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
