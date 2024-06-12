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
package org.apache.empire.jakarta.app;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Field;
import java.util.Iterator;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.faces.context.FacesContext;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.data.RecordData;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBELResolver extends ELResolver
{
    private static final Logger log = LoggerFactory.getLogger(DBELResolver.class);
    
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        if (base==null)
            log.warn("DBELResolver:getCommonPropertyType is not implemented!");
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property)
    {
        if ((base instanceof DBRowSet) ||
            (base instanceof DBDatabase) ||
            (base==null && property.equals("db")))
            log.warn("DBELResolver:getType is not implemented!");
        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1)
    {
        log.warn("DBELResolver:getFeatureDescriptors is not implemented!");
        return null;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property)
    {
        // Resolve database, table/view or column
        if (base instanceof DBRowSet)
        {   // Find matching column
            String   name = StringUtils.toString(property);
            DBColumnExpr column = ((DBRowSet)base).getColumn(name);
            if (column==null)
                column = findExpressionField(base, name); 
            if (column!=null)
                context.setPropertyResolved(true); 
            else
                log.error("ELResolver error: Column '{}' cannot be resolved for table/view '{}'.", name.toUpperCase(), ((DBRowSet)base).getName());
            // done
            return column;
        }
        else if (base instanceof DBDatabase)
        {   // Lookup RowSet
            String   name = StringUtils.toString(property);
            DBRowSet rset = ((DBDatabase)base).getRowSet(name);
            if (rset!=null)
                context.setPropertyResolved(true);
            else
                log.error("ELResolver error: Table/View '{}' cannot be resolved.", name.toUpperCase());
            // done
            return rset;
        }
        else if (base instanceof RecordData)
        {   // Lookup RowSet           
            String field= StringUtils.toString(property);
            // field must start with a capital Letter
            if (field==null || field.length()==0 || field.charAt(0)<'A' || field.charAt(0)>'Z')
                return null;
            // try to find field
            int index = ((RecordData)base).getFieldIndex(field);
            if (index<0)
            {   // not a field, it may be a property
                log.warn("ELResolver warning: field '{}' not found in record .", field);
                // not resolved, continue search
                return null; 
            }
            // Found! Return field value.
            context.setPropertyResolved(true);
            return ((RecordData)base).getValue(index);
        }
        else if (base==null)
        {   // LookupDatabase
            String name = StringUtils.toString(property);
            DBDatabase db = DBDatabase.findByIdentifier(name);
            if (db!=null)
                context.setPropertyResolved(true);
            // done
            return db;
        }
        return null;
    }
    
    public static DBColumnExpr findExpressionField(Object rowset, String property)
    {
        Class<?> c = rowset.getClass();
        try
        {   // Find a matching field name
            Field f = c.getField(property);
            if (f==null)
                return null;
            Object v = f.get(rowset);
            if (v==null)
            {   // invalid data type 
                log.error("ELResolver error: Field '{}.{}' is null.", c.getSimpleName(), property);
                return null;
            }    
            if (!(v instanceof DBColumnExpr))
            {   // invalid data type 
                log.error("ELResolver error: Field '{}.{}' is not a DBColumnExpr.", c.getSimpleName(), property);
                return null;
            }    
            return ((DBColumnExpr)v);
        }
        catch (SecurityException e)
        {
            log.error("ELResolver error: Unable to access field "+c.getSimpleName()+"."+property, e);
            return null;
        }
        catch (NoSuchFieldException e)
        {
            return null;
        }
        catch (IllegalArgumentException e)
        {
            log.error("ELResolver error: Unable to access field "+c.getSimpleName()+"."+property, e);
            return null;
        }
        catch (IllegalAccessException e)
        {
            log.error("ELResolver error: Unable to access field "+c.getSimpleName()+"."+property, e);
            return null;
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
    {
        // is it our's?
        if ((base instanceof DBRowSet) ||
            (base instanceof DBDatabase) ||
            (base==null && property.equals("db")))
        {   // read only
            log.info("ELResolver: property {} is read only.", property);
            return true;
        }
        // not our business
        return false;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value)
    {
        if (isReadOnly(context, base, property))
        {
            RuntimeException e = new NotSupportedException(this, "setValue");
            FacesContext.getCurrentInstance().getExternalContext().log(e.getMessage(), e);
            throw e;
        }    
    }

}
