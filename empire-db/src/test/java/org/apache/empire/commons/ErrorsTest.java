package org.apache.empire.commons;

import static org.junit.Assert.*;

import org.junit.Test;

public class ErrorsTest
{

	
	@Test
	public void testGetErrorMessage()
	{
		
		ErrorInfo info = new DummyErrorInfo(true, Errors.InvalidArg, new Object[]{"test", "testparam"});
		assertEquals("Invalid Argument test for parameter testparam.", Errors.getErrorMessage(info));
		
		// TODO should we fail in this case?
		// TODO find a way to force the correct number of params at compiletime
		info = new DummyErrorInfo(true, Errors.InvalidArg, new Object[]{"test"});
		assertEquals("Invalid Argument test for parameter {1}.", Errors.getErrorMessage(info));
	
		assertEquals("", Errors.getErrorMessage(null));		
		
		info = new DummyErrorInfo(false, Errors.Internal, null);
		assertEquals("", Errors.getErrorMessage(info));
		
		info = new DummyErrorInfo(true, null, null);
		assertEquals("", Errors.getErrorMessage(info));
		
		info = new DummyErrorInfo(true, Errors.None, null);
		assertEquals("", Errors.getErrorMessage(info));
	}
	
	private final class DummyErrorInfo implements ErrorInfo
	{
		private final boolean hasErrror;
		private final ErrorType type;
		private final Object[] params;
		
		public DummyErrorInfo(final boolean hasError, final ErrorType type, final Object[] params)
		{
			this.hasErrror = hasError;
			this.type = type;
			this.params = params;
		}
		
		public Object[] getErrorParams()
		{
			return params;
		}

		public String getErrorSource()
		{
			return "JUnit";
		}

		public ErrorType getErrorType()
		{
			return type;
		}

		public boolean hasError()
		{
			return hasErrror;
		}
	}

	

}
