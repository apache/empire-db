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
