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
package org.apache.empire.jsf2.websample.web.pages;


public class ErrorPage extends SamplePage
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    
    private boolean showDetails = false;

    public boolean isShowDetails()
    {
        return this.showDetails;
    }

    public void setShowDetails(boolean showDetails)
    {
        this.showDetails = showDetails;
    }

    public void toggleShowDetails()
    {
        this.showDetails = !this.showDetails;
    }

//    public String getMessage()
//    {
//        if (getSampleSession().getError() != null)
//        {
//            return getSampleSession().getError().getMessage();
//        }
//        return "NULL";
//    }
//
//    public String getType()
//    {
//        if (getSampleSession().getError() != null)
//        {
//            return getSampleSession().getError().getClass().getName();
//        }
//        return "NULL";
//    }

//    public List<String> getTrace()
//    {
//        List<String> trace = new ArrayList<String>();
//        if (getSampleSession().getError() != null && getSampleSession().getError().getStackTrace() != null)
//        {
//            for (StackTraceElement element : getSampleSession().getError().getStackTrace())
//            {
//                trace.add(element.toString());
//            }
//        }
//        return trace;
//    }
}
