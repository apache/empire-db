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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBReader;
import org.apache.empire.exceptions.BeanPropertySetException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.ItemNotFoundException;

public class EmpireReader extends DBReader
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void getBeanProperty(Object bean, String property, Object value)
    {
        try
        {
            if (bean == null)
                throw new InvalidArgumentException("bean", bean);
            if (StringUtils.isEmpty(property))
                throw new InvalidArgumentException("property", property);

            // Get descriptor
            PropertyDescriptor descriptor = BeanUtilsBean.getInstance().getPropertyUtils().getPropertyDescriptor(bean, property);
            if (descriptor == null)
            {
                return; // Skip this property setter
            }
            // Check enum
            Class<?> type = descriptor.getPropertyType();
            if (type.isEnum())
            {
                // Enum<?> ev = Enum.valueOf(type, value);
                boolean found = false;
                Enum<?>[] items = (Enum[]) type.getEnumConstants();
                for (int i = 0; i < items.length; i++)
                {
                    Enum<?> item = items[i];
                    if (ObjectUtils.compareEqual(item.name(), value))
                    {
                        value = item;
                        found = true;
                        break;
                    }
                }
                // Enumeration value not found
                if (!found)
                    throw new ItemNotFoundException(value);
            }
            
            // Set Property Value
            if (value != null)
            { // Bean utils will convert if necessary
                BeanUtils.setProperty(bean, property, value);
            }
            else
            { // Don't convert, just set
                PropertyUtils.setProperty(bean, property, null);
            }
        }
        catch (IllegalAccessException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
        catch (InvocationTargetException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
        catch (NoSuchMethodException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
        catch (NullPointerException e)
        {   log.error(bean.getClass().getName() + ": unable to set property '" + property + "'");
            throw new BeanPropertySetException(bean, property, e);
        }
    }

}
