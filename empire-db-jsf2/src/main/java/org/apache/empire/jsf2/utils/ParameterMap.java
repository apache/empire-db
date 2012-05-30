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
package org.apache.empire.jsf2.utils;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Locale;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages request parameters in a way that they cannot be analyzed and modified by the user 
 * @author doebele
 *
 */
public class ParameterMap implements Serializable
{
    private static final long             serialVersionUID = 1L;

    private static final Logger           log              = LoggerFactory.getLogger(ParameterMap.class);

    private static final SimpleDateFormat dateFormat       = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss", Locale.GERMAN);
    
    static private MessageDigest          md5              = null;
    {
        try
        {
            ParameterMap.md5 = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            ParameterMap.log.error("MessageDigest NoSuchAlgorithmException.", e);
            throw new RuntimeException(e);
        }
    }
    
    private final byte[] salt;
    
    public ParameterMap()
    {
        String dateTime = dateFormat.format(DateUtils.getTimeNow());
        salt = dateTime.getBytes();
    }
    
    public synchronized String encodeString(String valueAsString)
    {
        if (valueAsString==null)
            throw new InvalidArgumentException("valueAsString", valueAsString);
        // log
        if (log.isTraceEnabled())
            log.trace("Generating code for value {}.", valueAsString);
        // generate code
        md5.reset();
        if (salt!=null)
            md5.update(salt);
        md5.update(valueAsString.getBytes());
        byte s[] = ParameterMap.md5.digest();
        StringBuilder hash = new StringBuilder(32);
        for (int i = 0; i < s.length; i++)
        {   // add the hash part
            // String check = Integer.toHexString((0x000000ff & s[i]) | 0xffffff00).substring(6);;
            String part = Integer.toHexString(0x000000ff & s[i]);
            switch(part.length())
            {
                case 1: hash.append('0');
                case 2: hash.append(part);
                        break;
                default:
                        throw new UnexpectedReturnValueException(part, "Integer.toHexString");
                        // hash.append(part.substring(2));
            }
        }
        return hash.toString();
    }

    private Hashtable<String, String> codeMap = new Hashtable<String, String>();

    public String encodeStringWithCache(String valueAsString)
    {
        String code = codeMap.get(valueAsString);
        if (code==null)
        {   // generate code
            code = encodeString(valueAsString);
            codeMap.put(valueAsString, code);
        }
        /*
        else
        {   // Trace
            if (log.isTraceEnabled())
                log.trace("Using already generated code {} for value {}.", code, valueAsString);
        }
        */
        return code;
    }
    
    private final Hashtable<String, Hashtable<String, Object>> typeMap = new Hashtable<String, Hashtable<String, Object>>();
    
    private void putValue(String typeName, String key, Object value)
    {
        Hashtable<String, Object> map = typeMap.get(typeName);
        if (map==null)
        {   map = new Hashtable<String, Object>(1);
            typeMap.put(typeName, map);
        }
        if (key==null || value==null)
            log.warn("Key or value is null.");
        map.put(key, value);
    }

    public String put(String type, String key, boolean useCache)
    {
        // Generate id and put in map
        String id = (useCache ? encodeStringWithCache(key) : encodeString(key));
        putValue(type, id, key);
        return id;
    }

    public String put(Class<? extends Object> c, Object[] key)
    {
        // Generate id and put in map
        String ref = StringUtils.valueOf(key);
        String id = encodeString(ref);
        String type = c.getSimpleName();
        putValue(type, id, key);
        return id;
    }

    public String put(DBRowSet rowset, Object[] key)
    {
        // Generate id and put in map
        String ref = StringUtils.valueOf(key);
        String id = encodeString(ref);
        String type = rowset.getClass().getSimpleName();
        putValue(type, id, key);
        return id;
    }

    public Object get(String type, String id)
    {
        Hashtable<String, Object> map = typeMap.get(type);
        return (map!=null ? map.get(id) : null);
    }

    public Object[] get(Class<? extends Object> c, String id)
    {
        String type = c.getSimpleName();
        Hashtable<String, Object> map = typeMap.get(type);
        return (map!=null ? ((Object[])map.get(id)) : null);
    }

    public Object[] get(DBRowSet rowset, String id)
    {
        String type = rowset.getClass().getSimpleName();
        Hashtable<String, Object> map = typeMap.get(type);
        return (map!=null ? ((Object[])map.get(id)) : null);
    }

    public void clear(Class<? extends Object> c)
    {
        String type = c.getSimpleName();
        Hashtable<String, Object> map = typeMap.get(type);
        if (map!=null)
            map.clear();
    }

    public void clear(DBRowSet rowset)
    {
        String type = rowset.getClass().getSimpleName();
        Hashtable<String, Object> map = typeMap.get(type);
        if (map!=null)
            map.clear();
    }

}
