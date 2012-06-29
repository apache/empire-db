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
package org.apache.empire.db;

import java.io.Serializable;

/**
 * This class creates a DBReferene object for a foreing key relation.
 * 
 *
 */
public class DBRelation extends DBObject
{
    private final static long serialVersionUID = 1L;
    
    /**
     * DBCascadeAction enum
     * <pre>
     * This enum specifies options for a relation when deleting records
     * (see DBRelation.setOnDeleteAction)
     *  
     * - NONE:    No Action is performed and the operation will fail if depending records exist.
     * 
     * - CASCADE: Delete (or update) any depending records. This action will be performed by the database and thus 
     *            the option "ON DELETE CASCADE" is added to the DDL generated for the relation. 
     * 
     * - CASCADE_RECORDS: This option allows to handle the cascade in code. All depending records will be selected
     *            by the framework and for each record DBRowSet.deleteRecord() will be called. 
     *            The option "ON DELETE CASCADE" will NOT be added to the DDL generated for the relation. 
     * 
     * </pre>
     */
    public static enum DBCascadeAction
    {
        NONE,
        CASCADE,
        CASCADE_RECORDS
    }

	public static class DBReference implements Serializable
	{
	    private final static long serialVersionUID = 1L;
	    private DBTableColumn sourceColumn;
	    private DBTableColumn targetColumn;
	    
	    public DBReference(DBTableColumn sourceColumn, DBTableColumn targetColumn)
	    {
	       this.sourceColumn = sourceColumn;
	       this.targetColumn = targetColumn;
	    }

        public DBTableColumn getSourceColumn()
        {
            return sourceColumn;
        }

        public DBTableColumn getTargetColumn()
        {
            return targetColumn;
        }
	}

	// Members
    private DBDatabase    db;
    private String        name;
    private DBReference[] references;
    private DBCascadeAction onDeleteAction;

    /**
     * Creates a DBRelation object for a foreing key relation.
     * 
     * @param db the database object
     * @param name the name
     * @param references the references for this relation
     * @param onDeleteAction specifies the action performed when deleting affected records.
     */
	public DBRelation(DBDatabase db, String name, DBReference[] references, DBCascadeAction onDeleteAction)
	{
	   this.db         = db;
	   this.name       = name;
	   this.references = references;
	   this.onDeleteAction = onDeleteAction;
	}

    /**
     * Creates a DBRelation object for a foreing key relation.
     * 
     * @param db the database object
     * @param name the name
     * @param references the references for this relation
     */
    public DBRelation(DBDatabase db, String name, DBReference[] references)
    {
       this(db, name, references, DBCascadeAction.NONE);
    }
	
    /**
     * Returns the name.
     * 
     * @return Returns the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the full qualified table name.
     * 
     * @return the full qualified table name
     */
    public String getFullName()
    {
        String  schema = db.getSchema();
        return (schema!=null) ? schema+"."+name : name;
    }
    
    /**
     * Returns the references.
     * 
     * @return the references
     */
    public DBReference[] getReferences()
    {
        return references;
    }
    
    /**
     * Returns the table that is containing the foreign key (source table) 
     * @return true if the relation's source table is the given table 
     */
    public DBTable getForeignKeyTable()
    {
        return (DBTable)references[0].getSourceColumn().getRowSet();
    }

    /**
     * Returns the table that is referenced by this foreign key relation (target table)
     * @return true if the relation's target table 
     */
    public DBTable getReferencedTable()
    {
        return (DBTable)references[0].getTargetColumn().getRowSet();
    }
    

    @Override
    public DBDatabase getDatabase()
    {
        return db;
    }
    
    /**
     * return the action to perform when deleting affected records.
     * See DBCascadeAction enum for details.
     *
     * @return the action to perform when deleting affected records
     */
    public DBCascadeAction getOnDeleteAction()
    {
        return onDeleteAction;
    }

    /**
     * sets the action taken when deleting records that affect this foreign key relation
     * See DBCascadeAction enum for details.
     *
     * @param onDeleteAction the action to perform when deleting affected records
     */
    public void setOnDeleteAction(DBCascadeAction onDeleteAction)
    {
        this.onDeleteAction = onDeleteAction;
    }

    /**
     * short for 
     * <pre> 
     *  setOnDeleteAction(DBCascadeAction.CASCADE);
     * </pre>
     * See DBCascadeAction enum for details.
     */
    public void onDeleteCascade()
    {
        setOnDeleteAction(DBCascadeAction.CASCADE);
    }

    /**
     * short for 
     * <pre> 
     *  setOnDeleteAction(DBCascadeAction.CASCADE);
     * </pre>
     * See DBCascadeAction enum for details.
     */
    public void onDeleteCascadeRecords()
    {
        setOnDeleteAction(DBCascadeAction.CASCADE_RECORDS);
    }
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b.append("\"");
        b.append(this.getForeignKeyTable().getName());
        b.append("\" CONSTRAINT \"");
        b.append(this.name);
        b.append("\" FOREIGN KEY (");
        for (int i=0; i<references.length; i++)
        {
            b.append((i>0) ? ", \"" : "\"");
            b.append(references[i].getSourceColumn().getName());
            b.append("\"");
        }
        b.append(") REFERENCES \"");
        b.append(this.getReferencedTable().getName());
        b.append("\" (");
        for (int i=0; i<references.length; i++)
        {
            b.append((i>0) ? ", \"" : "\"");
            b.append(references[i].getTargetColumn().getName());
            b.append("\"");
        }
        b.append(")");
        return b.toString();
    }

}