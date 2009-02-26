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

package org.apache.empire.samples.cxf.wssample.server;

import java.io.StringWriter;

import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

public class DocumentWriter extends StringWriter
{
    
    private StyledDocument _doc;
    private Style _style;
    public DocumentWriter(StyledDocument d, Style style)
    {
        this._doc=d;
        this._style=style;
    }
    
    @Override
    public void write(String str)
    {
        try{
            _doc.insertString(_doc.getLength(), str, _style);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    @Override
    public void write(String str, int off, int len)
    {
        try{
            _doc.insertString(_doc.getLength(), str, _style);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
