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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used by the velocity templates.
 */
public class WriterService {

	private static final Logger log = LoggerFactory.getLogger(WriterService.class);

	private final CodeGenConfig config;
	private final Set<String> dbrecMethodNames;
	
	public WriterService(CodeGenConfig config)
	{
		this.dbrecMethodNames = loadDBRecordMethodNames();
		this.config = config;
	}
	
	/**
	 * Some g/setters like getState() can conflict with DBRecord
	 * methods. To check for conflicts, we'll need to know
	 * the method names.
	 * @return the DBRecord method's names
	 */
	protected Set<String> loadDBRecordMethodNames()
	{
		Method[] dbrecMethods = DBRecord.class.getMethods();
		Set<String> names = new HashSet<String>(dbrecMethods.length);
		for(Method method : dbrecMethods)
		{
			names.add(method.getName());
		}
		return names;
	}
	
	/**
	 * Returns the instance name of a table
	 */
	public String getTableName(DBTable t) {
		return StringUtils.toString(config.getTableNamePrefix(), "")
			 + deriveAttributeName(t.getName());
	}
	
	/**
	 * Returns the instance name of a view
	 */
	public String getViewName(DBView v) {
		return StringUtils.toString(config.getViewNamePrefix(), "")
			 + deriveAttributeName(v.getName());
	}
	
	/**
	 * Returns the instance name of a rowset
	 */
	public String getRowsetName(DBRowSet r) {
		// use same as table
		return StringUtils.toString(config.getTableNamePrefix(), "")
		 	 + deriveAttributeName(r.getName());
	}
	
	/**
	 * Returns the instance name of a column
	 */
	public String getColumnName(DBColumn c) {
		return StringUtils.toString(config.getColumnNamePrefix(), "")
			 + deriveAttributeName(c.getName());
	}

	/**
	 * Returns the java table class name for a given table name.
	 */
	public String getTableClassName(String tableName)
	{
		return StringUtils.toString(config.getTableClassPrefix(), "")
			+ deriveClassName(tableName)
			+ StringUtils.toString(config.getTableClassSuffix(),"");
	}

	/**
	 * Returns the java table class name for a given view name.
	 */
	public String getViewClassName(String viewName)
	{
		return StringUtils.toString(config.getViewClassPrefix(), "")
			+ deriveClassName(viewName)
			+ StringUtils.toString(config.getViewClassSuffix(), "");
	}
	
	/**
	 * Returns the java record class name for a given table name.
	 * 
	 * @param tableName the table name
	 */
	public String getRecordClassName(String tableName)
	{
		return deriveClassName(tableName) + "Record";
	}

	/**
	 * Returns the "getter" name for a given DBColumn.
	 * @param column the column
	 */
	public String getAccessorName(DBColumn column) 
	{
		return deriveAccessorName(column.getName(), getJavaType(column));
	}

	/**
	 * Returns the "setter" name for a given DBColumn
	 * @param column the column
	 */
	public String getMutatorName(DBColumn column) 
	{
		return deriveMutatorName(column.getName(), getJavaType(column));
	}
	
	/**
	 * Returns the attribute name for a given DBColumn
	public String getAttributeName(DBColumn c)
	{
		return deriveAttributeName(c.getName());
	}
	 */

