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
package org.apache.empire.commons;

import static org.junit.Assert.assertEquals;

import org.apache.empire.exceptions.EmpireException;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.junit.Before;
import org.junit.Test;

public class ErrorsTest
{
    private static final String ROOT_EXCEPTION_MSG = "This is the root exception message!"; 

    private Throwable cause;

    @Before
    public void setupException()
    {
        cause = new RuntimeException(ROOT_EXCEPTION_MSG);
    }
    
    @Test
    public void testInvalidArgumentException()
    {
        try {
            // Test InvalidArg
            throw new InvalidArgumentException("testparam", "testvalue");
            
        } catch (EmpireException e) {
            
            assertEquals(e.getMessage(), "Invalid Argument testvalue for parameter testparam.");
        }
    }    
    
    @Test
    public void testInternalException()
    {
        try {
            // Test InvalidArg
            throw new InternalException(cause);
            
        } catch (EmpireException e) {
            
            assertEquals(e.getErrorParams()[0], "RuntimeException");
            assertEquals(e.getErrorParams()[1], ROOT_EXCEPTION_MSG);
        }
    }    

    /*
   
    private static final String NATIVE_ERROR_MSG = "This is the native error message!"; 
    
	@Test
	public void testGetErrorMessage()
	{
        Throwable cause = new RuntimeException(ROOT_EXCEPTION_MSG);
        
	    try {
	        // Test InvalidArg
	        throw new EmpireException(Errors.InvalidArg, "test", "testparam");
	        
	    } catch (EmpireException e) {
	        
            assertEquals(e.getErrorType(), Errors.InvalidArg);
	        assertEquals(e.getMessage(), "Invalid Argument 'test' for parameter 'testparam'.");
	    }

        try {
            // Test Throwable wrapper
            throw new EmpireException(cause);
            
        } catch (EmpireException e) {

            assertEquals(e.getErrorParams()[0], "RuntimeException");
            assertEquals(e.getErrorParams()[1], ROOT_EXCEPTION_MSG);
        }

        try {
            // Test SQLException
            throw new EmpireException(DBErrors.SQLException, new Object[] { NATIVE_ERROR_MSG }, cause);
            
        } catch (EmpireException e) {

            assertEquals(e.getErrorParams()[0], NATIVE_ERROR_MSG);
            
        }
	}
	*/

}
