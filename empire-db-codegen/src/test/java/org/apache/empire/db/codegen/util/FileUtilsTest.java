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
