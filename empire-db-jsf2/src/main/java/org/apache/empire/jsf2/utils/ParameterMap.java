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
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBRowSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages request parameters in a way that they cannot be analysed and modified by the user 
 * @author doebele
 *
 */
public class ParameterMap implements Serializable
{
    private static final long             serialVersionUID = 1L;

    private static final Logger           log              = LoggerFactory.getLogger(ParameterMap.class);

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

    private final HashMap<String, HashMap<String, Object>> typeMap = new HashMap<String, HashMap<String, Object>>();
    
    private void putValue(String typeName, String key, Object value)
    {
        HashMap<String, Object> map = typeMap.get(typeName);
        if (map==null)
        {   map = new HashMap<String, Object>(1);
            typeMap.put(typeName, map);
        }
        map.put(key, value);
    }

    public String put(Class<? extends Object> c, Object[] key)
    {
        // Generate id and put in map
        String ref = StringUtils.valueOf(key);
        String id = generateId(ref);
        String type = c.getSimpleName();
        putValue(type, id, key);
        return id;
    }

    public String put(DBRowSet rowset, Object[] key)
    {
        // Generate id and put in map
        String ref = StringUtils.valueOf(key);
        String id = generateId(ref);
        String type = rowset.getClass().getSimpleName();
        putValue(type, id, key);
        return id;
    }

    public Object[] get(Class<? extends Object> c, String id)
    {
        String type = c.getSimpleName();
        HashMap<String, Object> map = typeMap.get(type);
        return (map!=null ? ((Object[])map.get(id)) : null);
    }

    public Object[] get(DBRowSet rowset, String id)
    {
        String type = rowset.getClass().getSimpleName();
        HashMap<String, Object> map = typeMap.get(type);
        return (map!=null ? ((Object[])map.get(id)) : null);
    }

    public void clear(Class<? extends Object> c)
    {
        String type = c.getSimpleName();
        HashMap<String, Object> map = typeMap.get(type);
        if (map!=null)
            map.clear();
    }

    public void clear(DBRowSet rowset)
    {
        String type = rowset.getClass().getSimpleName();
        HashMap<String, Object> map = typeMap.get(type);
        if (map!=null)
            map.clear();
    }

    private String generateId(String valueAsString)
    {
        // byte salt[] = UserBean.SALT.getBytes();
        try
        {
            String result = "";
            // md5.update(salt);
            ParameterMap.md5.update(valueAsString.getBytes("UTF8"));
            byte s[] = ParameterMap.md5.digest();
            for (int i = 0; i < s.length; i++)
            {
                // TODO DW: avoid magic numbers for readability || add few words of comments, else
                result += Integer.toHexString((0x000000ff & s[i]) | 0xffffff00).substring(6);
            }
            return result;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

}
