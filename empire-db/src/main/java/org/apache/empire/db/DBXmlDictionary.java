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

/**
 * This class is used to configure XML generation as performed by the 
 * getXmlDocument Document function on DBReader and DBRecord.<BR>
 */
public class DBXmlDictionary
{
    private static DBXmlDictionary dbXmlDictonary; 
    
    public static DBXmlDictionary getInstance()
    {
        if (dbXmlDictonary==null)
        {
            // dbXmlTagDictionary has not been set. Using Default Dictionary");
            dbXmlDictonary = new DBXmlDictionary();
        }    
        return dbXmlDictonary;
    }

    public static void set(DBXmlDictionary xmlDictonary)
    {
        dbXmlDictonary = xmlDictonary;
    }

    // ------- XML Element and Attribute Names -------
    
    public String getRowSetElementName() {
        return "rowset"; 
    }
    
    public String getRowElementName() {
        return "row"; 
    }
    
    public String getRowIdColumnAttribute() {
        return "id";
    }
    
}
