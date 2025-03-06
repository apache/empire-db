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
package org.apache.empire.jakarta.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Locale;

import org.apache.empire.commons.DateUtils;
import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.InvalidArgumentException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jakarta.pages.PageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages request parameters in a way that they cannot be analyzed and modified by the user 
 * @author doebele
 *
 */
public class ParameterMap // *Deprecated* implements Serializable
{
    // *Deprecated* private static final long serialVersionUID = 1L;

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
            throw new InternalException(e);
        }
    }
    
    private final byte[] salt;

    protected Hashtable<String, String> codeMap = new Hashtable<String, String>();

    protected final Hashtable<String, Hashtable<String, Object>> typeMap = new Hashtable<String, Hashtable<String, Object>>();
    
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

    public String encodeStringWithCache(String valueAsString)
    {
        String code = codeMap.get(valueAsString);
        if (code==null)
        {   // generate code
            code = encodeString(valueAsString);
            codeMap.put(valueAsString, code);
        }
        return code;
    }

    /**
     * gets a unique name for a given rowset  
     * @param rowset the rowset
     * @return a unique name for the given rowset
     */
    protected String getRowSetTypeName(DBRowSet rowset)
    {
        /*
         * alternatively use:
         *     rowset.getName();
         * or
         *     rowset.getFullName();
         */
        return rowset.getClass().getName();
    }

    /**
     * puts an object into the parameter map
     * @param typeName the type name
     * @param encodedId the id
     * @param item the object
     */
    protected void putValue(String typeName, String encodedId, Object item)
    {   // put in Table
        if (encodedId==null)
        {
            throw new InvalidArgumentException("encodedId", encodedId);
        }
        Hashtable<String, Object> map = typeMap.get(typeName);
        if (map==null)
        {   map = new Hashtable<String, Object>(1);
            typeMap.put(typeName, map);
        }
        map.put(encodedId, item);
    }
    
    /**
     * encodes the objectKey and stores the item in the parameter map
     * @param typeName the type name
     * @param objectKey the object key
     * @param item the object
     * @param useCache flag whether to cache the objectKey
     * @return the encoded identifier
     */
    protected String encodeAndStore(String typeName, String objectKey, Object item, boolean useCache)
    {   // Generate the id
        String encodedId = (useCache ? encodeStringWithCache(objectKey) : encodeString(objectKey));
        // store
        putValue(typeName, encodedId, item);
        // return id
        return encodedId;
    }

    /**
     * puts a key of a particular type into the parameter map 
     * @param type the type name
     * @param key the key string
     * @param value the value
     * @param useCache true if the keys should be cached
     * @return the encoded key
     */
    public String put(String type, String key, String value, boolean useCache)
    {
        // Generate id and put in map
        return encodeAndStore(type, key, value, useCache);
    }

    public Object get(String type, String id)
    {
        Hashtable<String, Object> map = typeMap.get(type);
        return (map!=null ? map.get(id) : null);
    }

    public void clear(String type)
    {
        Hashtable<String, Object> map = typeMap.get(type);
        if (map!=null)
            map.clear();
    }

    /**
     * Puts an object into the paramter map that implements the ParameterObject interface  
     * @param paramObject the param object
     * @param useCache flag whether to cache the objectKey
     * @return the encoded object
     */
    public String put(ParameterObject paramObject, boolean useCache)
    {
        String objectKey;
        // check param
        if (paramObject==null || StringUtils.isEmpty((objectKey=paramObject.getObjectKey())))
            throw new InvalidArgumentException("paramObject", paramObject);
        // Generate id and put in map
        String type = paramObject.getClass().getName();
        return encodeAndStore(type, objectKey, paramObject, useCache);
    }
    
    /**
     * Puts an object into the paramter map that implements the ParameterObject interface  
     * @param paramObject the param object
     * @return the encoded object
     */
    public String put(ParameterObject paramObject)
    {
        return put(paramObject, false);
    }

    /**
     * Returns the ParameterObject for the given encoded id
     * @param <T> the parameter type
     * @param paramType the param type class
     * @param id the item id
     * @return the object
     */
    @SuppressWarnings("unchecked")
    public <T extends ParameterObject> T get(Class<T> paramType, String id)
    {
        String type = paramType.getName();
        Hashtable<String, Object> map = typeMap.get(type);
        return (T)(map!=null ? map.get(id) : null);
    }

    public void clear(Class<? extends Object> paramType)
    {
        String type = paramType.getName();
        clear(type);
    }
    
    /**
     * Puts a RowSet key into the parameter map  
     * @param rowset the rowset
     * @param key the record key
     * @return the encoded key
     */
    public String put(DBRowSet rowset, Object[] key)
    {
        // Generate id and put in map
        String rowKey = StringUtils.arrayToString(key);
        String type = getRowSetTypeName(rowset);
        return encodeAndStore(type, rowKey, key, false);
    }

    /**
     * Returns a record key for a given RowSet from an encoded id 
     * @param rowset the RowSet for which to get the key
     * @param id the encoded id
     * @return the record key
     */
    public Object[] getKey(DBRowSet rowset, String id)
    {
        String type = getRowSetTypeName(rowset);
        Hashtable<String, Object> map = typeMap.get(type);
        return (map!=null ? ((Object[])map.get(id)) : null);
    }

    public void clear(DBRowSet rowset)
    {
        String type = getRowSetTypeName(rowset);
        clear(type);
    }

    /**
     * Puts a key for a given class into the parameter map 
     * @param c the class 
     * @param key the key for this class
     * @return the encoded key
     */
    public String put(Class<? extends Object> c, Object[] key)
    {
        // Generate id and put in map
        String ref = StringUtils.valueOf(key);
        String type = c.getName();
        return encodeAndStore(type, ref, key, false);
    }

    /**
     * Returns a class key from an encoded id
     * @param c the class for which to retrieve the key
     * @param id the encoded id
     * @return the class key
     */
    public Object[] getKey(Class<? extends Object> c, String id)
    {
        String type = c.getName();
        Hashtable<String, Object> map = typeMap.get(type);
        return (map!=null ? ((Object[])map.get(id)) : null);
    }
    
    /**
     * Generates an idParam which is only valid for the given page.
     * @param page the target page
     * @param rowset the rowset
     * @param key the key
     * @return the encoded object
     */
    public String put(PageDefinition page, DBRowSet rowset, Object[] key) {
        // Generate id and put in map
        String ref  = StringUtils.valueOf(key);
        String type = StringUtils.concat(page.getPageBeanName(), "$", getRowSetTypeName(rowset));
        return encodeAndStore(type, ref, key, false);
    }

    /**
     * Returns an record key for a given page and rowset
     * @param page the page
     * @param rowset the rowset
     * @param id the object id
     * @return the object key
     */
    public Object[] getKey(PageDefinition page, DBRowSet rowset, String id)
    {
        String type = StringUtils.concat(page.getPageBeanName(), "$", getRowSetTypeName(rowset));
        Hashtable<String, Object> map = typeMap.get(type);
        return (map!=null ? ((Object[])map.get(id)) : null);
    }

    public void clear(PageDefinition page, DBRowSet rowset)
    {
        String type = StringUtils.concat(page.getPageBeanName(), "$", getRowSetTypeName(rowset));
        clear(type);
    }
}
