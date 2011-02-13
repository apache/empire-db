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
package org.apache.empire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.ErrorObject;
import org.apache.empire.commons.ErrorType;
import org.apache.empire.commons.Errors;
import org.junit.Before;
import org.junit.Test;

public class EmpireExceptionTest
{

	private final class MockErrorInfo implements ErrorInfo
	{

		public Object[] getErrorParams()
		{
			return new Object[] { "JUnit", "Test" };
		}

		public String getErrorSource()
		{
			return "JUnitSource";
		}

		public ErrorType getErrorType()
		{
			return Errors.IllegalFormat;
		}

		public boolean hasError()
		{
			return true;
		}
	}

	private final class MockErrorObject extends ErrorObject
	{
		public MockErrorObject()
		{
			super(new MockErrorInfo());
		}
	}

	private EmpireException exception;
	private ErrorObject errorObject;

	@Before
	public void setupException()
	{
	    ErrorObject.setExceptionsEnabled(false);
		this.errorObject = new MockErrorObject();
		this.exception = new EmpireException(errorObject);
	}

	@Test
	public void testToString()
	{
		String expected = errorObject.getClass().getName() + ": The format of JUnit is invalid for Test";
		assertEquals(expected, exception.toString());
	}

	@Test
	public void testGetErrorType()
	{
		assertEquals(Errors.IllegalFormat, exception.getErrorType());
	}

	@Test
	public void testGetErrorObject()
	{
		assertEquals(errorObject, exception.getErrorObject());
		assertSame(errorObject, exception.getErrorObject());
	}

}
