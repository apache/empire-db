/*
 * ESTEAM Software GmbH, 11.03.2005
 * 
 */
package org.apache.empire.db;

import java.io.Reader;
import java.io.StringReader;

/**
 * This class allocates methods to store binary character objects in the database.
 *
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
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
            throw new IllegalArgumentException("reader was null");

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
            throw new IllegalArgumentException("text was null");
        }
        reader = new StringReader(text);
        this.length = text.length();
    }

    /**
     * Set the defaultEncoding used in a constructor.
     *
     * @return Returns the reader with the character data for the CLOB
     */
    public Reader getReader()
    {
        return reader;
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