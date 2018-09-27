package org.apache.empire.vue.sample.db;

import java.sql.Connection;

public interface RecordContext
{
    Connection getConnection();
}
