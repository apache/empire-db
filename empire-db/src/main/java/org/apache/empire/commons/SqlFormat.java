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
package org.apache.empire.commons;

import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.InvalidValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SqlFormat
 * 
 * This class pretty-formats an SQL command.
 *  
 * It may be either overwritten or replaced by a custom implementation
 * by setting a formatter that implements the SqlFormatter interface
 */
public class SqlFormat
{
    protected static final Logger log = LoggerFactory.getLogger(SqlFormat.class);

    /**
     * SqlFormatter interface for customization
     */
    public interface SqlFormatter
    {
        String format(String sqlCmd, Object[] paramValues, DataType[] paramTypes);
    }
    
    /**
     * The formatter interface used for SQL formatting
     */
    private static SqlFormatter formatter = new DefaultSqlFormatter();
    
    /**
     * Returns the current SLQ Formatter
     * @return the current SLQ Formatter
     */
    public static SqlFormatter getFormatter()
    {
        return formatter;
    }

    /**
     * Replace the current SqlFormatter
     * @param formatter the SQL formatter
     */
    public static void setFormatter(SqlFormatter formatter)
    {
        SqlFormat.formatter = formatter;
    }

    /**
     * DefaultSqlFormatter
     * Uses the default implementation to format SQL
     */
    public static final class DefaultSqlFormatter implements SqlFormatter
    {
        @Override
        public String format(String sqlCmd, Object[] paramValues, DataType[] paramTypes)
        {
            return new SqlFormat(sqlCmd, paramValues, paramTypes).build();
        }
    }
    
    /**
     * Formats an sqlCmd using the current SQL formatter
     * @param sqlCmd the SQL command
     * @param paramValues the param values to replace query params '?' (optional)
     * @param paramTypes the param types for the given param values (optional)
     * @return the formatted SQL
     */
    public static String format(String sqlCmd, Object[] paramValues, DataType[] paramTypes)
    {
        if (paramTypes==null && paramValues!=null) 
        {   paramTypes = new DataType[paramValues.length];
            for (int i=0; i<paramValues.length; i++)
            {   // ignore null
                if (paramValues[i]==null)
                    continue;
                paramTypes[i] = DataType.fromJavaType(paramValues[i].getClass());
            }
        } else if (paramValues!=null && paramValues.length!=paramTypes.length) {
            // Invalid paramTypes array. Size must match paramValues
            throw new InvalidArgumentException("paramTypes", paramTypes);
        }
        // format now
        return getFormatter().format(sqlCmd, paramValues, paramTypes);
    }

    /**
     * Formats an sqlCmd using the current SQL formatter
     * @param sqlCmd the SQL command
     * @param paramValues the param values to replace query params '?' (optional)
     * @return the formatted SQL
     */
    public static String format(String sqlCmd, Object[] paramValues)
    {
        return format(sqlCmd, paramValues, null);
    }

    /**
     * Formats an sqlCmd using the current SQL formatter
     * @param sqlCmd the SQL command
     * @return the formatted SQL
     */
    public static String format(String sqlCmd)
    {
        return format(sqlCmd, null, null);
    }
    
    /**
     * SQL KeyWord
     */
    public enum KeyWord 
    {
        SELECT(true, true, false),
        UPDATE(true, false, false),
        SET(false, true, true),
        INSERT_INTO(true, false, false),
        INSERT(true, false, false),
        VALUES(true, false, false),
        DELETE_FROM(true, false, false),
        FROM(true, false, false),
        INNER_JOIN(true, false, true),
        LEFT_JOIN(true, false, true),
        RIGHT_JOIN(true, false, true),
        FULL_JOIN(true, false, true),
        UNION(false, false, true),
        WHERE(true, false, false),
        GROUP_BY(true, true, false),
        HAVING(true, false, false),
        ORDER_BY(true, true, false),
        AND(true, false, true), // special handling depending on context
        BETWEEN(false, false, false), // context for AND
        CASE_WHEN(false, false, false), // context for AND
        ON(false, false, true), // for JOIN ... ON
        CREATE_VIEW(true, false, false), // context for AS
        AS(true, true, false), // only after CREATE VIEW
        // Oracle specific
        MERGE_INTO(true, false, false),
        USING(true, false, false),
        WHEN_MATCHED_THEN(true, true, false); 
        
        private final char[] chars;
        private boolean breakBefore;
        private boolean breakAfter;
        private boolean indent;

