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

import javax.faces.context.FacesContext;

import org.apache.empire.jsf2.app.FacesUtils;
import org.apache.empire.jsf2.utils.ParameterMap;
import org.apache.empire.jsf2.websample.db.SampleDB;

public class SampleUtils extends FacesUtils
{
    public static SampleApplication getSampleApplication()
    {
        return SampleApplication.get();
    }

    public static SampleDB getDatabase()
    {
        return getSampleApplication().getDatabase();
    }

    public static SampleUser getSampleUser()
    {
        FacesContext fc = getContext();
        return (SampleUser) getManagedBean(fc, SampleUser.MANAGED_BEAN_NAME);
    }

    public static SampleSession getSampleSession(FacesContext fc)
    {
        return (SampleSession) getManagedBean(fc, SampleSession.MANAGED_BEAN_NAME);
    }

    public static SampleSession getSampleSession()
    {
        return getSampleSession(FacesContext.getCurrentInstance());
    }

    public static ParameterMap getParameterMap()
    {
        return getParameterMap(getContext());
    }

    public static String getContextPath()
    {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    public static Object getRequestAttribute(final String key)
    {
        FacesContext fc = getContext();
        return getRequestAttribute(fc, key);
    }

    public static void setRequestAttribute(final String key, Object value)
    {
        FacesContext fc = getContext();
        setRequestAttribute(fc, key, value);
    }

    public static <T> T getManagedBean(final Class<T> cls)
    {
        FacesContext fc = getContext();
        return getManagedBean(fc, cls);
    }

    /* file */

    public static String getRealPath(String path)
    {
        FacesContext fc = getContext();
        return getRealPath(fc, path);
    }

    public static String getFilePath(String path, String file)
    {
        FacesContext fc = getContext();
        return getFilePath(fc, path, file);
    }
}
