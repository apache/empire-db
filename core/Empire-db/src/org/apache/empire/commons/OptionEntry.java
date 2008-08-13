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

/**
 * This class defines one possible value of a field and it's description<BR>
 * This class is used by the Options class to implement a set of options 
 * where the option value us used as the key for the set.<BR>
 * The text should only be used for display purposes e.g. to display a drop-down in a user interface.<BR>
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class OptionEntry
{
    private Object value;
    private String text;
    
    public OptionEntry(Object value, String text)
    {
        this.value = value;
        this.text = text;
    }

    public Object getValue()
    {
        return value;
    }

    public String getValueString()
    {   // Convenience Function   
        return (value!=null ? String.valueOf(value) : "");
    }
    
    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}
