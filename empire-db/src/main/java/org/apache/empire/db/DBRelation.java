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

/**
 * This class creates a DBReferene object for a foreing key relation.
 * 
 *
 */
public class DBRelation extends DBObject
{
	public static class DBReference
	{
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

    /**
     * Creates a DBRelation object for a foreing key relation.
     * 
     * @param db the database object
     * @param name the name
     * @param references the references for this relation
     */
	public DBRelation(DBDatabase db, String name, DBReference[] references)
	{
	   this.db         = db;
	   this.name       = name;
	   this.references = references;
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

    @Override
    public DBDatabase getDatabase()
    {
        return db;
    }

}