package org.apache.empire.commons;

import static org.junit.Assert.*;

import org.junit.Test;

public class ErrorTypeTest
{

	@Test
	public void testErrorType()
	{
		// TODO maybe we should find the highest param as some might not be in use (regex)
		ErrorType errorType = new ErrorType("key", " {0} and {1} and {3} is {2}");
		assertEquals(4, errorType.getNumParams());
		assertEquals("key", errorType.getKey());
		assertEquals(" {0} and {1} and {3} is {2}", errorType.getMessagePattern());
	}

}
