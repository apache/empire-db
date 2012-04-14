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
package org.apache.empire.jsf2.app;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.faces.context.FacesContext;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBColumn;
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
            DBColumn column = ((DBRowSet)base).getColumn(name);
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
        else if (base==null)
        {   // LookupDatabase
            String name = StringUtils.toString(property);
            DBDatabase db = DBDatabase.findById(name);
            if (db!=null)
                context.setPropertyResolved(true);
            // done
            return db;
        }
        return null;
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
