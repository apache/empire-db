package org.apache.empire.data;

import java.util.List;

public interface Entity
{
    String getName(); 

    List<? extends Column> getColumns();
    
    Column[] getKeyColumns();
}
