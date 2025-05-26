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
package org.apache.empire.jsf2.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.apache.empire.exceptions.NotSupportedException;
import org.junit.Test;

public class StyleClassTest
{
    /**
     * Test method for {@link org.apache.empire.commons.Options#size()}.
     */
    @Test
    public void testBuild()
    {
        StyleClass styleClass = new StyleClass();
        assertTrue(styleClass.isEmpty());
        
        styleClass.add("hello big world");
        assertTrue(styleClass.isNotEmpty());

        styleClass.add("big");
        assertEquals("hello big world", styleClass.build());
        
    }

    @Test
    public void testIgnore()
    {
        StyleClass styleClass = new StyleClass("hello big world");
        
        styleClass.add("-big");
        assertEquals("hello world", styleClass.build());
        
        styleClass.add("-hello -world");
        assertTrue(styleClass.isEmpty());

        styleClass.add("big");
        styleClass.add("success");
        assertEquals("success", styleClass.build());
        
        styleClass.remove("-big");
        styleClass.add("big");
        assertEquals("success big", styleClass.build());
    }

    @Test
    public void testImmutable()
    {
        StyleClass styleClass = new StyleClass("immutable style", true);
        assertThrows(NotSupportedException.class, () -> styleClass.add("JUnit"));
        
    }
    
}
