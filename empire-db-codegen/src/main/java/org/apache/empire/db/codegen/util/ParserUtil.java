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
package org.apache.empire.db.codegen.util;

import java.lang.reflect.Method;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.codegen.CodeGenConfig;

/**
 * This class is used by the velocity templates.
 */
public class ParserUtil {

	private static final Log log = LogFactory.getLog(ParserUtil.class);

	private CodeGenConfig config;
	
	private static HashSet<String> dbrecMethodNames;
	
	public ParserUtil(CodeGenConfig config) {

		this.config = config;
	}
	
	public static HashSet<String> getDBRecordMethodNames(){
		// some g/setters like getState() can conflict with DBRecord
		// methods. To check for conflicts, we'll need to know
		// the method names.
		if(dbrecMethodNames == null){
			Method[] dbrecMethods = DBRecord.class.getMethods();
			dbrecMethodNames = new HashSet<String>(dbrecMethods.length);
			for(Method method : dbrecMethods){
				dbrecMethodNames.add(method.getName());
			}
		}
		return dbrecMethodNames;
	}

	/**
	 * Returns the java table class name for a given table name.
	 */
	public String getTableClassName(String tableName) {
		return config.getTableClassPrefix() + javaClassName(tableName)
				+ config.getTableClassSuffix();
	}

	/**
	 * Returns the java table class name for a given view name.
	 */
	public String getViewClassName(String viewName) {
		return config.getViewClassPrefix() + javaClassName(viewName)
		+ config.getViewClassSuffix();
	}
	
	/**
	 * Returns the java record class name for a given table name.
	 */
	public String getRecordClassName(String tableName) {
		return javaClassName(tableName) + "Record";
	}

	/**
	 * Returns the "getter" name for a given DBColumn.
	 */
	public String getAccessorName(DBColumn c) {

		return deriveAccessorName(c.getName(), getJavaType(c).equalsIgnoreCase(
				"Boolean"));
	}

	/**
	 * Returns the "setter" name for a given DBColumn
	 */
	public String getMutatorName(DBColumn c) {

		return deriveMutatorName(c.getName(), getJavaType(c).equalsIgnoreCase("Boolean"));
	}
	
	/**
	 * Returns the attribute name for a given DBColumn
	 */
	public String getAttributeName(DBColumn c) {

		return deriveAttributeName(c.getName());
	}

	/**
	 * Returns whether the given table uses BigDecimal class or not. Velocity
	 * uses this information to generate the neccessary import expression.
	 */
	public boolean hasBigDecimalField(DBTable t) {
		
		for (DBColumn c : t.getColumns()){
			if (getJavaType(c).equalsIgnoreCase("BigDecimal"))
				return true;
		}
		return false;
	}

	/**
	 * Returns whether the given table uses Date class or not. Velocity
	 * uses this information to generate the neccessary import expression.
	 */
	public boolean hasDateField(DBTable t) {

		for (DBColumn c : t.getColumns())
		{
			if (getJavaType(c).equalsIgnoreCase("Date"))
				return true;
		}
		return false;
	}

	/**
	 * Returns whether the given table has a locking column or not.
	 */
	public boolean hasLockingColumn(DBTable t) {

		return t.getTimestampColumn() != null;
	}

	/**
	 * Returns the corresponding java type of the given empire DataType.
	 */
	public String getJavaType(DBColumn c) {
		DataType type = getDataType(c);
		// We added the attribute of original datatype to AUTOINC columns
		// in CodeGenParser.addColumn(). Now we need to use it so that
		// the g/setters deal with the right Java type.
		
		// If the original data type was not set as an attribute for some
		// reason this will just fall through to the bottom and
		// return "Byte[]", so no problem.
		if (type.equals(DataType.AUTOINC) && null != c.getAttribute("AutoIncDataType"))
			type = (DataType)c.getAttribute("AutoIncDataType");
		
		if (type.equals(DataType.INTEGER))
			return "Long";
		else if (type.equals(DataType.TEXT))
			return "String";
		else if (type.equals(DataType.DATE))
			return "Date";
		else if (type.equals(DataType.DATETIME))
			return "Date";
		else if (type.equals(DataType.CHAR))
			return "String";
		else if (type.equals(DataType.DOUBLE))
			return "Double";
		else if (type.equals(DataType.DECIMAL))
			return "BigDecimal";
		else if (type.equals(DataType.BOOL))
			return "Boolean";
		else if (type.equals(DataType.CLOB))
			return "String";
		else if (type.equals(DataType.BLOB))
			return "Byte[]";
		else if (type.equals(DataType.UNKNOWN))
			return "Byte[]";
		else {
			log.warn("SQL column type " + type.toString() + " not supported.");
			return "Byte[]";
		}
	}

	/**
	 * Returns the empire DataType of the given DBColumn.
	 */
	public DataType getDataType(DBColumn c) {
		DBTableColumn dbC = (DBTableColumn) c;
		return dbC.getDataType();
	}

	/**
	 * Returns the default value of the given DBColumn.
	 */
	public String getDefaultValue(DBColumn c) {
		DBTableColumn dbC = (DBTableColumn) c;
		Object val = dbC.getDefaultValue();
		if (val == null)
			return "null";

		return "\"" + val + "\"";
	}

	// ----------- private members
	
	
	/**
	 * Derives a java class name from a database table name.
	 */
	private static String javaClassName(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(name.charAt(0)));
		// Tables might already be camel case. Let's skip this if no '_' anywhere.
		if(name.substring(1).indexOf('_') <= 0) {
			if(name.length() > 1)
				sb.append(name.substring(1));
			return sb.toString();
		}
		boolean upperCase = false;
		for (int i = 1; i < name.length(); i++) 
		{
			char c = name.charAt(i);
			if (c == '_') {
				upperCase = true;
				continue;
			}
			if (upperCase)
				sb.append(Character.toUpperCase(c));
			else
				sb.append(Character.toLowerCase(c));
			upperCase = false;
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
	private static String deriveAccessorName(String attribute, boolean isBoolean) {
		return deriveRecordMethodName(attribute, isBoolean, true);
	}
	
	// We need to alter both getter and setter if the method name will
	// conflict with existing methods DBRecord. This will check both
	// so that getter and setter have matching suffixes if one or 
	// the other conflicts with an existing method.
	private static String deriveRecordMethodName(String attribute, boolean isBoolean, boolean isGetter) {
		attribute = deriveAttributeName(attribute);
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(attribute.charAt(0)));
		sb.append(attribute.substring(1));
		
		StringBuilder sbGet = new StringBuilder();
		if (isBoolean)
			sbGet.append("is");
		else
			sbGet.append("get");
		sbGet.append(sb);
		
		StringBuilder sbSet = new StringBuilder("set");
		sbSet.append(sb);
		sb = isGetter ? sbGet : sbSet;
		HashSet<String> names = getDBRecordMethodNames();
		if(names.contains(sbGet.toString()) || names.contains(sbSet.toString()))
			sb.append("Column"); // Any change will resolve the conflict.
		return sb.toString();
	}

	/**
	 * Derives the mutator method name based on the attribute name.
	 * 
	 * @param attribute
	 * @return
	 */
	private static String deriveMutatorName(String attribute, boolean isBoolean) {
		return deriveRecordMethodName(attribute, isBoolean, false);
	}
	
	/**
	 * Derives the attribute name based on the column name.
	 * 
	 * @param attribute
	 * @return
	 */
	private static String deriveAttributeName(String column) {
		return column.replace(' ', '_');
	}

}
