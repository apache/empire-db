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
package org.apache.empire.struts2.websample.web.actiontypes;

import org.apache.empire.data.Record;
import org.apache.empire.data.bean.BeanClass;
import org.apache.empire.data.bean.BeanRecordProxy;
import org.apache.empire.exceptions.ItemNotFoundException;
import org.apache.empire.exceptions.NotSupportedException;
import org.apache.empire.struts2.actionsupport.BeanActionSupport;
import org.apache.empire.struts2.actionsupport.SessionPersistence;
import org.apache.empire.struts2.exceptions.InvalidFormDataException;

import com.opensymphony.xwork2.interceptor.NoParameters;


/**
 * BeanDetailAction
 * <p>
 * This class allows using a JavaBean or Data Transfer Object (DTO) for form data processing.
 * Metadata for the Beans should be provided using the BeanClass and BeanProperty classes.
 * </p>
 * @author Rainer
 */
public abstract class BeanDetailAction<T> extends DetailAction
    implements NoParameters // set this to provide custom parameter handling
{
    protected final BeanActionSupport<T> beanSupport;
    
    /**
     * Constructs a BeanDetailAction from a BeanRecordProxy object
     * @param record the BeanRecordProxy for the bean 
     * @param persistence persistence level
     */
    public BeanDetailAction(BeanRecordProxy<T> record, SessionPersistence persistence)
    {
        beanSupport = new BeanActionSupport<T>(this, record, persistence);
    }

    /**
     * Constructs a BeanDetailAction from a BeanClass definition
     * @param beanClass the bean class defining the bean's metadata 
     * @param persistence persistence level
     */
    public BeanDetailAction(BeanClass beanClass, SessionPersistence persistence)
    {
        beanSupport = new BeanActionSupport<T>(this, beanClass, persistence);
    }
    
    /**
     * Returns the Record interface implmentation for the bean.
     * @return the Record interface implementation for the bean. 
     */
    public Record getRecord() 
    {
        return beanSupport.getRecord();
    }

    @Override
    public String doCreate() 
    {
        T bean = createBean();
        if (bean==null)
        {   // Must have an action error set!
            if (!hasActionError())
                setActionError(new NotSupportedException(beanSupport, "createBean"));
            return doCancel();
        }
        beanSupport.setData(bean);
        return INPUT;
    }

    @Override
    public String doLoad() 
    {
        // Read Record
        Object[] key = beanSupport.getActionParamKey();
        if (key!=null)
        {   // Load the bean
            T bean = loadBean(key);
            if (bean==null)
            {   // Must have an action error set!
                if (!hasActionError())
                    setActionError(new ItemNotFoundException(beanSupport.getRecordKeyString()));
                return doCancel();
            }
            beanSupport.setData(bean);
        }
        // Check if record is valid
        if (beanSupport.isValid()==false)
        {
            setActionError(new InvalidFormDataException());
            return doCancel();
        }
        // Test
        return INPUT;
    }

    @Override
    public String doDelete() 
    {
        Object[] key = null;
        if (beanSupport.hasActionKey(true))
        {
            if (beanSupport.getActionParamNewFlag()==false)
                key = beanSupport.getActionParamKey();
        }
        else
        {   // Get the bean key
            if (beanSupport.getRecord().isNew()==false)
                key = beanSupport.getRecordKeyValues();
        }
        // Delete the bean
        if (key!=null && deleteBean(key)==false)
        {   // An Error has occurred;
            return doCancel();
        }
        // Clear Data
        beanSupport.setData(null);
        return RETURN;
    }

    @Override
    public String doSave() {

        // bean Support
        if (beanSupport.isValid()==false)
        {   // Create new or reload existing item
            T bean;
            if (beanSupport.getActionParamNewFlag())
            {   // crate new Item
                bean = createBean();
            }
            else
            {   // reload existing item
                bean = loadBean(beanSupport.getActionParamKey());
            }
            if (bean==null)
            {   // Must have an action error set!
                if (!hasActionError())
                    setActionError(new ItemNotFoundException(beanSupport.getRecordKeyString()));
                return doCancel();
            }
            beanSupport.setData(bean);
        }
        else if (beanSupport.getRecord().isNew()==false) 
        {   // Check whether we have the right key
            if (!beanSupport.checkKey())
            {   // Record's don't match
                setActionError(new InvalidFormDataException());
                return doCancel();
            }
        }
        // LoadFormData 
        if (beanSupport.loadFormData()==false)
        {   // Error loading form data
            return INPUT;
        }
        
        // Save the record
        boolean isNew = beanSupport.getRecord().isNew();
        if (saveBean(beanSupport.getData(), isNew)==false)
        {   // Error saving bean
            return INPUT;
        }
        
        // Record has been saved successfully
        // beanSupport.updateSessionKey();
        beanSupport.setData(null);
        
        return RETURN;
    }
    
    // ------- overridables -------
    
    /**
     * Returns the bean for the supplied object key.
     * If an error occurs the function must set an action error and return null.
     * @return the bean object
     */
    protected abstract T createBean();
    
    /**
     * Returns the bean for the supplied object key.
     * If an error occurs the function must set an action error and return null.
     * @param key the bean's key values
     * @return the bean object
     */
    protected abstract T loadBean(Object[] key);
    
    /**
     * Saves a bean object 
     * @param bean 
     * @param isNew true the bean is a newly created object or false otherwise
     * @return true if the bean has been stored successfully or false otherwise
     */
    protected abstract boolean saveBean(T bean, boolean isNew);
    
    /**
     * Deletes a bean object
     * @param key 
     * @return true if the bean has been stored successfully or false otherwise
     */
    protected abstract boolean deleteBean(Object[] key);
    
}
