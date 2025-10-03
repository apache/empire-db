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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBContext;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDDLGenerator.DDLActionType;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBMaterializedView;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLBuilder;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.db.exceptions.QueryNoResultException;
import org.apache.empire.db.expr.column.DBValueExpr;
import org.apache.empire.db.validation.DBModelChecker;
import org.apache.empire.dbms.DBMSFeature;
import org.apache.empire.dbms.DBMSHandler;
import org.apache.empire.dbms.DBMSHandlerBase;
import org.apache.empire.dbms.DBSqlPhrase;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides support for the Oracle database system.<br>
 * Oracle Version 9 or higher is required.
 */
public class DBMSHandlerOracle extends DBMSHandlerBase
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBMSHandlerOracle.class);

    // Implementation of boolean types
    public enum BooleanType
    {
        CHAR,       // as CHAR(1) with 'Y' for true and 'N' for false
        NUMBER      // as NUMBER(1) with 1 for true and 0 for false
    }
    
    // Additional Oracle Keywords
    protected static final String[] ORACLE_KEYWORDS = new String[] { "date", "number" };        
    
    private boolean oracle8Compatibilty = false;

    private BooleanType booleanType = BooleanType.NUMBER;
    
    private DBDDLGenerator<?> ddlGenerator = null; // lazy creation
    
    private String schemaName;

    /**
     * Constructor for the Oracle database dbms.<br>
     * 
     */
    public DBMSHandlerOracle()
    {
        // Add additional Keywords
        super(ORACLE_KEYWORDS);
        // Info
        log.info("DBMSHandlerOracle created. Boolean Type is " + booleanType);
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

    public String getSchemaName()
    {
        return schemaName;
    }

    public void setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
    }

    /**
     * Returns whether or not a particular feature is supported by this dbms
     * @param type type of requested feature. @see DBMSFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBMSFeature type)
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
    public DBCommand createCommand(boolean autoPrepareStmt)
    {
        // create oracle command
        return new DBCommandOracle(this, autoPrepareStmt);
    }

    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBMSHandler#getSQLPhrase(DBSqlPhrase)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(DBSqlPhrase phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL:                      return "null";
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
            case SQL_CURRENT_DATE:              return "trunc(sysdate)";
            case SQL_DATE_TEMPLATE:             return "TO_DATE('{0}', 'YYYY-MM-DD')";
            case SQL_CURRENT_TIME:              return "TO_DATE('1970-01-01'||TO_CHAR(sysdate, 'HH24:MI:SS'), 'YYYY-MM-DD HH24:MI:SS')";
            case SQL_TIME_TEMPLATE:             return "TO_DATE('1970-01-01 {0}', 'YYYY-MM-DD HH24:MI:SS')";
            case SQL_CURRENT_DATETIME:          return "sysdate";
            case SQL_DATETIME_TEMPLATE:         return "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')";
            case SQL_CURRENT_TIMESTAMP:         return "systimestamp";
            case SQL_TIMESTAMP_TEMPLATE:        return "TO_TIMESTAMP('{0}', 'YYYY-MM-DD HH24:MI:SS.FF')";
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
            case SQL_FUNC_ESCAPE:               return "? escape {0:VARCHAR}";
            case SQL_FUNC_CONCAT:               return "concat(?)"; // ATTENTION: only takes two parameters!
            case SQL_FUNC_CONTAINS:             return "contains(?, {0:VARCHAR})>0"; // for comparison only
            // Numeric
            case SQL_FUNC_ABS:                  return "abs(?)";
            case SQL_FUNC_ROUND:                return "round(?,{0})";
            case SQL_FUNC_TRUNC:                return "trunc(?,{0})";
            case SQL_FUNC_CEILING:              return "ceil(?)";
            case SQL_FUNC_FLOOR:                return "floor(?)";
            case SQL_FUNC_MOD:                  return "mod(?,{0})";
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
            case SQL_FUNC_STRAGG:               return "listagg(?,{0:VARCHAR}) WITHIN GROUP (ORDER BY {1})";
            // Others
            case SQL_FUNC_DECODE:               return "decode(?{0})";
            case SQL_FUNC_DECODE_SEP:           return ", ";
            case SQL_FUNC_DECODE_PART:          return "{0}, {1}";
            case SQL_FUNC_DECODE_ELSE:          return "{0}";
            // Not defined
            default:
                // log.warn("SQL phrase " + phrase.name() + " is not defined!");
                return phrase.getSqlDefault();
        }
    }

    /**
     * @see DBMSHandler#getConvertPhrase(DataType, DataType, Object)
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
                if (format instanceof String)
                { // Convert using a format string
                    return "to_char(?, '"+format.toString()+"')";
                }
                return "to_char(?)";
            // convert to clob
            case CLOB:
                return "to_clob(?)";
            // Convert to number
            case INTEGER:
            case FLOAT:
            case DECIMAL:
                if (format instanceof String)
                { // Convert using a format string
                    return "to_number(?, '"+format.toString()+"')";
                }
                return "to_number(?)";
            // Convert to date
            case DATE:
            case DATETIME:
                if (format instanceof String)
                { // Convert using a format string
                    return "to_date(?, '"+format.toString()+"')";
                }
                return "to_date(?)";
            case TIME:
                return "to_date(?, 'HH24:mm:ss')";
            case TIMESTAMP:
                if (format instanceof String)
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
     * Adds a statement parameter to a prepared statement
     */
    @Override
    protected void addStatementParam(PreparedStatement pstmt, int paramIndex, Object value)
        throws SQLException
    {
        if ((value instanceof Boolean) && (booleanType==BooleanType.CHAR))
        {   // for BooleanType.CHAR
            value = ((Boolean)value) ? 'Y' : 'N';
        }
        super.addStatementParam(pstmt, paramIndex, value);
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
     * Gives the dbms the oportunity to change the value
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
     * @see DBMSHandlerBase#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    { // Use Oracle Sequences
        DBSQLBuilder sql = createSQLBuilder();
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
     * @see DBMSHandlerBase#getNextSequenceValueExpr(DBTableColumn col)
     */
    @Override
    public DBColumnExpr getNextSequenceValueExpr(DBTableColumn column)
    {
        String seqName = StringUtils.toString(column.getDefaultValue());
        if (StringUtils.isEmpty(seqName))
            throw new InvalidArgumentException("column", column);
        DBSQLBuilder sql = createSQLBuilder();
        column.getDatabase().appendQualifiedName(sql, seqName, null);
        sql.append(".NEXTVAL");
        return column.getDatabase().getValueExpr(sql.toString(), DataType.UNKNOWN);
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
     * @see DBMSHandler#getDDLScript(DDLActionType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DDLActionType type, DBObject dbo, DBSQLScript script)
    {
        if (ddlGenerator==null)
            ddlGenerator = new OracleDDLGenerator(this);
        // forward request
        ddlGenerator.getDDLScript(type, dbo, script); 
    }

    /**
     * @see DBMSHandler#appendEnableRelationStmt(DBRelation, boolean, DBSQLScript)  
     */
    @Override
    public void appendEnableRelationStmt(DBRelation r, boolean enable, DBSQLScript script)
    {
        // ALTER TABLE {table.name} {ENABLE|DISABLE} CONSTRAINT {relation.name}
        DBSQLBuilder sql = createSQLBuilder();
        sql.append("ALTER TABLE ");
        r.getForeignKeyTable().addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(enable ? " ENABLE " : " DISABLE ");
        sql.append("CONSTRAINT ");
        sql.append(r.getName());
        // add
        script.addStmt(sql);
    }
    
    /**
     * Checks whether the database definition matches the real database structure.
     */
    public void checkDatabase(DBDatabase db, String owner, Connection conn)
    {
        DBContext context = new DBContextStatic(this, conn);
        // Check Params
        if (owner==null || owner.length()==0)
            throw new InvalidArgumentException("owner", owner);
        // Database definition
        OracleSYSDatabase sysDB = new OracleSYSDatabase(this);
        // Check Columns
        DBCommand sysDBCommand = createCommand(false);
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
    
    @Override
    public OracleDBModelParser createModelParser(String catalog, String schema)
    {
        // Check schema
        String schemaPattern = StringUtils.coalesce(schema, this.schemaName);
        if (StringUtils.isEmpty(schemaPattern))
            throw new InvalidPropertyException("schemaName", null);
        // create parser
        return new OracleDBModelParser(schemaPattern);
    }

    /**
     * Creates a DataModelChecker instance of this DBMSHandler
     * @return
     */
    @Override
    public DBModelChecker createModelChecker(DBDatabase db)
    {   // the default model checker
        OracleDBModelParser modelParser = createModelParser(null, (db!=null ? db.getSchema() : null));
        return new OracleDBModelChecker(modelParser, getBooleanType());
    }
                             
    /**
     * Immediately refreshes a Materialized View
     * @param matView the materialized view to refresh
     * @param context the database context
     */
    public void refreshMView(DBMaterializedView matView, DBContext context)
    {
        // check param
        if (matView==null || matView.getDatabase().getDbms()!=this)
            throw new InvalidArgumentException("matView", matView);
        // refresh command
        String refreshSqlCmd = StringUtils.concat("BEGIN DBMS_MVIEW.REFRESH('", matView.getFullName() , "'); END;");
        context.executeSQL(refreshSqlCmd, null);
    }

}

