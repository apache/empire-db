package org.apache.empire.db.codegen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

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
        config.setDbSchema(null);
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
        assertEquals("Should have EMPLOYEES->DEPARTMENTS relation.", 1, relations.size());

        final DBRelation dbRelation = relations.get(0);
        assertEquals("EMPLOYEES_DEPARTMENT_I_FK", dbRelation.getName());

        final DBReference[] references = dbRelation.getReferences();
        assertEquals("Should reference one column.", 1, references.length);

        final DBReference dbReference = references[0];
		assertEquals(dbReference.getSourceColumn(), employees.getColumn("DEPARTMENT_ID"));
        assertEquals(dbReference.getTargetColumn(), departments.getColumn("DEPARTMENT_ID"));
    }
}
