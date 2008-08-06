/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.commons;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.w3c.dom.Element;

/**
 * This class holds a map of objects which are identified by a case insensitive key string.
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
@SuppressWarnings("serial")
public class Attributes extends LinkedHashMap<String, Object>
{
    /**
     * @param key the attribute
     * @return the attribute value
     */
    public Object get(String key)
    {   // Check Key
        if (key==null || key.length()==0)
            return null;
        // Get
        return super.get(key.toLowerCase());
    }

    @Override
    public Object get(Object key)
    {   // Check Key
        return get((key!=null ? key.toString() : null));
    }

    @Override
    public Object put(String key, Object v)
    {
        if (key==null || key.length()==0)
            return null;
        return super.put(key.toLowerCase(), v);
    }

    /**
     * @param name the attribute
     * @param object the attribute value
     */
    public void set(String name, Object object)
    {
        this.put(name, object);
    }

    /**
     * @param element the XMLElement to which to append the options
     * @param flags options (currently unused)
     */
    public void addXml(Element element, long flags)
    {
        // add All Options
        Iterator<String> i = keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            element.setAttribute(key, String.valueOf(get(key)));
        }
    }
}
