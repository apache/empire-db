package org.apache.empire.db.codegen.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileUtilsTest {
	
	private File testParent;

	@Before
	public void createTestFolder(){
		testParent = new File("target/testparent");
		cleanup();
		boolean success = testParent.mkdirs();
		assertTrue("could not create test file",success);
	}
	
	@After
	public void cleanup(){
		if(testParent.exists()){
			boolean success = testParent.delete();
			assertTrue(success);
		}
	}
	
	@Test
	public void testCleanDirectory() {
		File testChildren = new File(testParent, "this/is/a/test");
		boolean success = testChildren.mkdirs();
		assertTrue("Could not create test folder", success);
		FileUtils.cleanDirectory(testParent);
		assertTrue("Parent was deleted", testParent.exists());
		assertEquals(0, testParent.list().length);
		
		// TODO test with files
		// TODO test fail if file in use
	}

}
