/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db.expr.compare;

import org.apache.empire.db.DBExpr;

/**
 * This class is a common base class for all SQL filter constraints classes<br>
 * <P>
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de</A>
 */
public abstract class DBCompareExpr extends DBExpr
{
  public abstract boolean isMutuallyExclusive(DBCompareExpr other);

  /**
   * Creates a new DBCompareORExpr object.
   */
  public DBCompareExpr and(DBCompareExpr expr)
  {
      // " AND "
      return new DBCompareAndOrExpr(this, expr, false);
  }

  /**
   * Create a new DBCompareORExpr object.
   */
  public DBCompareExpr or(DBCompareExpr expr)
  {
      // " OR "
      return new DBCompareAndOrExpr(this, expr, true);
  }
  
  /**
   * Creates a sql-expression for the not() function.
   * 
   * @return the new DBCompareColExpr object
   */
  public DBCompareExpr not()
  {
      return new DBCompareNotExpr(this);
  }
  
}