	/**
	 * Returns whether the given table uses BigDecimal class or not. Velocity
	 * uses this information to generate the neccessary import expression.
	 * 
	 * @param table the table to inspect
	 */
	public boolean hasBigDecimalField(DBTable table)
	{	
		for (DBColumn column : table.getColumns())
		{
			if (getJavaType(column) == BigDecimal.class)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the given table uses Date class or not. Velocity
	 * uses this information to generate the neccessary import expression.
	 * 
	 * @param table the table to inspect
	 */
	public boolean hasDateField(DBTable table)
	{
		for (DBColumn column : table.getColumns())
		{
			if (getJavaType(column) == Date.class)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the given table has a locking column or not.
	 * 
	 * @param table the table to inspect
	 */
	public boolean hasLockingColumn(DBTable table)
	{
		return table.getTimestampColumn() != null;
	}

	/**
	 * Returns the corresponding java type of the given empire DataType.
	 * 
	 * @param column the column to get the type for
	 */
	public Class<?> getJavaType(DBColumn column)
	{
		DataType type = getDataType(column);
		// We added the attribute of original datatype to AUTOINC columns
		// in CodeGenParser.addColumn(). Now we need to use it so that
		// the g/setters deal with the right Java type.
		
		// If the original data type was not set as an attribute for some
		// reason this will just fall through to the bottom and
		// return "Byte[]", so no problem.
		if (DataType.AUTOINC.equals(type) && null != column.getAttribute("AutoIncDataType"))
		{
			type = (DataType)column.getAttribute("AutoIncDataType");
		}
		
		// TODO might be better to add this to the enum
		// TODO use primitives for non-nullable columns?
		switch(type){
        case AUTOINC:
		case INTEGER:
			return Long.class;
        case VARCHAR:
			return String.class;
		case DATE:
		case DATETIME:
        case TIMESTAMP:
			return Date.class;
		case CHAR:
			return String.class;
		case FLOAT:
			return Double.class;
		case DECIMAL:
			return BigDecimal.class;
		case BOOL:
			return Boolean.class;
		case CLOB:
			return String.class;
		case BLOB:
			return Byte[].class;
		case UNKNOWN:
			return Byte[].class;
		default:
			log.warn("SQL column type " + type.toString() + " not supported, falling back to byte array.");
			return Byte[].class;
		}
	}

	/**
	 * Returns the empire DataType of the given DBColumn.
	 */
	public DataType getDataType(DBColumn c)
	{
		DBTableColumn dbC = (DBTableColumn) c;
		return dbC.getDataType();
	}

	/**
	 * Returns the default value of the given DBColumn.
	 */
	protected String getDefaultValue(DBColumn c)
	{
		DBTableColumn dbC = (DBTableColumn) c;
		Object val = dbC.getDefaultValue();
		if (val == null)
		{
			return null;
		}
		if (val instanceof Number)
		{
		    return String.valueOf(val);
		}
		return "\"" + String.valueOf(val) + "\"";
	}
	
    /**
     * Returns the list of key columns
     */
    public String getKeyColumns(DBTable t)
    {
        DBColumn[] keyColumns =t.getKeyColumns();
        StringBuilder b = new StringBuilder();
        for (int i=0; i<keyColumns.length; i++)
        {
            if (i>0)
                b.append(", ");
            b.append(keyColumns[i].getName());
        }
        return b.toString();
    }
	
	/**
	 * Derives a java class name from a database table name.
	 */
	protected String deriveClassName(String name)
	{
		// PreserverCharacterCase
		if (config.isPreserverCharacterCase()) {
			return name;
		}
		// Build camel case string
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(name.charAt(0)));
		// Tables might already be camel case. Let's skip this if no '_' anywhere.
		/*
		if(name.substring(1).indexOf('_') <= 0)
		{
			if(name.length() > 1)
				sb.append(name.substring(1).toLowerCase());
			return sb.toString();
		}
		*/
		boolean nextCharacterUppercase = false;
		for (int i = 1; i < name.length(); i++) 
		{
			char c = name.charAt(i);
			if (c == '_') {
				nextCharacterUppercase = true;
				continue;
			}
			if (nextCharacterUppercase)
				sb.append(Character.toUpperCase(c));
			else
				sb.append(Character.toLowerCase(c));
			nextCharacterUppercase = false;
		}
		return sb.toString();
	}

	/**
	 * Derives the accessor method name based on the attribute name.
	 * 
	 * @param attribute
	 * @param isBoolean
	 * @return
	 */
	protected String deriveAccessorName(String attribute, Class<?> type)
	{
		return deriveRecordMethodName(attribute, type, true);
	}
	
	/**
	 * We need to alter both getter and setter if the method name will
	 * conflict with existing methods DBRecord. This will check both
	 * so that getter and setter have matching suffixes if one or 
	 * the other conflicts with an existing method.
	 */
	protected String deriveRecordMethodName(String attribute, Class<?> type, boolean isGetter) {
		attribute = deriveAttributeName(attribute);
		StringBuilder attributeName = new StringBuilder();
		attributeName.append(Character.toUpperCase(attribute.charAt(0)));
		
		// convert the method's name to CamelCase
		boolean nextCharacterUppercase = false;
		for (int i = 1; i < attribute.length(); i++) 
		{
			char c = attribute.charAt(i);
			if (c == '_') {
				nextCharacterUppercase = true;
				continue;
			}
			if (nextCharacterUppercase)
				attributeName.append(Character.toUpperCase(c));
			else
				attributeName.append(Character.toLowerCase(c));
			nextCharacterUppercase = false;
		}
		
		StringBuilder sbGet = new StringBuilder(getGetterPrefix(type));
		sbGet.append(attributeName);
		
		StringBuilder sbSet = new StringBuilder("set");
		sbSet.append(attributeName);
		attributeName = isGetter ? sbGet : sbSet;
		if(dbrecMethodNames.contains(sbGet.toString()) || dbrecMethodNames.contains(sbSet.toString()))
		{
			// Any change will resolve the conflict.
			attributeName.append("Column");
		}
		return attributeName.toString();
	}
	
	protected String getGetterPrefix(Class<?> type){
		if (type == boolean.class || type == Boolean.class)
		{
			return "is";
		}
		else
		{
			return "get";
		}
	}

	/**
	 * Derives the mutator method name based on the attribute name.
	 * 
	 * @param attribute
	 * @return
	 */
	protected String deriveMutatorName(String attribute, Class<?> type)
	{
		return deriveRecordMethodName(attribute, type, false);
	}
	
	/**
	 * Derives the attribute name based on the column name.
	 * 
	 * @param attribute
	 * @return
	 */
	protected String deriveAttributeName(String column)
	{
        // find invalid chars
        char[] invalidChars = new char[] { '$','#' };
        for (int i=0; i<invalidChars.length; i++)
        {   // Remove
            char c = invalidChars[i];
            if (column.indexOf(c)>=0)
                column=StringUtils.remove(column, c);
        }
        // replace dash
        if (column.indexOf('-')>=0)
            column=column.replace('-','_');
        // replace space
		return column.replace(' ', '_');
	}

}
