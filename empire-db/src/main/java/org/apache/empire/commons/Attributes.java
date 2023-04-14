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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.empire.exceptions.InvalidArgumentException;
import org.w3c.dom.Element;

/**
 * This class holds a map of objects which are identified by a case insensitive key string.
 * 
 */
public class Attributes extends AbstractSet<Attributes.Attribute> implements Cloneable, Serializable 
{
    private static final long serialVersionUID = 1L;
    
	public static final class Attribute implements Serializable 
	{
        private static final long serialVersionUID = 1L;
        
	    private String name;
        private String namespace;
	    private Object value;
	    
	    public Attribute(String name, Object value, String namespace)
        {   // check name
	        if (name==null || (this.name=name.trim()).length()==0)
	            throw new InvalidArgumentException("name", name);
	        this.namespace = namespace;
            this.value = value;
        }
        public Attribute(String name, Object value)
        {   // check name
            this(name, value, null);
        }
        public String getName()
        {
            return name;
        }
        public String getNamespace()
        {
            return namespace;
        }
        public Object getValue()
        {
            return value;
        }
        public void setValue(Object value)
        {
            this.value = value;
        }
        @Override
        public boolean equals(Object other)
        {   // check namespace
            if (namespace!=null)
            {
                String otherNs = (other instanceof Attribute) ? ((Attribute)other).getNamespace() : null;
                if (!namespace.equalsIgnoreCase(otherNs))
                    return false; // Namespace don't match
            }
            // check name
            String otherName = (other instanceof Attribute) ? ((Attribute)other).getName() : other.toString();
            return name.equalsIgnoreCase(otherName);
        }
	}
	
	private static final class EmptyIterator implements Iterator<Attribute>
	{
        @Override
        public boolean hasNext()  {
            return false;
        }
        @Override
        public Attribute next() {
            return null;
        }
        @Override
        public void remove() {
        }
	}

    // *Deprecated* private static final long serialVersionUID = 1L;
    
	private static final EmptyIterator emptyIterator = new EmptyIterator(); 
	
	protected ArrayList<Attributes.Attribute> attributes = null;
	
	protected ArrayList<Attributes.Attribute> list()
	{
	    if (attributes==null)
	        attributes = new ArrayList<Attributes.Attribute>(2);
	    return attributes;
	}
    
    public Attributes()
    {
    }
    
    public Attributes(int size)
    {
        if (size>0)
            attributes = new ArrayList<Attributes.Attribute>(size);
    }

    @Override
    public Attributes clone()
    {
         Attributes clone = new Attributes();
         if (attributes!=null)
             clone.attributes = new ArrayList<Attributes.Attribute>(attributes);
         return clone;
    }

    @Override
    public Iterator<Attribute> iterator()
    {
        return (attributes!=null ? list().iterator() : emptyIterator);
    }

    @Override
    public int size()
    {
        return (attributes!=null ? attributes.size() : 0);
    }
    
    @Override
    public void clear()
    {
        if (attributes!=null)
            attributes.clear();
    }
    
    @Override
    public boolean contains(Object item)
    {
        if (item==null)
            return false;
        // find
        String name = (item instanceof Attribute) ? ((Attribute)item).getName() : item.toString();
        return (indexOf(name)>=0);
    }

    public final boolean contains(String name)
    {
        return contains((Object)name); 
    }
    
    @Override
    public boolean add(Attribute a)
    {
        set(a.getName(), a.getValue());
        return true;
    }

    @Override
    public boolean remove(Object item)
    {
        if (item==null)
            return false;
        // find
        String name = (item instanceof Attribute) ? ((Attribute)item).getName() : item.toString();
        int i = indexOf(name);
        if (i<0)
            return false;
        // remove
        list().remove(i);
        return true;
    }
    
    public final boolean remove(String name)
    {
        return remove((Object)name);
    }

    /**
     * @param name the attribute name
     * @return the attribute value
     */
    public int indexOf(String name)
    {   // Find an Entry
        if (attributes==null)
            return -1;
        // Find it now
        int size = attributes.size();
        for (int i = 0; i < size; i++)
        { // Search List for Index
            Attribute a = attributes.get(i);
            if (a.getName().equalsIgnoreCase(name))
                return i;
        }
        return -1;
    }

	/**
     * @param name the attribute name
     * @return the attribute value
     */
    public Object get(String name)
    {   // check name
        if (attributes==null || name==null || name.length()==0)
            return null;
        // find
        int i = indexOf(name);
        if (i<0)
            return null; // Not set
        // found
        return list().get(i).getValue();
    }

    /**
     * @param name the attribute
     * @param value The attribute to set
     * @return the attribute
     */
    public Attribute set(String name, Object value)
    {
        // check name
        if (name==null || name.length()==0)
            return null;
        // Find
        int i = indexOf(name);
        if (i<0)
        {   // new attribute
            Attribute a = new Attribute(name, value); 
            list().add(a);
            return a;
        }
        else
        {   // existing attribute
            Attribute a = list().get(i);
            a.setValue(value);
            return a;
        }
    }

    @Override
    public Object[] toArray()
    {
        return (isEmpty() ? new Object[0] : list().toArray());
    }

    @Override
    public String toString()
    {
        if (isEmpty())
            return "{ /*empty*/ }";
        // create string
        StringBuilder b = new StringBuilder();
        b.append("{");
        boolean first = true;
        for (Attribute a : list())
        {
            b.append((first) ? "\"" : ",\r\n \"");
            String ns = a.getNamespace();
            if (StringUtils.isNotEmpty(ns))
            {   // append namespace
                b.append(ns);
                b.append(":");
            }
            b.append(a.getName());
            b.append("\":\"");
            b.append(a.getValue());
            b.append("\"");
            first = false;
        }    
        b.append("}");
        return b.toString();
    }
    
    /**
     * @param element the XMLElement to which to append the options
     * @param flags options (currently unused)
     */
    public void addXml(Element element, long flags)
    {
        if (isEmpty())
            return;
        // add All Options
    	for (Attribute a : list())
    	{
    	    Object value = a.getValue();
    	    if (value==null)
    	        continue;
    	    // set xml attribute
            String ns = a.getNamespace();
            if (StringUtils.isNotEmpty(ns))
                element.setAttributeNS(ns, a.getName(), String.valueOf(value));
            else
                element.setAttribute(a.getName(), String.valueOf(value));
    	}
    }
}
