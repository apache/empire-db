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
package org.apache.empire.db;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class allocates methods to store binary large objects in the database.
 *
 *
 */
public class DBBlobData
{
    /** Logger */
    private static final Log log          = LogFactory.getLog(DBBlobData.class);

    /**
     * The stream associated with this object
     */
    private InputStream   inputStream     = null;

    /**
     * The length of the data in the <code>inputStream</code>.
     */
    private int           length          = 0;

    /**
     * The defaultEncoding used for the constructor.
     *
     * @see DBBlobData#DBBlobData(String)
     * @see DBBlobData#setDefaultEncoding(String)
     */
    private static String defaultEncoding = null;

    /**
     * Constructor to pass LOB data to the setValue methods
     * of a record, consisting of the input stream where
     * the data can be loaded from and the length of the data.
     *
     * @param inputStream The stream where the data will be read from
     * @param length The number of bytes to read from the stream
     * @throws IllegalArgumentException If the inputStream is null
     */
    public DBBlobData(InputStream inputStream, int length)
    	throws IllegalArgumentException
    {
        if (inputStream == null)
        {
            throw new IllegalArgumentException("inputStream was null");
        }
        this.inputStream = inputStream;
        this.length = length;
    }

    /**
     * Constructor for LobData from a byte array.
     *
     * @param data The byte array
     * @see java.lang.String#getBytes(java.lang.String)
     * @throws IllegalArgumentException If the text is null or the encoding is unsupported
     */
    public DBBlobData(byte[] data)
    	throws IllegalArgumentException
    {
        if (data == null)
        {
            throw new IllegalArgumentException("data was null");
        }
        // Set Input Stream
        this.inputStream = new ByteArrayInputStream(data);
        this.length = data.length;
    }

    /**
     * Constructor for LobData from a string.
     *
     * @param text The string to be used as data
     * @param encoding The encoding of the string. The string must be converted to a byte array to put in the BLOB. It is
     *            necessary to convert the string.
     * @see java.lang.String#getBytes(java.lang.String)
     * @throws IllegalArgumentException If the text is null or the encoding is unsupported
     */
    public DBBlobData(String text, String encoding)
    	throws IllegalArgumentException
    {
        byte[] byteArray = null;
        if (text == null)
        {
            throw new IllegalArgumentException("text was null");
        }
        if (encoding == null)
        {
            throw new IllegalArgumentException("encoding was null");
        }
        try
        {
            byteArray = text.getBytes(encoding);
            this.inputStream = new ByteArrayInputStream(byteArray);
            this.length = byteArray.length;
        } catch (UnsupportedEncodingException uee)
        {
            log.error("Unsupported encoding " + encoding, uee);
            throw new IllegalArgumentException("encoding " + encoding + " is unsupported");
        }
    }

    /**
     * Constructor for LobData from a string.
     *
     * @param text The string to be used as data. The encoding is set using setDefaultEncoding
     * @see java.lang.String#getBytes(java.lang.String)
     * @throws IllegalArgumentException If the text is null or the encoding is unsupported
     */
    public DBBlobData(String text)
    	throws IllegalArgumentException
    {
        this(text, defaultEncoding);
    }
    
    /**
     * Returns the inputStream with the binary data for the BLOB.
     *
     * @return Returns the inputStream with the binary data for the BLOB
     */
    public InputStream getInputStream()
    {
        return inputStream;
    }

    /**
     * Returns the length of the BLOB data in bytes.
     * 
     * @return Returns the length of the BLOB data in bytes
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Sets the defaultEncoding used in a constructor.
     *
     * @param defaultEncoding Set the defaultEncoding used in a constructor
     * @see DBBlobData#DBBlobData(String)
     */
    public static void setDefaultEncoding(String defaultEncoding)
    {
        DBBlobData.defaultEncoding = defaultEncoding;
    }
}