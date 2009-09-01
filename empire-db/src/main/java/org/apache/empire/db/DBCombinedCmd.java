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

import java.util.Set;

/**
 * This class is used for building up a partition of a SQL-Command.
 * It handles the insert from a specified key word between two DBCommandExpr objects.
 * <P>
 * 
 *
 */
public class DBCombinedCmd extends DBCommandExpr
{
   // Memebers
   private DBCommandExpr left;
   private DBCommandExpr right;
   private String        keyWord;

  /**
   * Constructs a new DBFuncExpr object and
   * sets the specified parameters to this object.
   * 
   * @param left the first DBCommandExpr object
   * @param keyWord the key word between the wo DBCommandExpr objects
   * @param right the second DBCommandExpr object
   */
   public DBCombinedCmd(DBCommandExpr left, String keyWord, DBCommandExpr right)
   {
      this.left    = left;
      this.right   = right;
      this.keyWord = keyWord;
   }

   @Override
   public boolean isValid()
   {
       return (left.isValid() && right.isValid()); 
   }

  /**
   * Returns the current DBDatabase object.
   * 
   * @return the current DBDatabase object
   */
   @Override
   public DBDatabase getDatabase()
   {
      return left.getDatabase();
   }

   /**
    * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
    */
   @Override
   public void addReferencedColumns(Set<DBColumn> list)
   {
      left .addReferencedColumns(list);
      right.addReferencedColumns(list);
   }
  /**
   * Calls the method dbDBCommandExpr.getSelectExprList from the private variable 'left'.
   * Returns a array of all DBColumnExpr object of the Vector: 'select'.
   * 
   * @see org.apache.empire.db.DBCommandExpr#getSelectExprList()
   * @return returns an array of all DBColumnExpr object of the Vector: 'select'
   */
   @Override
   public DBColumnExpr[] getSelectExprList()
   {
      // DebugMsg(2, "Check: getSelectExprList() for DBCombinedCmd");
      return left.getSelectExprList();
   }

  /**
   * Creates the SQL-Command.
   * 
   * @param buf the SQL-Command
   * @return true if the creation was successful
   */
   @Override
   public boolean getSelect(StringBuilder buf)
   {
      // the left part
      left.clearOrderBy();
      if (!left.getSelect(buf))
           return error(left);
      // concat keyword     
      buf.append( " " );
      buf.append( keyWord );
      buf.append( " (" );
      // the right part
      right.clearOrderBy();
      if (!right.getSelect(buf))
           return error(right);
      // done
      buf.append( ")" );
      // Add optional Order by statement
      if ( orderBy!=null )
      {    // Having
           buf.append("\r\nORDER BY ");
           addListExpr(buf, orderBy, CTX_DEFAULT, ", ");
      }
      return success();
   }

   @Override
   public void orderBy(DBColumnExpr expr, boolean desc)
   {
      super.orderBy(getCmdColumn(expr), desc);
   }


}


