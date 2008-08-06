/*
 * ESTEAM Software GmbH
 */
package org.apache.empire.commons;

/**
 * This interface allows access to the last error that occured on a object.
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public interface ErrorInfo
{
  /**
   * Returns wether or not an error is set.
   * 
   * @return true if an error has been set, or otherwise false
   */
  boolean hasError();

  /**
   * Returns the last error code.
   * 
   * @return the error code of ERR_NONE if no error has been set.
   */
  ErrorType getErrorType();

  /**
   * Returns the parameters used to build the error text.
   * 
   * @return the error message or null if no error has been set.
   */
  Object[] getErrorParams();

  /**
   * Returns the source object's class name that raised the error
   * 
   * @return the error message or null if no error has been set.
   */
  String getErrorSource();
  
}