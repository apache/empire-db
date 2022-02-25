package org.apache.empire.dbms;

/**
 * Enum for all SQL phrases that may be supplied by the dbms
 * @author rainer
 */
public enum DBSqlPhrase
{
    // sql-phrases
    SQL_NULL                ("null"),
    SQL_PARAMETER           ("?"),
    SQL_RENAME_TABLE        (" "),
    SQL_RENAME_COLUMN       (" AS "),
    SQL_DATABASE_LINK       ("@"),          // Oracle
    SQL_QUOTES_OPEN         ("\""),         // MSSQL: [
    SQL_QUOTES_CLOSE        ("\""),         // MSSQL: ]
    SQL_CONCAT_EXPR         ("+"),          // Oracle: "||"
    SQL_PSEUDO_TABLE        (""),           // Oracle: "DUAL"

    // data types
    SQL_BOOLEAN_TRUE        ("1"),          // Oracle Y
    SQL_BOOLEAN_FALSE       ("0"),          // Oracle N
    SQL_CURRENT_DATE        ("sysdate"),    // Oracle
    SQL_DATE_PATTERN        ("yyyy-MM-dd"), // SimpleDateFormat
    SQL_DATE_TEMPLATE       ("TO_DATE('{0}', 'YYYY-MM-DD')"),   // MSSql: convert(date, '{0}', 111)
    SQL_DATETIME_PATTERN    ("yyyy-MM-dd HH:mm:ss.SSS"),        // SimpleDateFormat
    SQL_DATETIME_TEMPLATE   ("TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')"), // Oracle
    SQL_CURRENT_TIMESTAMP   ("systimestamp"),                   // Oracle
    SQL_TIMESTAMP_PATTERN   ("yyyy-MM-dd HH:mm:ss.SSS"),        // SimpleDateFormat
    SQL_TIMESTAMP_TEMPLATE  ("TO_TIMESTAMP('{0}', 'YYYY.MM.DD HH24:MI:SS.FF')"), // Oracle

    // functions
    SQL_FUNC_COALESCE       ("coalesce(?, {0})"),       // Oracle: nvl(?, {0})
    SQL_FUNC_SUBSTRING      ("substring(?,{0})"),       // Oracle: substr(?,{0})
    SQL_FUNC_SUBSTRINGEX    ("substring(?, {0}, {1})"), // Oracle: substr(?,{0},{1})
    SQL_FUNC_REPLACE        ("replace(?, {0}, {1})"),   // Oracle: replace(?,{0},{1})
    SQL_FUNC_REVERSE        ("reverse(?)"),             // Oracle: reverse(?) 
    SQL_FUNC_STRINDEX       ("charindex({0}, ?)"),      // Oracle: instr(?, {0})
    SQL_FUNC_STRINDEXFROM   ("charindex({0}, ?, {1})"), // Oracle: instr(?, {0}, {1}) 
    SQL_FUNC_LENGTH         ("length(?)"),              // MSSql: len(?)
    SQL_FUNC_UPPER          ("upper(?)"),
    SQL_FUNC_LOWER          ("lower(?)"),
    SQL_FUNC_TRIM           ("trim(?)"),
    SQL_FUNC_LTRIM          ("ltrim(?)"),
    SQL_FUNC_RTRIM          ("rtrim(?)"),
    SQL_FUNC_ESCAPE         ("? escape '{0}'"),
    SQL_FUNC_CONCAT         ("concat(?)"),
    
    // Numeric
    SQL_FUNC_ABS            ("abs(?)"),
    SQL_FUNC_ROUND          ("round(?, {0})"),
    SQL_FUNC_TRUNC          ("trunc(?, {0})"),
    SQL_FUNC_FLOOR          ("floor(?)"),
    SQL_FUNC_CEILING        ("ceiling(?)"),             // Oracle: ceil(?)
    SQL_FUNC_MODULO         ("((?) % {0})"),            // Oracle: mod(?)
    SQL_FUNC_FORMAT         ("format(?, {0:VARCHAR})"), // Oracle: TO_CHAR(?, {0:VARCHAR})

    // Date
    SQL_FUNC_DAY            ("day(?)"),                 // Oracle: extract(day from ?)
    SQL_FUNC_MONTH          ("month(?)"),               // Oracle: extract(month from ?)
    SQL_FUNC_YEAR           ("year(?)"),                // Oracle: extract(year from ?)

    // Aggregation
    SQL_FUNC_SUM            ("sum(?)", true),
    SQL_FUNC_MAX            ("max(?)", true),
    SQL_FUNC_MIN            ("min(?)", true),
    SQL_FUNC_AVG            ("avg(?)", true),

    // Decode
    SQL_FUNC_DECODE         ("case ? {0} end"),         // Oracle: decode(? {0})
    SQL_FUNC_DECODE_SEP     (" "),                      // Oracle: ,
    SQL_FUNC_DECODE_PART    ("when {0} then {1}"),      // Oracle: {0}, {1}
    SQL_FUNC_DECODE_ELSE    ("else {0}");               // Oracle: {0}

    private final String sqlDefault;
    private final boolean aggregate;
    
    private DBSqlPhrase(String sqlDefault, boolean aggregate)
    {
        this.sqlDefault = sqlDefault;
        this.aggregate = aggregate;
    }
    
    private DBSqlPhrase(String sqlDefault)
    {
        this(sqlDefault, false);
    }

    public String getSqlDefault()
    {
        return sqlDefault;
    }

    public boolean isAggregate()
    {
        return aggregate;
    }
}
