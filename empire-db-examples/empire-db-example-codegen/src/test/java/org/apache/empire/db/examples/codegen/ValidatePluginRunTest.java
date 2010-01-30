package org.apache.empire.db.examples.codegen;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;


public class ValidatePluginRunTest {
	
	@Test
	public void testTargetFolder(){
		File file = new File("target/generated-sources/empiredb");
		assertTrue("No sources generated", file.exists());
		// TODO add extra validation for the real generated sources
	}

}
