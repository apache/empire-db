/*
 * ESTEAM Software GmbH, 03.07.2007
 */
package org.apache.empire.struts2.action;

public interface ActionAccessValidator
{
    /*
     * checks wether or not the user must login first to access this page
     * 
     * return true if 
     * 1. the action requries login
     * 2. the user has not been logged on already
     * or false otherwise.
     * 
     * check is performed by ActionAccessInterceptor 
     */
    boolean loginRequired();
    
    /*
     * checks wether or not the user has access to this action
     * or an idividual method respectively
     * 
     * check is performed by ActionAccessInterceptor 
     */
    boolean hasAccess(String method);
}
