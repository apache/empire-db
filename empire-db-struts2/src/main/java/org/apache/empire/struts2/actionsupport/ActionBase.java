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
package org.apache.empire.struts2.actionsupport;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.empire.commons.ErrorInfo;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.Column;
import org.apache.empire.data.Record;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.struts2.action.ActionItemProperty;
import org.apache.empire.struts2.action.RequestParamProvider;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.LocaleProvider;


public abstract class ActionBase
    implements ActionItemProperty, RequestParamProvider, LocaleProvider
{
    // Logger
    protected static Logger log = LoggerFactory.getLogger(ActionBase.class);
 
    private static final char KEY_SEP_CHAR  = '/';
    private static final char NEW_FLAG_CHAR = '*';
    
    // ------- Must Implements -------
    
    protected abstract Connection getConnection(); 

    protected abstract void addFieldError(String name, Column column, ErrorInfo error);

    public abstract int getListPageSize();
    
    // ------- ActionObject accessors -------

    protected String getActionObjectName(ActionContext context, String name)
    {
        ActionProxy proxy = getActionProxy(context);
        if (proxy==null)
            return null;
        return proxy.getActionName() + "." + name;
    }
    
    protected Object getActionObject(String name)
    {
        // Get the object key
        ActionContext context = ActionContext.getContext();
        String key = getActionObjectName(context, name);
        if (key==null)
            return null;
        // Get the object from the session
        return context.getSession().get(key);
    }
    
    protected void putActionObject(String name, Object item)
    {   // Put object
        ActionContext context = ActionContext.getContext();
        String key = getActionObjectName(context, name);
        if (key!=null)
            context.getSession().put(key, item);
    }
    
    protected void removeActionObject(String name)
    {   // Clear object
        ActionContext context = ActionContext.getContext();
        String key = getActionObjectName(context, name);
        if (key!=null)
            context.getSession().remove(key);
    }
    
    // ------- Action Bean storage -------

    private ActionProxy getActionProxy(ActionContext context)
    {
        ActionInvocation invocation = context.getActionInvocation();
        if (invocation==null)
        {
            log.error("Action Invocation cannot be obtained. Calling from action constructor?");
            return null;
        }
        ActionProxy proxy = invocation.getProxy();
        if (proxy==null)
        {
            log.error("ActionProxy cannot be obtained. Calling from action constructor?");
            return null;
        }
        return proxy;
    }
    
    protected String getActionBeanName(ActionContext context, Class objClass, String ownerProperty)
    {
        ActionProxy proxy = getActionProxy(context);
        if (proxy==null)
            return null;
        if (ownerProperty==null)
            return proxy.getActionName()+ "." + objClass.getName();
        // Default
        return proxy.getActionName()+ "." + ownerProperty + "." + objClass.getName();
    }
    
    public Object getActionBean(Class objClass, boolean create, String ownerProperty)
    {
        if (objClass==null)
            return null;
        // get the object key
        ActionContext context = ActionContext.getContext();
        String key = getActionBeanName(context, objClass, ownerProperty);
        if (key==null)
            return null;
        // get the object from the session
        Object obj = context.getSession().get(key);
        if (obj==null && create)
        {   try {
                obj = objClass.newInstance();
                context.getSession().put(key, obj);
            } catch(Exception e) {
                log.error("Cannot create Instance of type " + objClass.getName(), e);
            }
        }
        return obj;
    }

    public Object getActionBean(Class objClass, boolean create)
    {
        return getActionBean(objClass, create, null);
    }
    
    public void putActionBean(Object obj, String ownerProperty)
    {
        if (obj==null || obj instanceof String || obj.getClass().isPrimitive() || obj.getClass().isArray())
        {   // Error
            log.error("Unable to store object on session. Object is null, a primitive type or a string!");
            return;
        }    
        // Put object
        ActionContext context = ActionContext.getContext();
        String key = getActionBeanName(context, obj.getClass(), ownerProperty);
        if (key!=null)
            context.getSession().put(key, obj);
    }

    public void putActionBean(Object obj)
    {
        putActionBean(obj, null);
    }
    
    public void removeActionBean(Class objClass, String propertyName)
    {
        if (objClass==null)
            return;
        ActionContext context = ActionContext.getContext();
        String key = getActionBeanName(context, objClass, propertyName);
        if (key!=null)
            context.getSession().remove(key);
    }

    public void removeActionBean(Class objClass)
    {
        removeActionBean(objClass, null);
    }
    
    // ------- Record key conversion helpers -------

    /**
     * this method assembles all key values to a combined string
     * The key parts will be separated by forward slashes (KEY_SEP_CHAR)
     * thus the key parts must not contain forward slashes.
     * Additionally the functions adds an asterisk if the record is new
     * i.e. has not yet been inserted into the database
     * 
     * @param record the record for which to create a key string
     * @return the record key string
     */
    public String getRecordKeyString(Record record)
    {
        if (record.isValid()==false)
            return null; // not valid
        // Get Key Columns
        Column[] keyCols = record.getKeyColumns();
        if (keyCols==null)
            return null;
        // Get Values
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<keyCols.length; i++)
        {
            if (i>0) buf.append(KEY_SEP_CHAR);
            buf.append( StringUtils.valueOf(record.getValue(keyCols[i]) ));
        }
        // is new record
        if (record.isNew())
            buf.append(NEW_FLAG_CHAR);
        // Convert to String
        return buf.toString();
    }
    
    /**
     * this method assembles all key values to a combined string
     * The key parts will be separated by forward slashes (KEY_SEP_CHAR)
     * thus the key parts must not contain forward slashes.
     * Additionally the functions adds an asterisk if the record is new
     * i.e. has not yet been inserted into the database
     * 
     * @param key the key values of the record
     * @param isNew flag indicating wether or not the record is a new record
     * @return the record key string
     */
    public String getRecordKeyString(Object[] key, boolean isNew)
    {
        // Get Values
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<key.length; i++)
        {
            if (i>0) buf.append(KEY_SEP_CHAR);
            buf.append( StringUtils.valueOf(key[i]) );
        }
        // is new record
        if (isNew)
            buf.append(NEW_FLAG_CHAR);
        // Convert to String
        return buf.toString();
    }
    
    /**
     * returns a DBColumnExpr that assembles a key for the given rowset/ table
     * this method should be used when selecting the id column of a table whith a non simple primary key
     * i.e. a key which consists of more than one column
     * 
     * @param rowset the table for which go create a record key expression
     * @param aliasName the name of the key expression in the resultset ( ... AS aliasName)
     * @return a DBColumnExpr for the DBCommand select phrase
     */
    protected static DBColumnExpr getRecordKeyExpr(DBRowSet rowset, String aliasName)
    {
        if (rowset==null)
            return null; // Invalid Argument
        DBColumn[] keyCols = rowset.getKeyColumns();
        if (keyCols==null || keyCols.length==0)
            return null; // No Pimary key
        // Get Values
        DBColumnExpr expr = keyCols[0];
        for (int i=1; i<keyCols.length; i++)
        {
            expr = expr.append(KEY_SEP_CHAR).append(keyCols[i]);
        }
        if (aliasName==null || aliasName.length()==0)
            return expr;
        // Return expression
        return expr.as(aliasName);
    }
    
    /**
     * this method parses a key string and returns the result as an object array
     * 
     * @param s the key string (e.g. taken from the request)
     * @return the record key
     */
    protected Object[] getRecordKeyFromString(String s)
    {
        if (s==null || s.equals("*"))
            return null;
        // Count parts
        int count = 1;
        for (int i=0; (i=s.indexOf(KEY_SEP_CHAR, i)+1)>0; ) count++;
        // Alloc Array
        Object[] key = new Object[count];
        count = 0;
        int i = 0;
        while(true)
        {   // 
            int n=s.indexOf(KEY_SEP_CHAR, i);
            if (n>=i)
            {   // Set Key Part
                key[count] = (n>i) ? s.substring(i, n) : null;
                i = n + 1;
            }
            else
            {   // Rest
                n = s.length();
                if (n>0 && s.charAt(n-1)==NEW_FLAG_CHAR)
                    n--; // Ignore NewFlagChar
                // Copy
                key[count] = (n>i) ? s.substring(i, n) : null;
                break;
            }
            count++;
        }
        // done
        return key;
    }

    /**
     * this method checks a key string for a record new flag which indicates 
     * that the record is transient i.e. has not yet been inserted into the database
     *
     * @param s the key string (e.g. taken from the request)
     * @return true if the record is new or false otherwise
     */
    protected boolean getRecordNewFlagFromString(String s)
    {
        return (s!=null && s.length()>0 && s.charAt(s.length()-1)==NEW_FLAG_CHAR);
    }
    
}
