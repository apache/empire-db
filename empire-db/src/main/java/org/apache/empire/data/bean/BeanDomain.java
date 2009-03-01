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
package org.apache.empire.data.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * BeanDomain
 * This class defines Metadata for a domain of java classes.
 * Create a new class for your domain metadata and derive it from BeanDomain.
 * Then create a BeanClass subclass (preferably as a nested class) for each of your domain objects.
 * A metadata defintion consitst primarily of the domain name and a list of classes.  
 * @author Rainer
 */
public abstract class BeanDomain
{
    private final String name;
    private final List<BeanClass> classes = new ArrayList<BeanClass>();
    
    protected BeanDomain(String name)
    {
        this.name = name;
    }

    protected void addClass(BeanClass beanClass)
    {
        classes.add(beanClass);
        beanClass.domain = this;
    }
    
    public String getName()
    {
        return name;
    }
    
    public List<BeanClass> getClasses()
    {
        return classes;
    }
    
}
