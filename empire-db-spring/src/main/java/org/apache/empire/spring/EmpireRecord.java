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
package org.apache.empire.spring;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.empire.data.Column;
import org.apache.empire.db.DBRecord;
import org.apache.empire.exceptions.BeanPropertyGetException;

public class EmpireRecord extends DBRecord
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void setRecordValue(Column column, Object bean, String property)
    {
        try
        {
            // Get descriptor
            Object value;
            PropertyUtilsBean pub = BeanUtilsBean.getInstance().getPropertyUtils();
            PropertyDescriptor descriptor = pub.getPropertyDescriptor(bean, property);
            if (descriptor == null) {
                return; // Skip this property setter
            }
            
            // Get Property Value
            value = pub.getSimpleProperty(bean, property);

            // Check enum
            if (value instanceof Enum<?>)
                value = ((Enum<?>)value).name();
                    
            // Set the record value
            setValue( column, value ); 

        } catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        } catch (NoSuchMethodException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        } catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to get property '" + property + "'");
            throw new BeanPropertyGetException(bean, property, e);
        }
    }
}
