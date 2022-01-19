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

package org.apache.empire.db;

import org.apache.empire.data.DataType;
import org.apache.empire.db.context.DBContextStatic;
import org.apache.empire.db.expr.order.DBOrderByExpr;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.01.11 21:56
 */
public class SerializeTest {

  @Test
  public void testDBObject() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (DBObject dbo : getObjectsToSerialize()) {
      baos.reset();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(dbo);

      ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
      DBObject dbo2 = (DBObject) oin.readObject();

      //very simple check...
      Assert.assertNotNull(dbo2.getDatabase());
      Assert.assertEquals(dbo.getClass(), dbo2.getClass());
      Assert.assertEquals(dbo.getDatabase().getClass(), dbo2.getDatabase().getClass());
    }
  }

  public DBObject[] getObjectsToSerialize() {
    final TestDatabase db = new TestDatabase();
    DBContext context = new DBContextStatic(new MockDriver(), null); 
    DBRecord rec = new DBRecord(context, db.T_TEST);
    rec.init(true);
    db.open(context.getDriver(), null);
    return new DBObject[] {
            db,
            db.T_TEST,
            db.V_TEST,
            db.T_TEST.C_ID,
            db.T_TEST.C_TEXT,
            db.V_TEST.C_TEXT,
            db.V_TEST.createCommand(),
            rec,
            new DBRelation(db, "test", new DBRelation.DBReference[]{db.T_TEST.C_TEXT.referenceOn(db.T_TEST.C_TEXT)}),
            new DBOrderByExpr(db.T_TEST.C_TEXT, true),
            db.T_TEST.C_ID.is(55)
    };
  }

  static class TestDatabase extends DBDatabase {
    private final static long serialVersionUID = 1L;
    
    public final TestTable T_TEST = new TestTable(this);
    public final TestView V_TEST = new TestView(this, T_TEST);

    static class TestTable extends DBTable {
      private final static long serialVersionUID = 1L;
      public final DBTableColumn C_ID;
      public final DBTableColumn C_TEXT;

      TestTable(DBDatabase db) {
        super("testtable", db);
        this.C_ID = addColumn("id", DataType.INTEGER, 0, true);
        setPrimaryKey(C_ID);
        this.C_TEXT = addColumn("text", DataType.VARCHAR, 255, false);
      }
    }
    static class TestView extends DBView {
      private final static long serialVersionUID = 1L;
      public final DBViewColumn C_TEXT;

      TestView(DBDatabase db, TestTable t) {
        super("testview", db);
        this.C_TEXT = addColumn(t.C_TEXT);
      }

      @Override
      public DBCommandExpr createCommand() {
        TestDatabase db = (TestDatabase) getDatabase();
        DBCommand cmd = db.createCommand();
        cmd.select(db.T_TEST.C_TEXT);
        cmd.where(db.T_TEST.C_ID.isGreaterThan(5));
        return cmd;
      }
    }

  }
}