        private KeyWord(boolean breakBefore, boolean breakAfter, boolean indent)
        {
            chars = name().toCharArray();
            for (int i=0; i<chars.length; i++)
                if (chars[i]=='_')
                    chars[i]=' ';
            this.breakBefore = breakBefore;
            this.breakAfter = breakAfter;
            this.indent = indent;
        }
        
        public int length()
        {
            return chars.length;
        }

        public boolean isBreakBefore()
        {
            return breakBefore;
        }

        public void setBreakBefore(boolean breakBefore)
        {
            this.breakBefore = breakBefore;
        }

        public boolean isBreakAfter()
        {
            return breakAfter;
        }

        public void setBreakAfter(boolean breakAfter)
        {
            this.breakAfter = breakAfter;
        }

        public boolean isIndent()
        {
            return indent;
        }

        public void setIndent(boolean indent)
        {
            this.indent = indent;
        }

        public boolean equals(char[] s, int pos)
        {
            for (int i=0; i<chars.length; i++)
                if (chars[i]!=Character.toUpperCase(s[pos+i]))
                    return false;
            // whole word?
            pos+=chars.length;
            if (pos<s.length && s[pos]!=' ')
                return false; // nope
            // found
            return true;
        }
    }
    
    /*
     * Members
     */

    // Constants
    private static final String NEWLINE = System.lineSeparator();
    private static final String INS_SINGLE = "  ";
    private static final String INS_DOUBLE = "    ";
    
    private final char[] cmd;
    private final Object[] paramValues; 
    private final DataType[] paramTypes;
    private final StringBuilder sql;
    private final int len;
    private int pos = 0;
    private int level = 0;
    private int paramIndex = 0;
    private boolean newLine;
    
    /**
     * SqlFormat Constructor
     * @param sqlCmd the SQL command string
     * @param paramValues the parameter values
     * @param paramTypes the parameter types
     */
    protected SqlFormat(String sqlCmd, Object[] paramValues, DataType[] paramTypes)
    {
        this.cmd = prepareCmd(sqlCmd);
        this.paramValues = paramValues;
        this.paramTypes = paramTypes;
        this.sql = new StringBuilder(cmd.length+cmd.length/2);
        this.len = cmd.length;
    }
    
    /**
     * Builds the formatted SQL
     * @return the formatted SQL
     */
    public String build()
    {
        int beg = 0;
        newLine = false;
        KeyWord pkw = null; // previous keyword
        for (char pc=' '; pos<len; pos++)
        {
            char c = cmd[pos];
            if (c=='(')
            {   // new block
                if (pos==beg && allowBlock(pkw))
                    beg = append(beg, pos+1, true, true, level++);
                else
                    skipParenthesis(); // find the end of parenthesis
            }
            else if (c==')' && pos>beg)
            {   // end of block
                beg = append(beg, pos--, true, true, level--);
            }
            else if (c==',')
            {   // end of column
                beg = append(beg, ++pos, true, true, level);
            }
            else if (c=='\'')
            {   // end of column
                skipLiteral();
            }
            else if (c=='*' && pc=='/')
            {   // comment, skip till the end
                for (pos++; pos<len-1; pos++)
                    if (cmd[pos]=='*' && cmd[++pos]=='/')
                        break;
            }
            else if (beg==pos || pc==' ')
            {   // find keyword
                KeyWord kw = findKeyWord(pos);
                if (kw!=null)
                {   // a keyword was found
                    if (kw == KeyWord.BETWEEN || kw == KeyWord.CASE_WHEN ||
                        (kw == KeyWord.AND && pkw == KeyWord.BETWEEN)) // AND belongs to BETWEEN
                    {   // ignore this keyword
                        pos+=kw.length();
                    }
                    else if ((kw == KeyWord.AND && pkw == KeyWord.CASE_WHEN) ||
                             (kw == KeyWord.AS && pkw != KeyWord.CREATE_VIEW)) // AS not following CREATE VIEW
                    {   // ignore this keyword and maintain previous
                        pos+=kw.length();
                        kw = pkw; // maintain previous
                    }
                    else if (pos==beg)
                    {   // append the keyword
                        if (kw == KeyWord.AND)
                        {   // double insert if the previous keyword was indented (through level+1)
                            boolean indent = pkw.isIndent() ? false : kw.isIndent();
                            beg = append(beg, pos+=kw.length(), indent, kw.isBreakAfter(), pkw.isIndent() ? level+1 : level);
                            kw = pkw; // maintain previous
                        }
                        else
                            beg = append(beg, pos+=kw.length(), kw.isIndent(), kw.isBreakAfter(), level);
                    }
                    else
                    {   // append everything before the keyword
                        beg = append(beg, --pos, true, kw.isBreakBefore(), level);
                        kw = pkw; // maintain previous
                    }
                    pkw = kw; // set previous
                }
            }
            pc = c; // set previous
        }
        // the rest
        if (beg<len)
            append(beg, len, true, true, level);
        // done
        return sql.toString();
    }

