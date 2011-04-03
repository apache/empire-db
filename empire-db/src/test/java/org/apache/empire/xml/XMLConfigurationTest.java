package org.apache.empire.xml;

import static org.junit.Assert.assertNotNull;

import org.apache.empire.EmpireException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class XMLConfigurationTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testInitFail() {
		XMLConfiguration configuration = new XMLConfiguration();
		
		expectedException.expect(EmpireException.class);
		expectedException.expectMessage("The file missing.xml was not found.");
		
		configuration.init("missing.xml", false);
	}

	@Test
	public void testGetRootNode() {
		XMLConfiguration configuration = new XMLConfiguration();
		configuration.init("src/test/resources/testconfig.xml", false);
		assertNotNull(configuration.getRootNode());
	}

	@Test
	public void testReadConfiguration() {
		XMLConfiguration configuration = new XMLConfiguration();
		configuration.readConfiguration("src/test/resources/testconfig.xml", false);
		assertNotNull(configuration.getRootNode());
	}

//	@Test
//	public void testReadPropertiesObjectStringArray() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testReadPropertiesObjectElement() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetPropertyValue() {
//		fail("Not yet implemented");
//	}

}
