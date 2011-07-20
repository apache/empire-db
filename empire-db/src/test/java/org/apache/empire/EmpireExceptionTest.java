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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.apache.empire.commons.EmpireException;
import org.apache.empire.commons.Errors;
import org.junit.Before;
import org.junit.Test;

public class EmpireExceptionTest
{
    private EmpireException exception;

    @Before
    public void setupException()
    {
        this.exception = new EmpireException(Errors.InvalidArg, -1, "param");
    }

    @Test
    public void testToString()
    {
        String expected = "Invalid Argument -1 for parameter param.";
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
        assertEquals(exception.getMessage(),   "Invalid Argument -1 for parameter param.");
        assertEquals(exception.getErrorType(), Errors.InvalidArg);
        assertArrayEquals(exception.getErrorParams(), new Object[]{ -1, "param" });
    }

}
