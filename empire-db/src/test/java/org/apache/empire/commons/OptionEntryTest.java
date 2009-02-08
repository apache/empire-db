package org.apache.empire.commons;

import static org.junit.Assert.*;

import org.junit.Test;

public class OptionEntryTest
{

	@Test
	public void testOptionEntry()
	{
		OptionEntry entry = new OptionEntry(Boolean.TRUE, "junit");
		assertEquals(Boolean.TRUE, entry.getValue());
		assertEquals("junit", entry.getText());
	}
	
	@Test
	public void testGetValueString()
	{
		// TODO add javadoc to this method
		OptionEntry entry = new OptionEntry(Boolean.TRUE, "junit");
		assertEquals("true", entry.getValueString());
		entry = new OptionEntry(null, "junit");
		assertEquals("", entry.getValueString());
	}

	@Test
	public void testGetSetText()
	{
		OptionEntry entry = new OptionEntry(Boolean.TRUE, "junit");
		entry.setText(null);
		assertEquals(null, entry.getText());
		entry.setText("updated");
		assertEquals("updated", entry.getText());
	}

}