    /*
     * implementation functions
     */
    
    protected char[] prepareCmd(String sqlCmd)
    {
        // replace all CRLF with SPACE
        sqlCmd = StringUtils.replaceAll(sqlCmd, "\r\n", " ").trim();
        // replace chars
        sqlCmd=sqlCmd.replace('\n', ' '); // remaining LF
        sqlCmd=sqlCmd.replace('\t', ' '); // Tab
        // combine spaces
        sqlCmd = combineBlanks(sqlCmd);
        return sqlCmd.toCharArray();
    }

    protected boolean allowBlock(KeyWord kw)
    {
        return (kw != KeyWord.SELECT && kw != KeyWord.WHERE);
    }
    
    protected int append(int beg, int end, boolean indent, boolean newLineAfter, int lev)
    {
        if (newLine)
        {   // insert new line
            sql.append(NEWLINE);
            for (int i=0; i<lev; i++)
                sql.append(INS_DOUBLE);
            if (indent)
                sql.append(INS_SINGLE);
        }
        else if (sql.length()>0)
            sql.append(" ");
        if (beg<end)
        {   // replace params
            while (paramValues!=null)
            {   // find next
                int idx = findNext('?', beg);
                if (idx<0 || idx>=end)
                    break;
                // found
                sql.append(cmd, beg, idx-beg);
                sql.append(nextParamValue());
                beg = idx+1;
            }
            // append the rest
            if (beg<end)
                sql.append(cmd, beg, end-beg);
        }
        // skip blanks
        while (end<len && cmd[end]==' ') end++;
        // reset new line
        newLine = newLineAfter;
        return end;
    }
    
    protected String nextParamValue()
    {
        if (paramIndex>=paramValues.length)
            throw new InvalidValueException(paramIndex);
        DataType type = paramTypes[paramIndex];
        Object value = paramValues[paramIndex++]; 
        if (value==null)
            return "null";
        if (type.isBoolean())
            return ObjectUtils.getBoolean(value) ? "1" : "0";
        if (type.isDate())
            return ObjectUtils.formatDate(ObjectUtils.getDate(value), (type!=DataType.DATE));
        if (type.isText())
            return StringUtils.concat("'", ObjectUtils.getString(value), "'"); 
        // convert to string
        return ObjectUtils.getString(value);
    }
    
    protected KeyWord findKeyWord(int pos)
    {
        for (KeyWord kw : KeyWord.values()) {
            if (kw.equals(cmd, pos))
                return kw;
        }
        return null;
    }

    protected void skipLiteral()
    {
        if (cmd[pos]=='\'')
            for (pos++; (pos<len && cmd[pos]!='\'');)
                pos++; 
    }

    protected void skipParenthesis()
    {
        int p=1;
        while (pos<len) 
        {   char c = cmd[++pos];
            if (c=='\'')
                skipLiteral();
            if (c==')' && (--p)==0)
                break;
            if (c=='(')
                p++;
        }
    }
    
    /*
     * helpers
     */

    protected final int findNext(char find, int from)
    {
        for (int i=from; i<len; i++) {
            if (cmd[i]=='\'')
                for (i++; (i<len && cmd[i]!='\'');) i++; // skip literal
            if (cmd[i]==find)
                return i;
        }
        return -1;
    }

    protected final int findNext(String s, String find, int from)
    {
        int end = s.length();
        for (int i=from, j=0; i<end; i++) 
        { 
            char c = s.charAt(i);
            if (c=='\'') {
                for (i++; (i<end && s.charAt(i)!='\'');) i++; // skip literal
            }
            if (c==find.charAt(j)) {
                if ((++j)==find.length())
                    return ++i-j; // found
            }
            else j=0; 
        }
        return -1;
    }
    
    protected final String combineBlanks(String s)
    {
        int end = findNext(s, "  ", 0);
        if (end<0)
            return s; // nothing to do
        // process
        int beg = 0;
        StringBuilder b = new StringBuilder();
        char[] chars = s.toCharArray();
        while (end>0)
        {
            b.append(chars, beg, (++end)-beg);
            beg = ++end;
            while (chars[beg]==' ') beg++;
            end = findNext(s, "  ", beg);
        }
        if (beg<chars.length)
            b.append(chars, beg, chars.length-beg);
        // done
        return b.toString();
    }
}
