package org.apache.empire.data;

import java.util.List;

public interface EntityType
{
    String getEntityName(); 

    List<? extends Column> getColumns();
    
    Column[] getKeyColumns();
    
    Class<?> getBeanType();
}
