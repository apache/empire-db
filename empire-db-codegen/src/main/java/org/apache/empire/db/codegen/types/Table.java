package org.apache.empire.db.codegen.types;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.data.DataType;
import org.apache.empire.db.codegen.util.StringUtils;

public class Table {
    private static final Log log = LogFactory.getLog(Table.class);
    
    private String tableName;
	private Column pkCol;
	private List<Column> fkCols;
	private List<Column> simpleCols;
	private Column lockingCol;
	
	private Map<String, Column> columnMap = new HashMap<String, Column>();
	
	private List<Child> childTables = new ArrayList<Child>();
	
	private static String pkColName;
	private static String lockColName;

	public Table(String tableName, String schema, 
			DatabaseMetaData dbMeta) {
		this.tableName = tableName.toUpperCase();
		this.createColumns(dbMeta, schema);
	}
	
	public static String getPkColName() {
		return pkColName;
	}
	public static void setPkColName(String name) {
		pkColName = name;
	}
	public static String getLockColName() {
		return lockColName;
	}
	public static void setLockColName(String name) {
		lockColName = name;
	}
	
	public void addChildTable(String fkColName, Table table) {
		this.childTables.add(new Child(fkColName, table));
	}
	public List<Child> getChildTables() {
		return this.childTables;
	}
	public String getTableName() {
		return tableName.toUpperCase();
	}

	public Column getPkCol() {
		return pkCol;
	}

	public List<Column> getFkCols() {
		return fkCols;
	}

	public List<Column> getSimpleCols() {
		return simpleCols;
	}

	public Column getLockingCol() {
		return this.lockingCol;
	}
	
	public String getClassName() {
		return StringUtils.javaClassName(this.tableName) + "Table";
	}
	public String getRecordClassName() {
		return StringUtils.javaClassName(this.tableName) + "Record";
	}
	public boolean hasBigDecimalField() {
		boolean bdField = false;
		for (Column col: this.simpleCols) {
			if (col.getEmpireType() == DataType.DECIMAL) {
				bdField = true;
				break;
			}
		}
		return bdField;
	}
	public boolean hasChildRecords() {
		return !this.childTables.isEmpty();
	}
	public Column getColumn(String name) {
		return this.columnMap.get(name.toUpperCase());
	}
	public static class Child {
		Child(String fkColName, Table childTable) {
			this.fkColName = fkColName;
			this.childTable = childTable;
		}
		private String fkColName;
		private Table childTable;
		public String getFkColName() {
			return fkColName.toUpperCase();
		}
		public Table getChildTable() {
			return childTable;
		}
		public String getChildType() {
			return StringUtils.javaClassName(childTable.getTableName());
		}
		public String getFkType() {
			StringBuilder sb = new StringBuilder(this.fkColName.toUpperCase());
			StringUtils.replaceAll(sb, "_" + Table.getPkColName(),
					"");
			return StringUtils.javaClassName(sb.toString());
		}
		public String getFkMutatorName() {
			Column col = childTable.getColumn(this.fkColName);
			return col.getJavaMutatorName();
		}
	}
	// ------------------------------------------------------------------------
	// Private members
	// ------------------------------------------------------------------------
	private void createColumns(DatabaseMetaData dbMeta, String schema) {
		this.pkCol = this.findPkColumn(dbMeta, schema);
		this.columnMap.put(pkCol.getName().toUpperCase(), pkCol);
		this.simpleCols = new ArrayList<Column>();
		this.fkCols = this.findFkColumns(dbMeta, schema);
		for (Column col: this.fkCols) {
			this.columnMap.put(col.getName().toUpperCase(), col);
		}
		try {
			ResultSet rs = dbMeta.getColumns(null, schema, tableName, "");			
			while (rs.next()) {
				Column col = new Column(rs);
				if (!col.equals(pkCol) && !fkCols.contains(col)) {
					if (col.getName().equalsIgnoreCase(lockColName)) {
						this.lockingCol = col;
						col.setLockingCol(true);
					}
					else {
						this.simpleCols.add(col);
					}
					this.columnMap.put(col.getName().toUpperCase(), col);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	private Column findPkColumn(DatabaseMetaData dbMeta, String schema) {
		try {
			ResultSet pkRs = dbMeta.getPrimaryKeys(null, schema, tableName);
			if(pkRs.next()) {
				Column col = new Column(pkRs, true);
				return col;
			}
			else {
				throw new RuntimeException("Primary key not found for table " +
						this.tableName);
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	private List<Column> findFkColumns(DatabaseMetaData dbMeta, String schema) {
		List<Column> fkCols = new ArrayList<Column>();
		try {
			ResultSet fkRs = dbMeta.getImportedKeys(null, schema, tableName);
			while(fkRs.next()) {
				Column col = new Column(fkRs, false, true);
				fkCols.add(col);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
		return fkCols;
	}
}
