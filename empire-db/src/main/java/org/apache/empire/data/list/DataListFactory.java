package org.apache.empire.data.list;

import java.util.List;

import org.apache.empire.data.RecordData;

public interface DataListFactory<T extends DataListEntry>
{
    void prepareQuery();
    
    List<T> newList(int capacity);

    T newEntry(int rownum, RecordData dataRow);
    
    void completeQuery(List<T> list);
}
