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
package org.apache.empire.jsf2.websample.web;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.faces.context.FacesContext;

import org.apache.empire.commons.ClassUtils;
import org.apache.empire.jsf2.app.WebDBContext;
import org.apache.empire.jsf2.websample.db.SampleDB;

/**
 * This is an example for a custom DBContext extension
 * @author rainer
 */
public class SampleContext extends WebDBContext<SampleDB>
{
    private static final long serialVersionUID = 1L;
    
    private final transient SampleSession session;
    
    /**
     * Custom serialization for transient fields.
     * 
     */
    private void writeObject(ObjectOutputStream strm) throws IOException 
    {   // write the object
        strm.defaultWriteObject();
    }
    
    /**
     * Custom deserialization for transient fields.
     */
    private void readObject(ObjectInputStream strm) 
        throws IOException, ClassNotFoundException
    {   // Restore Session
        FacesContext fc = FacesContext.getCurrentInstance();
        SampleSession session = SampleUtils.getSampleSession(fc);
        ClassUtils.setPrivateFieldValue(SampleContext.class, this, "session", session);
        // Read the object
        strm.defaultReadObject();
    }
    
    /**
     * Constructs a SampleContext 
     */
    public SampleContext(SampleDB db, SampleSession session)
    {
        super(db);
        // the session
        this.session = session;
    }

    public SampleUser getUser()
    {
        return session.getUser();
    }
}
