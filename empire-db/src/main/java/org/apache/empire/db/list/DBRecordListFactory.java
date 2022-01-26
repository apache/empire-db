/*
 * ESTEAM Software GmbH, 26.01.2022
 */
package org.apache.empire.db.list;

import java.util.List;

import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;

public interface DBRecordListFactory<T extends DBRecord>
{
    void prepareQuery(DBCommand cmd);
    
    List<T> newList(int capacity);

    T newRecord(int rownum, DBRecordData dataRow);
    
    void completeQuery(List<T> list);
}
