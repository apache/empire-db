package org.apache.empire.db.codegen;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.empire.db.codegen.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class CodeGenAppTest {
	
	@Before
	public void cleanup() throws IOException{
		File generated = new File("target/generated");
		if(generated.exists()){
			boolean deleted = FileUtils.deleteDirectory(generated);
			if(!deleted){
				throw new IOException("Could not delete previously generated sources");
			}
		}
	}

	@Test
	public void testMain() {
		String[] args = new String[]{"src/test/resources/testconfig.xml"};
		
		// execute app
		CodeGenApp.main(args);
		
		// expected files
		File expected = new File("target/generated/dbsample/org/apache/empire/db/samples/dbsample/SampleDB.java");
		assertTrue("missing generated code", expected.exists());
	}
	
	@Test
	public void testMainUsingTemplateFolder() {
		String[] args = new String[]{"src/test/resources/testconfig_using_template_folder.xml"};
		
		// execute app
		CodeGenApp.main(args);
		
		// expected files
		File expected = new File("target/generated/dbsample/org/apache/empire/db/samples/dbsample/SampleDB.java");
		assertTrue("missing generated code", expected.exists());
	}
	
	@Test
	public void testMainFailInvalidTemplateFolder() {
		String[] args = new String[]{"src/test/resources/testconfig_invalid_template_folder.xml"};
		try{
			CodeGenApp.main(args);
			fail("This should fail as the template path is missing");
		}catch(RuntimeException ex){
			assertTrue("Wrong message", ex.getMessage().startsWith("Provided template folder missing or not readable:"));
		}
	}

}
