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
package org.apache.empire.db.driver.oracle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBDriverFeature;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBSqlPhrase;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.driver.DBDatabaseDriverBase;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the Oracle database system.<br>
 * Oracle Version 9 or higher is required.
 */
public class DBDatabaseDriverOracle extends DBDatabaseDriverBase
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBDatabaseDriverOracle.class);

    // Implementation of boolean types
    public enum BooleanType
    {
        CHAR,       // as CHAR(1) with 'Y' for true and 'N' for false
        NUMBER      // as NUMBER(1) with 1 for true and 0 for false
    }
    
    private boolean oracle8Compatibilty = false;

    private BooleanType booleanType = BooleanType.NUMBER;
    
    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation

    /**
     * Constructor for the Oracle database driver.<br>
     * 
     */
    public DBDatabaseDriverOracle()
    {
        // Info
        log.info("DBDatabaseDriverOracle created. Boolean Type is " + booleanType);
        // Additional reserved names 
        this.reservedSQLKeywords.add("date");
        this.reservedSQLKeywords.add("number");
    }

    public boolean isOracle8Compatibilty()
    {
        return oracle8Compatibilty;
    }

    public void setOracle8Compatibilty(boolean oracle8Compatibilty)
    {
        this.oracle8Compatibilty = oracle8Compatibilty;
    }

    public BooleanType getBooleanType()
    {
        return booleanType;
    }

    public void setBooleanType(BooleanType booleanType)
    {
        this.booleanType = booleanType;
        log.info("DBDatabaseDriverOracle Boolean Type set to " + booleanType);
    }

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: 	return false;
            case SEQUENCES:     	return true;
            case SEQUENCE_NEXTVAL:  return true;
            case QUERY_LIMIT_ROWS:  return true;
            case QUERY_SKIP_ROWS:   return true;
            default:
                // All other features are not supported by default
                return false;
        }
    }

    /**
     * Creates a new Oracle command object.
     * 
     * @return the new DBCommandOracle object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create oracle command
        return new DBCommandOracle(db);
    }

    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBDatabaseDriver#getSQLPhrase(int)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(DBSqlPhrase phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL:                return "null";
            case SQL_PARAMETER:                 return " ? ";
            case SQL_RENAME_TABLE:              return " ";
            case SQL_RENAME_COLUMN:             return " AS ";
            case SQL_DATABASE_LINK:             return "@";
            case SQL_QUOTES_OPEN:               return "\"";
            case SQL_QUOTES_CLOSE:              return "\"";
            case SQL_CONCAT_EXPR:               return " || ";
            case SQL_PSEUDO_TABLE:              return "DUAL";
            // data types
            case SQL_BOOLEAN_TRUE:              return (booleanType==BooleanType.CHAR) ? "'Y'" : "1";
            case SQL_BOOLEAN_FALSE:             return (booleanType==BooleanType.CHAR) ? "'N'" : "0";
            case SQL_CURRENT_DATE:              return "sysdate";
            case SQL_DATE_PATTERN:              return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:             return "TO_DATE('{0}', 'YYYY-MM-DD')";
            case SQL_CURRENT_TIMESTAMP:         return "systimestamp";
            case SQL_DATETIME_PATTERN:          
            case SQL_TIMESTAMP_PATTERN:         return "yyyy-MM-dd HH:mm:ss.SSS";
            case SQL_DATETIME_TEMPLATE:
            case SQL_TIMESTAMP_TEMPLATE:        return "TO_TIMESTAMP('{0}', 'YYYY.MM.DD HH24:MI:SS.FF')";
            // functions
            case SQL_FUNC_COALESCE:             return "nvl(?, {0})";
            case SQL_FUNC_SUBSTRING:            return "substr(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:          return "substr(?, {0}, {1})";
            case SQL_FUNC_REPLACE:              return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:              return "reverse(?)"; 
            case SQL_FUNC_STRINDEX:             return "instr(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:         return "instr(?, {0}, {1})"; 
            case SQL_FUNC_LENGTH:               return "length(?)";
            case SQL_FUNC_UPPER:                return "upper(?)";
            case SQL_FUNC_LOWER:                return "lower(?)";
            case SQL_FUNC_TRIM:                 return "trim(?)";
            case SQL_FUNC_LTRIM:                return "ltrim(?)";
            case SQL_FUNC_RTRIM:                return "rtrim(?)";
            case SQL_FUNC_ESCAPE:               return "? escape '{0}'";
            // Numeric
            case SQL_FUNC_ABS:                  return "abs(?)";
            case SQL_FUNC_ROUND:                return "round(?,{0})";
            case SQL_FUNC_TRUNC:                return "trunc(?,{0})";
            case SQL_FUNC_CEILING:              return "ceil(?)";
            case SQL_FUNC_FLOOR:                return "floor(?)";
            case SQL_FUNC_MODULO:               return "mod(?,{0})";
            case SQL_FUNC_FORMAT:               return "TO_CHAR(?, {0:VARCHAR})";
            // Date
            case SQL_FUNC_DAY:                  return oracle8Compatibilty ? "to_number(to_char(?,'DD'))"   : "extract(day from ?)";
            case SQL_FUNC_MONTH:                return oracle8Compatibilty ? "to_number(to_char(?,'MM'))"   : "extract(month from ?)";
            case SQL_FUNC_YEAR:                 return oracle8Compatibilty ? "to_number(to_char(?,'YYYY'))" : "extract(year from ?)";
            // Aggregation
            case SQL_FUNC_SUM:                  return "sum(?)";
            case SQL_FUNC_MAX:                  return "max(?)";
            case SQL_FUNC_MIN:                  return "min(?)";
            case SQL_FUNC_AVG:                  return "avg(?)";
            // Others
            case SQL_FUNC_DECODE:               return "decode(? {0})";
            case SQL_FUNC_DECODE_SEP:           return ",";
            case SQL_FUNC_DECODE_PART:          return "{0}, {1}";
            case SQL_FUNC_DECODE_ELSE:          return "{0}";
            // Not defined
            default:
                // log.warn("SQL phrase " + phrase.name() + " is not defined!");
                return phrase.getSqlDefault();
        }
    }

    /**
     * @see DBDatabaseDriver#getConvertPhrase(DataType, DataType, Object)
     */
    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        switch (destType)
        {
            /*
             * case DBExpr.DT_BOOL: return "convert(bit, ?)"; case DBExpr.DT_INTEGER: return "convert(int, ?)"; case
             * DBExpr.DT_DECIMAL: return "convert(decimal, ?)"; case DBExpr.DT_NUMBER: return "convert(float, ?)"; case
             * DBExpr.DT_DATE: return "convert(datetime, ?, 111)"; case DBExpr.DT_DATETIME: return "convert(datetime, ?, 120)";
             */
            // Convert to text
            case VARCHAR:
            case CHAR:
            case CLOB:
                if (format != null)
                { // Convert using a format string
                    return "to_char(?, '"+format.toString()+"')";
                }
                return "to_char(?)";
            // Convert to number
            case INTEGER:
            case FLOAT:
            case DECIMAL:
                if (format != null)
                { // Convert using a format string
                    return "to_number(?, '"+format.toString()+"')";
                }
                return "to_number(?)";
            // Convert to date
            case DATE:
            case DATETIME:
                if (format != null)
                { // Convert using a format string
                    return "to_date(?, '"+format.toString()+"')";
                }
                return "to_date(?)";
            case TIMESTAMP:
                if (format != null)
                { // Convert using a format string
                    return "to_timestamp(?, '"+format.toString()+"')";
                }
                return "to_timestamp(?)";
            // Unknown Type
            default:
                log.error("getConvertPhrase: unknown type " + destType);
                return "?";
        }
    }

    /**
     * Extracts native error message of an sqlExeption.
     * 
     * @param sqle the SQLException
     * @return the error message of the database 
     */
    @Override
    public String extractErrorMessage(SQLException sqle)
    {
        String msg = sqle.getMessage();
        msg = msg.substring(msg.indexOf(':') + 1);
        // Find End
        int end = msg.indexOf("ORA");
        if (end >= 0)
            msg = msg.substring(0, end - 1);
        return msg;
    }
    
    /**
     * Gets the value of a sql ResultSet.
     * Gives the driver the oportunity to change the value
     * i.e. to simulate missing data types with other types.
     * 
     * @param rset the sql Resultset with the current data row
     * @param columnIndex one based column Index of the desired column
     * @param dataType the desired data type
     * @return the value of the column 
     */
    @Override
    public Object getResultValue(ResultSet rset, int columnIndex, DataType dataType)
        throws SQLException
    {
        // Check for character large object
        if (dataType == DataType.BOOL)
        {   // Get character large object
            String val = rset.getString(columnIndex);
            if (val==null || rset.wasNull())
                return null;
            // Check Value
            if (val.equalsIgnoreCase("Y") || val.equals("1"))
                return Boolean.TRUE;
            return Boolean.FALSE;    
        }
        // Default
        return super.getResultValue(rset, columnIndex, dataType);
    }

    /**
     * @see DBDatabaseDriver#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    { // Use Oracle Sequences
        StringBuilder sql = new StringBuilder(80);
        sql.append("SELECT ");
        db.appendQualifiedName(sql, seqName, null);
        sql.append(".NEXTVAL FROM DUAL");
        // Query next sequence value
        String sqlCmd = sql.toString();
        if (log.isDebugEnabled())
            log.debug("Executing: " + sqlCmd);
        Object val = querySingleValue(sqlCmd, null, DataType.UNKNOWN, conn);
        if (ObjectUtils.isEmpty(val))
        {   // Error!
            log.error("getNextSequenceValue: Invalid sequence value for sequence " + seqName);
            throw new QueryNoResultException(sqlCmd);
        }
        // Done
        return val;
    }

    /**
     * @see DBDatabaseDriver#getNextSequenceValueExpr(DBTableColumn col)
     */
    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        String seqName = StringUtils.toString(column.getDefaultValue());
        if (StringUtils.isEmpty(seqName))
            throw new InvalidArgumentException("column", column);
        StringBuilder sql = new StringBuilder(80);
        column.getDatabase().appendQualifiedName(sql, seqName, null);
        sql.append(".NEXTVAL");
        return new DBValueExpr(column.getDatabase(), sql.toString(), DataType.UNKNOWN);
    }
    
    /**
     * Overridden. Returns a timestamp that is used for record updates created by the database server.
     * 
     * @return the current date and time of the database server.
     */
    @Override
    public java.sql.Timestamp getUpdateTimestamp(Connection conn)
    {
        // Default implementation
        ResultSet rs = null;
        try
        {   // Oracle Timestamp query
            rs = executeQuery("SELECT systimestamp FROM DUAL", null, false, conn);
            return (rs.next() ? rs.getTimestamp(1) : null);
        } catch (SQLException e) {
            // throw exception
            throw new EmpireSQLException(this, e);
        } finally
        { // Cleanup
            try
            { // ResultSet close
                Statement stmt = (rs!=null) ?  rs.getStatement() : null;
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                // throw exception
                throw new EmpireSQLException(this, e);
            }
        }
    }

    /**
     * @see DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        if (ddlGenerator==null)
            ddlGenerator = new OracleDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
    }

    /**
     * @see DBDatabaseDriver#appendEnableRelationStmt(DBRelation, boolean, DBSQLScript)  
     */
    @Override
    public void appendEnableRelationStmt(DBRelation r, boolean enable, DBSQLScript script)
    {
        // ALTER TABLE {table.name} {ENABLE|DISABLE} CONSTRAINT {relation.name}
        StringBuilder b = new StringBuilder();
        b.append("ALTER TABLE ");
        r.getForeignKeyTable().addSQL(b, DBExpr.CTX_FULLNAME);
        b.append(enable ? " ENABLE " : " DISABLE ");
        b.append("CONSTRAINT ");
        b.append(r.getName());
        // add
        script.addStmt(b);
    }
    
    /**
     * Checks whether the database definition matches the real database structure.
     */
    @Override
    public void checkDatabase(DBDatabase db, String owner, Connection conn)
    {
        DBContext context = new DBContextStatic(this, conn);
        // Check Params
        if (owner==null || owner.length()==0)
            throw new InvalidArgumentException("owner", owner);
        // Database definition
        OracleSYSDatabase sysDB = new OracleSYSDatabase(this);
        // Check Columns
        DBCommand sysDBCommand = sysDB.createCommand();
        sysDBCommand.select(sysDB.CI.getColumns());
        sysDBCommand.where (sysDB.CI.C_OWNER.is(owner));
        
        OracleDataDictionnary dataDictionnary = new OracleDataDictionnary();
        DBReader rd = new DBReader(context);
        try
        {
            rd.open(sysDBCommand);
            // read all
            log.info("---------------------------------------------------------------------------------");
            log.info("checkDatabase start: " + db.getClass().getName());
            String skipTable = "";
            while (rd.moveNext())
            {
                String tableName = rd.getString(sysDB.CI.C_TABLE_NAME);

                // if a table wasn't found before, skip it
                if (tableName.equals(skipTable))
                    continue;

                // check if the found table exists in the DBDatabase object
                String columnName = rd.getString(sysDB.CI.C_COLUMN_NAME);
                DBTable dbTable = db.getTable(tableName);
                DBView  dbView  = db.getView(tableName);
                
                String dataType = rd.getString(sysDB.CI.C_DATA_TYPE);
                int charLength = rd.getInt(sysDB.CI.C_CHAR_LENGTH);
                int dataLength = rd.getInt(sysDB.CI.C_DATA_LENGTH);
                int dataPrecision = rd.getInt(sysDB.CI.C_DATA_PRECISION);
                int dataScale = rd.getInt(sysDB.CI.C_DATA_SCALE);
                String nullable = rd.getString(sysDB.CI.C_NULLABLE);
                
                dataDictionnary.fillDataDictionnary(tableName, columnName, dataType, 
                                                    charLength, dataLength, dataPrecision, dataScale, nullable);
                
                if (dbTable != null)
                {
                    
                    // check if the found column exists in the found DBTable
                    DBColumn col = dbTable.getColumn(columnName);
                    if (col == null)
                    {
                        log.warn("COLUMN NOT FOUND IN " + db.getClass().getName() + "\t: [" + tableName + "]["
                                       + columnName + "][" + dataType + "][" + dataLength + "]");
                        continue;
                    }
                    /*
                    else
                    {   // check the DBTableColumn definition
                        int length = (charLength>0) ? charLength : dataLength;
                        dataDictionnary.checkColumnDefinition(col, dataType, length, dataPrecision, dataScale, nullable.equals("N"));
                    }
                    */
                } 
                else if (dbView!=null)
                {
                    log.debug("Column check for view " + tableName + " not yet implemented.");
                } 
                else
                {
                    log.debug("TABLE OR VIEW NOT FOUND IN " + db.getClass().getName() + "\t: [" + tableName + "]");
                    // skip this table
                    skipTable = tableName;
                    continue;
                }
            }
            // check Tables
            dataDictionnary.checkDBTableDefinition(db.getTables());
            // check Views
            dataDictionnary.checkDBViewDefinition (db.getViews());
            // done
            log.info("checkDatabase end: " + db.getClass().getName());
            log.info("---------------------------------------------------------------------------------");
        } finally {
            // close
            rd.close();
        }
    }

}

