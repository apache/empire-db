/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.db;
// java
import java.util.Set;

/**
 * This class is used for building up a partition of a SQL-Command.
 * It handles the insert from a specified key word between two DBCommandExpr objects.
 * <P>
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de</A>
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
  /**
   * This helper function adds the DBColumnExpr objects to the Vector: 'orderBy'.
   * 
   * @param expr the first DBColumnExpr object
   * @param desc an boolean value
   */
   @Override
   public void orderBy(DBColumnExpr expr, boolean desc)
   {
      // set statement
      super.orderBy(getCmdColumn(expr), desc);
   }


}


