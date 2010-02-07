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
package org.apache.empire.samples.spring;

import java.util.logging.Logger;

import org.apache.empire.commons.ErrorObject;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * 
 */
public class SampleSpringApp {
    private static final Logger log = Logger.getLogger(SampleSpringApp.class.getName());
    static {
        ErrorObject.setExceptionsEnabled(true);
    }
    //creates the application context
    //this is usually in some bootstrapping code; so your application will
    //just have one at runtime.
    static ApplicationContext ctx = getContext();

    //get the service that is the entry point into the application
    //normally this is injected by spring into classes that need it
    static EmpireApp appBean = ctx.getBean("empireApp", EmpireApp.class);

    public static void main(String[] args) throws Exception {

        System.out.println("Running Spring Example...");

        appBean.setupDatabase();
        appBean.clearDatabase();
        
        System.out.println("*** Step 6: insertDepartment() & insertEmployee() ***");
        int idDevDep = appBean.insertDepartment("Development", "ITTK");
		int idSalDep = appBean.insertDepartment("Sales", "ITTK");

        int idPers1 = appBean.insertEmployee("Peter", "Sharp", "M", idDevDep);
		int idPers2 = appBean.insertEmployee("Fred", "Bloggs", "M", idDevDep);
		int idPers3 = appBean.insertEmployee("Emma", "White", "F", idSalDep);

        System.out.println("*** Step 7: updateEmployee() ***");
        appBean.updateEmployee(idPers1, "+49-7531-457160");
        appBean.updateEmployee(idPers2, "+49-5555-505050");
        appBean.updateEmployee(idPers3, "+49-040-125486");

        System.out.println("*** Step 8 Option 1: queryRecords() / Tab-Output ***");
        appBean.doQuery(EmpireApp.QueryType.Reader);

        System.out.println("*** Step 8 Option 2: queryRecords() / Bean-List-Output ***");
        appBean.doQuery(EmpireApp.QueryType.BeanList);

        System.out.println("*** Step 8 Option 3: queryRecords() / XML-Output ***");
        appBean.doQuery(EmpireApp.QueryType.XmlDocument);
    }



    static GenericApplicationContext getContext() {
        log.info("Creating Spring Application Context ...");
        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
        reader.loadBeanDefinitions(new ClassPathResource("/applicationContext.xml"));

        ctx.refresh();
        return ctx;
    }
}
