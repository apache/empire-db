/*
 * ESTEAM Software GmbH, 20.07.2007
 */
package org.apache.empire.struts2.action;

public interface ListPagingInfo
{
    int getPageSize();

    int getFirstItemIndex();

    int getLastItemIndex();
    
    int getItemCount();
    
    int getPageCount();

    int getPage();
    
    void setPage(int page);
    
    String getListPropertyName();

}
