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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.empire.data.DataType;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.exceptions.UnspecifiedErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBCmdParamList
 * This class handles command parameters for Prepared Statements
 */
public class DBCmdParamList implements DBCmdParams
{
    protected static final Logger log = LoggerFactory.getLogger(DBCmdParamList.class);

    protected ArrayList<DBCmdParam> cmdParams;
    
    private int paramUsageCount = 0;

    public DBCmdParamList()
    {
        cmdParams = null;
    }

    public DBCmdParamList(int size)
    {
        cmdParams = (size>0 ? new ArrayList<>(size) : null);
    }
    
    @Override
    public boolean isEmpty()
    {
        return (cmdParams==null || cmdParams.isEmpty());
    }
    
    @Override
    public int size()
    {
        return (cmdParams==null ? 0 : cmdParams.size());
    }

    @Override
    public Iterator<DBCmdParam> iterator()
    {
        if (cmdParams==null)
            throw new NotSupportedException(this, "iterator");
        return cmdParams.iterator();
    }
    
    public void clear(int capacity)
    {
        paramUsageCount = 0;
        if (capacity>0)
            cmdParams= new ArrayList<DBCmdParam>(capacity);
        else
            cmdParams = null;
    }
    
    public void add(DBCmdParam param)
    {
        if (cmdParams==null)
            cmdParams= new ArrayList<DBCmdParam>();
        // Create and add the parameter to the parameter list 
        cmdParams.add(param);
    }
    
    public void remove(DBCmdParam param)
    {
        if (cmdParams==null || !cmdParams.remove(param))
            log.warn("Unable to remove DBCmdParam: Param not found");
    }
    
    public Object[] getParamValues()
    {
        if (cmdParams==null || paramUsageCount==0)
            return null;
        // Check whether all parameters have been used
        if (paramUsageCount!=cmdParams.size())
            log.info("DBCommand parameter count ("+String.valueOf(cmdParams.size())
                   + ") does not match parameter use count ("+String.valueOf(paramUsageCount)+")");
        // Create result array
        Object[] values = new Object[paramUsageCount];
        for (int i=0; i<values.length; i++)
            values[i]=cmdParams.get(i).getValue();
        // values
        return values;
    }
    
    /**
     * internally used to reset the command param usage count.
     * Note: Only one thread my generate an SQL statement 
     */
    public void resetParamUsage(DBCommand cmd)
    {
        paramUsageCount = 0;
        if (cmdParams==null)
            return;
        // clear subquery params
        for (int i=cmdParams.size()-1; i>=0 ;i--)
            if (cmdParams.get(i).getCmd()!=cmd)
                cmdParams.remove(i);
    }
    
    /**
     * internally used to remove unused Command Params from list
     * Note: Only one thread my generate an SQL statement 
     */
    public void completeParamUsage(DBCommand cmd)
    {
        if (cmdParams==null)
            return;
        // check whether all params have been used
        if (paramUsageCount < cmdParams.size())
        {   // Remove unused parameters
            log.warn("DBCommand has {} unused Command params", cmdParams.size()-paramUsageCount);
            for (int i=cmdParams.size()-1; i>=paramUsageCount; i--)
            {   // Remove temporary params
                if (cmdParams.get(i).getCmd()!=cmd)
                    cmdParams.remove(i);
            }
        }
    }
    
    /**
     * internally used to reorder the command params to match their order of occurance
     */
    protected void notifyParamUsage(DBCmdParam param)
    {
        int index = cmdParams.indexOf(param);
        if (index<0) 
        {   // Error: parameter probably used twice in statement!
            throw new UnspecifiedErrorException("The CmdParam has not been found on this Command.");
        }
        if (index < paramUsageCount)
        {   // Warn: parameter used twice in statement!
            log.debug("The DBCmdParam already been used. Adding a temporary copy");
            cmdParams.add(paramUsageCount, new DBCmdParam(null, param.getDataType(), param.getValue()));
        }
        else if (index > paramUsageCount)
        {   // Correct parameter order
            cmdParams.remove(index);
            cmdParams.add(paramUsageCount, param);
        }
        paramUsageCount++;
    }
    
    public void mergeSubqueryParams(DBCmdParams subQueryParams)
    {
        if (subQueryParams==null || subQueryParams.isEmpty())
            return;
        // Subquery has parameters
        if (cmdParams==null)
            cmdParams= new ArrayList<DBCmdParam>(subQueryParams.size());
        for (DBCmdParam p : subQueryParams)
            cmdParams.add(paramUsageCount++, new DBCmdParam(null, DataType.UNKNOWN, p.getValue()));
    }

    /*
    protected void mergeSubqueryParamsObsolete(Object[] subQueryParams)
    {
        if (subQueryParams==null || subQueryParams.length==0)
            return;
        // Subquery has parameters
        if (cmdParams==null)
            cmdParams= new ArrayList<DBCmdParam>(subQueryParams.length);
        for (int p=0; p<subQueryParams.length; p++)
            cmdParams.add(paramUsageCount++, new DBCmdParam(null, DataType.UNKNOWN, subQueryParams[p]));
        log.warn("mergeSubqueryParamsObsolete");
    }
    
    public void addJoin(DBSQLBuilder sql, DBJoinExpr join, long context, int whichParams)
    {
        // remember insert pos
        int paramInsertPos = paramUsageCount;
        // now add the join
        join.addSQL(sql, context);
        // Merge subquery params
        Object[] subQueryParams = join.getSubqueryParams(whichParams);
        if (subQueryParams!=null)
        {
            if (paramInsertPos == paramUsageCount)
                mergeSubqueryParamsObsolete(subQueryParams);
            else
            {   // Some Params have been used in additional Join constraints
                int tempCounter = paramUsageCount;
                paramUsageCount = paramInsertPos;
                mergeSubqueryParamsObsolete(subQueryParams);
                int insertCount = (paramUsageCount - paramInsertPos);
                paramUsageCount = tempCounter + insertCount;
            }
        }
    }
    */
    
}
