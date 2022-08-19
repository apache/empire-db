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
package org.apache.empire.dbms.oracle;

// Java
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * implements a column expression for the Oracle rownum function
 */
public class OracleRowNumExpr extends DBColumnExpr
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(OracleRowNumExpr.class);
    
    public final DBDatabase   db;

    /**
     * Constructs a new OracleRowNumExpr object.
     * 
     * @param db the database
     */
    public OracleRowNumExpr(DBDatabase db)
    {
        this.db = db;
    }

    /**
     * Returns the current DBDatabase object.
     * @return the current DBDatabase object
     */
    @SuppressWarnings("unchecked")
    @Override
    public final DBDatabase getDatabase()
    {
        return db;
    }

    /**
     * Returns the data type of the DBColumnExpr object.
     * 
     * @return the data type
     */
    @Override
    public DataType getDataType()
    {
        return DataType.INTEGER;
    }

    /**
     * Not an Enum. Returns null
     */
    @Override
    public Class<Enum<?>> getEnumType()
    {
        return null;
    }

    /**
     * Returns the column name.
     * 
     * @return the column name
     */
    @Override
    public String getName()
    {
        return "rownum";
    }

    /** this helper function calls the DBColumnExpr.addXML(Element, long) method */
    @Override
    public Element addXml(Element parent, long flags)
    {
        Element elem = XMLUtil.addElement(parent, "column");
        elem.setAttribute("name", getName());
        elem.setAttribute("function", "rownum");
        return elem;
    }

    /**
     * Returns null.
     * @return null
     */
    @Override
    public DBColumn getSourceColumn()
    {
        return null;
    }

    /**
     * Returns null.
     * @return null
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    /**
     * Always returns false
     * @return false
     */
    @Override
    public boolean isAggregate()
    {
        return false;
    }

    /**
     * Creates the SQL-Command.
     * 
     * @param sql the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(DBSQLBuilder sql, long context)
    {
    	if (!(db.getDbms() instanceof DBMSHandlerOracle))
    	{
    		log.warn("Oracle RowNumExpression can be used with Oracle databases only!");
    	}
        sql.append("rownum");
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {   // nothing to do!
        return;
    }

    /**
     * @see java.lang.Object#equals(Object) 
     */
    @Override
    public boolean equals(Object other)
    {
    	return (other instanceof OracleRowNumExpr);
    }

}
