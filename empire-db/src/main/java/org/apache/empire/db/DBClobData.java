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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.empire.exceptions.InternalException;

/**
 * This class allocates methods to store binary character objects in the database.
 *
 *
 */
public class DBClobData
{
    /**
     * The reader associated with this object
     */
    private Reader     reader = null;

    /**
     * The length of the data in the <code>reader</code>
     */
    private int        length = 0;

    /**
     * Constructor to pass LOB data to the setValue methods of
     * a record, consisting of the input stream where the data can be
     * loaded from and the length of the data.
     *
     * @param reader The reader where the character data will be read from
     * @param length The number of characters to read from the reader
     * @throws IllegalArgumentException If the reader is null
     */
    public DBClobData(Reader reader, int length)
        throws IllegalArgumentException
    {
        if (reader == null)
        {
            throw new IllegalArgumentException("reader must not be null");

        }
        this.reader = reader;
        this.length = length;
    }

    /**
     * Constructor for LobData from a string.
     *
     * @param text The string to be used as data
     * @throws IllegalArgumentException If the text is null
     */
    public DBClobData(String text) 
        throws IllegalArgumentException
    {
        if (text == null)
        {
            throw new IllegalArgumentException("text must not be null");
        }
        reader = new StringReader(text);
        this.length = text.length();
    }

    /**
     * Get the Reader for the large string
     *
     * @return Returns the reader with the character data for the CLOB
     */
    public Reader getReader()
    {
        try
        {
            reader.reset();
            return reader;
        }
        catch (IOException e)
        {
            throw new InternalException(e);
        }
    }

    /**
     * Returns the length of the CLOB data in characters.
     *
     * @return Returns the length of the CLOB data in characters
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Returns a CLOB String.
     *
     * @return Returns CLOB String
     */
    @Override
    public String toString()
    {
        // WARNING: String contained in reader is NOT supplied.
        return super.toString();
    }
}