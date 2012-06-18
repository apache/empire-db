/*
 * ESTEAM Software GmbH, 18.04.2012
 */
package org.apache.empire.jsf2.websample.web.pages;

import org.apache.empire.jsf2.pages.PageDefinition;
import org.apache.empire.jsf2.pages.PageDefinitions;

public final class SamplePages extends PageDefinitions
{
    public static final PageDefinition LoginPage          = new PageDefinition("/pages/loginPage.xhtml", LoginPage.class);

    public static final PageDefinition EmployeeListPage   = new PageDefinition("/pages/employeeListPage.xhtml", EmployeeListPage.class, SamplePages.LoginPage);
    public static final PageDefinition EmployeeDetailPage = new PageDefinition("/pages/employeeDetailPage.xhtml", EmployeeDetailPage.class, SamplePages.EmployeeListPage);

}