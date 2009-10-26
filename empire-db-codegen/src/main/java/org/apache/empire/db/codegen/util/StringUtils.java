package org.apache.empire.db.codegen.util;

public class StringUtils {
	/**
	 * Derives a java class name from a database table name.
	 * @param tableName
	 * @return
	 */
	public static String javaClassName(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(name.charAt(0)));
		boolean upperCase = false;
		for (int i=1; i<name.length(); i++) {
			char c = name.charAt(i);
			if (c == '_') {
				upperCase = true;
				continue;
			}
			if (upperCase) sb.append(Character.toUpperCase(c));
			else sb.append(Character.toLowerCase(c));
			upperCase = false;
		}
		return sb.toString();
	}
	/**
	 * Derives a java attribute name from a database column name.
	 * @param colName
	 * @return
	 */
	public static String deriveAttributeName(String colName) {
		String name = javaClassName(colName);
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toLowerCase(name.charAt(0)));
		sb.append(name.substring(1));
		return sb.toString();
	}
	/**
	 * Derives an attribute name from the given class name.
	 * @param className
	 * @return
	 */
	public static String deriveAttributeNameFromClass(String className) {
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toLowerCase(className.charAt(0)));
		sb.append(className.substring(1));
		return sb.toString();
	}
	/**
	 * Derives the accessor method name based on the attribute name.
	 * @param attribute
	 * @param isBoolean
	 * @return
	 */
	public static String deriveAccessorName(String attribute, boolean isBoolean) {
		StringBuilder sb = new StringBuilder();
		if (isBoolean) sb.append("is");
		else sb.append("get");
		sb.append(Character.toUpperCase(attribute.charAt(0)));
		sb.append(attribute.substring(1));
		return sb.toString();
	}
	/**
	 * Derives the mutator method name based on the attribute name.
	 * @param attribute
	 * @return
	 */
	public static String deriveMutatorName(String attribute) {
		StringBuilder sb = new StringBuilder();
		sb.append("set");
		sb.append(Character.toUpperCase(attribute.charAt(0)));
		sb.append(attribute.substring(1));
		return sb.toString();
	}

	public static StringBuilder replaceAll(StringBuilder sb, 
			String oldValue, String newValue) {
		int strIndex = sb.indexOf(oldValue);
		int endIndex;
		while (strIndex > -1) {
			endIndex = strIndex + oldValue.length();
			sb.replace(strIndex, endIndex, newValue);
			strIndex = sb.indexOf(oldValue, strIndex);
		}
		return sb;
	}
}
