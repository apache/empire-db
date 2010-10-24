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
package org.apache.empire.struts2.actionsupport;

import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.ResourceBundleTextProvider;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.TextProviderSupport;

public class TextProviderActionSupport extends TextProviderSupport
{

    private static Map<Locale, Map<String, String>> maps = new Hashtable<Locale, Map<String, String>>();
    protected static Log log = LogFactory.getLog(TextProviderActionSupport.class);
    private static final String INVALID_TEXT = "NO_TEXT";
    
    private LocaleProvider aLocaleProvider;
    private static boolean cachingEnabled=true;

    public static void setCachingEnabled(boolean enable)
    {
        cachingEnabled = enable;
        if (cachingEnabled)
            log.info(TextProviderActionSupport.class.getName() + ": Ccaching has been enabled.");
        else
            log.warn(TextProviderActionSupport.class.getName() + ": Caching is NOT enabled for Application!");
    }
    
    // ------- Singleton Constructor -------
    
    private TextProviderActionSupport(LocaleProvider provider)
    {
        
        this.aLocaleProvider = provider;
        ((ResourceBundleTextProvider) this).setLocaleProvider(provider);
    }

    //  ------- Singleton getInstance -------
    
    public static TextProvider getInstance(Class clazz, LocaleProvider provider)
    {
        TextProvider instance=new TextProviderActionSupport(provider);
        if (instance instanceof ResourceBundleTextProvider)
        {
            ((ResourceBundleTextProvider) instance).setClazz(clazz);
        }
        return instance;
    }
    
    // ------- Properties -------
    
    public static boolean isCachingEnabled()
    {
        return cachingEnabled;
    }
    
    // ------- Overrides -------
    @Override
    public String getText(String key, String defaultValue, List<Object> args)
    {
        if(key==null)
        {
            log.debug("NULL key supplied to TextProviderActionSupport, NULL is not permitted!");
            return INVALID_TEXT;
        }
        // simple texts can be stored in our Hashtable
        if(cachingEnabled && (args==null || args.size()==0)){
            Map<String, String> map = getMap(aLocaleProvider.getLocale());
            if(map.containsKey(key))
            {
                // read value from map
                return map.get(key);
            }
            else
            {
                // insert value into map
                String value = super.getText(key, defaultValue, args);
                if(value==null)
                {
                    log.debug("NULL value returned in TextProviderActionSupport, NULL is not permitted!");
                    return INVALID_TEXT;
                }
                map.put(key, value);
                return value;
            }
        }
        return super.getText(key, defaultValue, args);
    }

    // ------- Helpers -------
    private Map<String, String> getMap(Locale l)
    {
        Map<String, String> map;
        map = maps.get(l);
        if(map==null)
        {
            map=new Hashtable<String, String>();
            maps.put(l, map);
        }
        return map;
    }

}
