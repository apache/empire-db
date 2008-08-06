/*
 * ESTEAM Software GmbH, 23.07.2007
 */
package org.apache.empire.struts2.action;


public interface ActionItemProperty
{
    /**
     * returns the name of the parameter used to get and set the action's item
     * This function is used by the jsp tags as default parameter name of the item attribute
     * By default this ist the name "item"
     * 
     * @return the name of the item parameter
     */
    String getItemPropertyName();
}
