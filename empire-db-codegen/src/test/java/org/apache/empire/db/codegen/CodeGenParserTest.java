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
package org.apache.empire.db.codegen;

import static org.apache.empire.data.DataType.DECIMAL;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBRelation.DBReference;
import org.junit.Before;
import org.junit.Test;


public class CodeGenParserTest {
    private transient CodeGenParser parser;

    @Before
    public void setUp() throws Exception {
        final CodeGenConfig config = new CodeGenConfig();
        config.init("src/test/resources/testconfig.xml");
        config.setDbSchema("PUBLIC");
        config.setDbTablePattern("DEPARTMENTS,EMPLOYEES,ORGANIZATIONS");
        parser = new CodeGenParser(config);
    }

    @Test
    public void testLoadDbModel() {
        final DBDatabase db = parser.loadDbModel();

        final DBTable departments = db.getTable("DEPARTMENTS");
        final DBTable employees = db.getTable("EMPLOYEES");

        assertNotNull("Expected DEPARTMENTS table.", departments);
		assertNotNull("Expected EMPLOYEES table.", employees);

        final List<DBRelation> relations = db.getRelations();
        assertEquals("Should have one relation.", 1, relations.size());

        final DBRelation dbRelation = relations.get(0);
        assertEquals("EMPLOYEES_DEPARTMENT_I_FK", dbRelation.getName());

        final DBReference[] references = dbRelation.getReferences();
        assertEquals("Should reference one column.", 1, references.length);

        final DBReference dbReference = references[0];
		assertEquals(dbReference.getSourceColumn(), employees.getColumn("DEPARTMENT_ID"));
        assertEquals(dbReference.getTargetColumn(), departments.getColumn("DEPARTMENT_ID"));
        
        final DBColumn salary = employees.getColumn("SALARY");

        assertThat(salary.getDataType(), is(DECIMAL));
        assertThat(salary.getSize(), is(10.2));
    }
}
