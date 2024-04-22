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

import java.io.Serializable;

import org.apache.empire.exceptions.InternalException;

/**
 * This class defines one possible value of a field and it's description<BR>
 * This class is used by the Options class to implement a set of options 
 * where the option value us used as the key for the set.<BR>
 * The text should only be used for display purposes e.g. to display a drop-down in a user interface.<BR>
 */
public class OptionEntry implements Cloneable, Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final Object value;
    private String text;
    private boolean active;
    
    public OptionEntry(Object value, String text, boolean active)
    {
        this.value = value;
        this.text = text;
        this.active = active;
    }
    
    public OptionEntry(Object value, String text)
    {
        this(value, text, true);
    }

    public Object getValue()
    {
        return value;
    }

    public boolean valueEquals(Object value)
    {
        return ObjectUtils.compareEqual(this.value, value);
    }
    
    @Override
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new InternalException(e);
        }
    }

    /**
     * Gets the value as string
     * 
     * @return the string representation of the value or an empty string if the value is <code>null</code>
     */
    public String getValueString()
    {
        // check null
        if (value==null)
            return StringUtils.EMPTY;
        // check enum
        if (value instanceof Enum<?>)
            return ObjectUtils.getString((Enum<?>)value);
        // convert
        return String.valueOf(value);
    }
    
    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }
    
    @Override
    public String toString()
    {
        return StringUtils.concat("{", StringUtils.toString(value, StringUtils.NULL), "=", text, "}");
    }
}
